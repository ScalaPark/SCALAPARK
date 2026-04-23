package app.config

import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory

import java.time.Duration
import java.util.Collections
import scala.jdk.CollectionConverters._

// Long-running Kafka poller. Runs on its own daemon thread and invokes `handler` for each
// message payload. Offsets are committed synchronously after every poll batch so that a
// crash at most replays the last batch (at-least-once), and the `orderId` unique upsert
// in Mongo makes replays idempotent.
class KafkaConsumerClient(
  consumerProps: java.util.Map[String, AnyRef],
  topic: String,
  pollTimeoutMs: Long
) {
  private val logger = LoggerFactory.getLogger(getClass)
  private val consumer = new KafkaConsumer[String, String](consumerProps)
  @volatile private var running = true

  consumer.subscribe(Collections.singletonList(topic))

  def runLoop(handler: String => Unit): Unit = {
    logger.info(s"Subscribed to topic '$topic', starting poll loop")
    try {
      while (running && !Thread.currentThread().isInterrupted) {
        val records = consumer.poll(Duration.ofMillis(pollTimeoutMs))
        records.asScala.foreach { record =>
          try handler(record.value())
          catch {
            case e: Exception =>
              logger.error(s"Handler failed for record at offset=${record.offset()}: ${e.getMessage}")
          }
        }
        if (!records.isEmpty) consumer.commitSync()
      }
    } finally {
      try consumer.close()
      catch { case _: Exception => () }
    }
  }

  def stop(): Unit = {
    running = false
    consumer.wakeup()
  }
}
