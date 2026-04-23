package scala.ecommerce.config

import pureconfig.*

case class KafkaConfig(
  bootstrapServers: String,
  keySerializer: String,
  valueSerializer: String
) derives ConfigReader

case class ProducerConfig(
  producer: KafkaConfig,
  topic: String
) derives ConfigReader

object ProducerConfig:
  def getConfig(resource: String): (java.util.Map[String, AnyRef], String) =
    val source = ConfigSource.resources(resource).loadOrThrow[ProducerConfig]
    val config = java.util.Map.of[String, AnyRef](
      "bootstrap.servers", source.producer.bootstrapServers,
      "key.serializer", source.producer.keySerializer,
      "value.serializer", source.producer.valueSerializer
    )
    (config, source.topic)
