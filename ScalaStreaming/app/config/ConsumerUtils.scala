package app.config

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.time.Duration
import scala.jdk.CollectionConverters._

trait ConsumerUtils[T] {
  protected val logger: Logger = LoggerFactory.getLogger(getClass)

  protected val keyDeSerializer = new StringDeserializer()
  protected val valueDeSerializer = new StringDeserializer()

  protected def poll(
    consumer: KafkaConsumer[String, String],
    timeout: Duration
  ): List[(String, String)] = {
    val records: ConsumerRecords[String, String] = consumer.poll(timeout)
    records.asScala.toList.map((record: ConsumerRecord[String, String]) => (record.key(), record.value()))
  }
}
