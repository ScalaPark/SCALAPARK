package app.streaming

import app.config.ConsumerConfig
import app.models._
import app.repository.ValidatedOrderRepository
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.cdimascio.dotenv.Dotenv
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.Dataset
import org.apache.spark.sql.Encoders
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.bson.Document

import java.time.LocalDateTime

object OrderSparkStreamingJob {

  case class ValidationOutput(
    topic: String,
    partition: Int,
    offset: Long,
    orderId: String,
    correlationId: String,
    status: String,
    errors: Seq[String],
    processedAt: String,
    rawPayload: String,
    order: Option[OrderRecord]
  )

  def main(args: Array[String]): Unit = {
    val dotenv = Dotenv.configure().ignoreIfMissing().load()
    import scala.jdk.CollectionConverters._
    dotenv.entries().asScala.foreach(e => System.setProperty(e.getKey, e.getValue))

    val runtimeConfig = ConsumerConfig.getConfig("kafka-intro.conf")

    val spark = SparkSession
      .builder()
      .appName(runtimeConfig.spark.appName)
      .master(runtimeConfig.spark.master)
      .getOrCreate()

    import spark.implicits._

    val ordersRaw = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", runtimeConfig.consumerProps("bootstrap.servers").toString)
      .option("subscribe", runtimeConfig.topics.input)
      .option("startingOffsets", runtimeConfig.consumerProps("auto.offset.reset").toString)
      .load()
      .selectExpr(
        "topic",
        "partition",
        "offset",
        "CAST(key AS STRING) AS key",
        "CAST(value AS STRING) AS value",
        "timestamp"
      )

    val validationEncoder = Encoders.product[ValidationOutput]

    val validatedDs: Dataset[ValidationOutput] = ordersRaw
      .as[(String, Int, Long, String, String, java.sql.Timestamp)]
      .mapPartitions { rows =>
        val mapper = buildMapper()
        rows.map { case (topic, partition, offset, _, payload, _) =>
          try {
            val record = mapper.readValue(payload, classOf[OrderRecord])
            val errors = validateRecord(record)
            val status = if (errors.isEmpty) "VALID" else "INVALID"
            ValidationOutput(
              topic = topic,
              partition = partition,
              offset = offset,
              orderId = record.header.orderId,
              correlationId = record.header.correlationId,
              status = status,
              errors = errors,
              processedAt = LocalDateTime.now().toString,
              rawPayload = payload,
              order = Some(record)
            )
          } catch {
            case ex: Exception =>
              ValidationOutput(
                topic = topic,
                partition = partition,
                offset = offset,
                orderId = "UNPARSEABLE-" + System.currentTimeMillis(),
                correlationId = "N/A",
                status = "DESERIALIZATION_ERROR",
                errors = Seq("Unable to deserialize message: " + ex.getMessage),
                processedAt = LocalDateTime.now().toString,
                rawPayload = payload,
                order = None
              )
          }
        }
      }(validationEncoder)

    val validatedForKafka: DataFrame = validatedDs
      .toDF()
      .selectExpr(
        "orderId AS key",
        "to_json(struct(orderId, correlationId, status, errors, processedAt, order)) AS value"
      )

    val validationQuery = validatedForKafka.writeStream
      .format("kafka")
      .option("kafka.bootstrap.servers", runtimeConfig.producerProps("bootstrap.servers").toString)
      .option("topic", runtimeConfig.topics.validatedOutput)
      .option(
        "checkpointLocation",
        runtimeConfig.spark.checkpointBasePath + "/orders-validated"
      )
      .start()

    val metricsAgg = validatedDs
      .withColumn("processedAtTs", to_timestamp(col("processedAt")))
      .withWatermark("processedAtTs", "30 seconds")
      .groupBy(window(col("processedAtTs"), runtimeConfig.metrics.publishIntervalSeconds + " seconds"))
      .agg(
        count(lit(1)).as("total"),
        sum(when(col("status") === "VALID", 1).otherwise(0)).as("valid"),
        sum(when(col("status") === "INVALID", 1).otherwise(0)).as("invalid"),
        sum(when(col("status") === "DESERIALIZATION_ERROR", 1).otherwise(0)).as("deserializationErrors")
      )

    val metricsRaw = metricsAgg
      .selectExpr(
        "concat('metrics-', cast(unix_timestamp(window.end) as string)) AS key",
        "to_json(named_struct('windowStart', cast(window.start as string), 'windowEnd', cast(window.end as string), 'total', total, 'valid', valid, 'invalid', invalid, 'deserializationErrors', deserializationErrors)) AS value"
      )

    val metricsQuery = metricsRaw.writeStream
      .format("kafka")
      .option("kafka.bootstrap.servers", runtimeConfig.producerProps("bootstrap.servers").toString)
      .option("topic", runtimeConfig.topics.metricsOutput)
      .outputMode("update")
      .option(
        "checkpointLocation",
        runtimeConfig.spark.checkpointBasePath + "/orders-validation-metrics"
      )
      .start()

    val mongoValidatedQuery = validatedDs.writeStream
      .foreachBatch { (batch: Dataset[ValidationOutput], _: Long) =>
        val repository = new ValidatedOrderRepository(runtimeConfig.mongo)
        batch.collect().foreach { event =>
          val doc = new Document()
          doc.put("topic", event.topic)
          doc.put("partition", Int.box(event.partition))
          doc.put("offset", Long.box(event.offset))
          doc.put("orderId", event.orderId)
          doc.put("correlationId", event.correlationId)
          doc.put("status", event.status)
          doc.put("errors", event.errors.mkString("; "))
          doc.put("processedAt", event.processedAt)
          doc.put("rawPayload", event.rawPayload)
          doc.put("order", event.order.map(_.toString).getOrElse("N/A"))
          repository.saveValidatedDocument(doc)
        }
      }
      .option(
        "checkpointLocation",
        runtimeConfig.spark.checkpointBasePath + "/mongo-validated-orders"
      )
      .start()

    val mongoMetricsQuery = metricsAgg.writeStream
      .foreachBatch { (batch: DataFrame, batchId: Long) =>
        val repository = new ValidatedOrderRepository(runtimeConfig.mongo)
        if (!batch.isEmpty) {
          batch.collect().foreach { row =>
            val windowRow = row.getAs[org.apache.spark.sql.Row]("window")
            val doc = new Document()
            doc.put("windowStart", windowRow.getAs[java.sql.Timestamp]("start").toString)
            doc.put("windowEnd", windowRow.getAs[java.sql.Timestamp]("end").toString)
            doc.put("batchId", Long.box(batchId))
            doc.put("total", Long.box(row.getAs[Long]("total")))
            doc.put("valid", Long.box(row.getAs[Long]("valid")))
            doc.put("invalid", Long.box(row.getAs[Long]("invalid")))
            doc.put("deserializationErrors", Long.box(row.getAs[Long]("deserializationErrors")))
            repository.saveMetricsDocument(doc)
          }
        }
      }
      .option(
        "checkpointLocation",
        runtimeConfig.spark.checkpointBasePath + "/mongo-validation-metrics"
      )
      .start()

    validationQuery.awaitTermination()
    metricsQuery.awaitTermination()
    mongoValidatedQuery.awaitTermination()
    mongoMetricsQuery.awaitTermination()
  }

  private def buildMapper(): ObjectMapper = {
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    mapper.registerModule(new JavaTimeModule())
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    mapper
  }

  private def validateRecord(record: OrderRecord): Seq[String] = {
    val emailErrors =
      if (record.customer.email.contains("@")) Seq.empty[String]
      else Seq("customer.email is invalid")

    val itemsPresenceErrors =
      if (record.items.nonEmpty) Seq.empty[String]
      else Seq("items must not be empty")

    val itemValueErrors = record.items.zipWithIndex.flatMap { case (item, index) =>
      val quantityError =
        if (item.quantity > 0) Seq.empty[String]
        else Seq("items[" + index + "].quantity must be > 0")

      val priceError =
        if (item.price > 0) Seq.empty[String]
        else Seq("items[" + index + "].price must be > 0")

      quantityError ++ priceError
    }

    val paymentErrors =
      try {
        val paymentExpiration = LocalDateTime.parse(record.payment.expirationDate)
        if (paymentExpiration.isAfter(LocalDateTime.now())) Seq.empty[String]
        else Seq("payment.expirationDate must be in the future")
      } catch {
        case _: Exception => Seq("payment.expirationDate has invalid format")
      }

    emailErrors ++ itemsPresenceErrors ++ itemValueErrors ++ paymentErrors
  }
}
