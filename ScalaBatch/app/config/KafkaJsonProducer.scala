package app.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

import scala.util.Try

class KafkaJsonProducer(producerProps: java.util.Map[String, AnyRef], mapper: ObjectMapper) {
  private val producer = new KafkaProducer[String, String](producerProps)

  def send(topic: String, key: String, event: AnyRef): Unit = {
    val payload = mapper.writeValueAsString(event)
    producer.send(new ProducerRecord[String, String](topic, key, payload)).get()
  }

  def close(): Unit = Try(producer.close())
}
