package app

import app.batch.DailyReportJob
import app.config.BatchConfig
import app.config.KafkaConsumerClient
import app.config.KafkaJsonProducer
import app.models.BatchOrderRecord
import app.models.ValidatedOrderEnvelope
import app.repository.BatchOrderRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import io.github.cdimascio.dotenv.Dotenv
import org.apache.spark.sql.SparkSession
import org.bson.Document
import org.slf4j.LoggerFactory

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import scala.jdk.CollectionConverters._
import scala.util.control.NonFatal

object MainBatchApp {
  private val logger = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    // Load .env if present and expose entries to PureConfig via system properties.
    val dotenv = Dotenv.configure().ignoreIfMissing().load()
    dotenv.entries().asScala.foreach(e => System.setProperty(e.getKey, e.getValue))

    val cfg = BatchConfig.getConfig("batch.conf")

    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    mapper.registerModule(new JavaTimeModule())
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    val repo = new BatchOrderRepository(cfg.mongo)
    val producer = new KafkaJsonProducer(cfg.producerProps, mapper)

    val spark = SparkSession
      .builder()
      .appName(cfg.spark.appName)
      .master(cfg.spark.master)
      .getOrCreate()

    val job = new DailyReportJob(spark, repo)
    val consumer = new KafkaConsumerClient(cfg.consumerProps, cfg.topics.input, cfg.pollTimeoutMs)

    // --- Phase 1: continuous ingestion thread ---
    val consumerThread = new Thread(
      () =>
        consumer.runLoop { payload =>
          try {
            val envelope = mapper.readValue(payload, classOf[ValidatedOrderEnvelope])
            if (envelope.status == "VALID") {
              BatchOrderRecord.from(envelope) match {
                case Some(record) =>
                  repo.saveValidOrder(record)
                  logger.info(s"Persisted valid order ${record.orderId}")
                case None =>
                  logger.warn(s"Dropping VALID envelope with missing order payload: ${envelope.orderId}")
              }
            }
          } catch {
            case NonFatal(e) =>
              logger.error(s"Failed to handle orders-validated payload: ${e.getMessage}")
          }
        },
      "kafka-consumer-thread"
    )
    consumerThread.setDaemon(true)
    consumerThread.start()

    // --- Phase 2/3: scheduled batch job, publishes report to Kafka + Mongo ---
    val scheduler = Executors.newSingleThreadScheduledExecutor()
    val runnable: Runnable = () => {
      try {
        logger.info("Running daily report batch job")
        val report = job.run()
        if (report.totalOrders == 0) {
          logger.info("Skipping report publication — no orders yet")
        } else {
          val key = s"report-${System.currentTimeMillis()}"
          producer.send(cfg.topics.reportOutput, key, report)
          repo.saveReport(Document.parse(mapper.writeValueAsString(report)))
          logger.info(
            s"Published report: orders=${report.totalOrders}, revenue=${report.totalRevenue}, " +
              s"topProducts=${report.topProducts.size}, topCities=${report.topCitiesByOrders.size}"
          )
        }
      } catch {
        case NonFatal(e) => logger.error(s"Batch run failed: ${e.getMessage}", e)
      }
    }

    val interval = cfg.batch.intervalMinutes.toLong
    scheduler.scheduleAtFixedRate(runnable, interval, interval, TimeUnit.MINUTES)
    logger.info(s"Scheduler started — first run in $interval minute(s), fixed rate thereafter")

    Runtime.getRuntime.addShutdownHook(new Thread(() => {
      logger.info("Shutdown hook: stopping services")
      scheduler.shutdown()
      consumer.stop()
      producer.close()
      try spark.stop()
      catch { case _: Exception => () }
      repo.close()
    }))

    // Keep the main thread alive while the daemon consumer thread runs.
    consumerThread.join()
  }
}
