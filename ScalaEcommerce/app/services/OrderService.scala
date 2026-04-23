package scala.ecommerce.services

import javax.inject.Singleton
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.StringSerializer
import scala.ecommerce.config.ProducerConfig
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import scala.ecommerce.models.Order

@Singleton
class OrderService:

  private val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)
  mapper.registerModule(JavaTimeModule())
  mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

  private val (config, topic) = ProducerConfig.getConfig("kafka-intro.conf")
  private val producer = new KafkaProducer[String, String](config, new StringSerializer(), new StringSerializer())

  def produce(order: Order): Either[String, String] =
    try
      val json = mapper.writeValueAsString(order)
      val record = new ProducerRecord[String, String](topic, order.header.orderId, json)
      producer.send(record).get()
      Right(order.header.orderId)
    catch
      case ex: Exception => Left(s"Kafka send failed: ${ex.getMessage}")
