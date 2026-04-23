package app.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

import scala.jdk.CollectionConverters._

class KafkaJsonProducer(config: Map[String, AnyRef]) {
  private val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)
  mapper.registerModule(new JavaTimeModule())
  mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

  private val producer = new KafkaProducer[String, String](config.asJava)

  def send(topic: String, key: String, event: AnyRef): Unit = {
    val payload = mapper.writeValueAsString(event)
    producer.send(new ProducerRecord[String, String](topic, key, payload)).get()
  }
}
