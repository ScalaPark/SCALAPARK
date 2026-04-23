package app.config

import pureconfig.ConfigSource
import pureconfig.generic.auto._

case class KafkaConsumerConfig(
  bootstrapServers: String,
  groupId: String,
  autoOffsetReset: String,
  enableAutoCommit: Boolean,
  keyDeserializer: String,
  valueDeserializer: String
)

case class KafkaProducerConfig(
  bootstrapServers: String,
  keySerializer: String,
  valueSerializer: String
)

case class TopicsConfig(
  input: String,
  validatedOutput: String,
  metricsOutput: String
)

case class MetricsConfig(
  publishIntervalSeconds: Int
)

case class MongoConfig(
  uri: String,
  database: String,
  collection: String,
  metricsCollection: String
)

case class SparkConfig(
  appName: String,
  master: String,
  checkpointBasePath: String,
  triggerProcessingTime: String
)

case class ConsumerAppConfig(
  consumer: KafkaConsumerConfig,
  producer: KafkaProducerConfig,
  topics: TopicsConfig,
  metrics: MetricsConfig,
  mongo: MongoConfig,
  spark: SparkConfig
)

case class RuntimeConfig(
  consumerProps: Map[String, AnyRef],
  producerProps: Map[String, AnyRef],
  topics: TopicsConfig,
  metrics: MetricsConfig,
  mongo: MongoConfig,
  spark: SparkConfig
)

object ConsumerConfig {
  def getConfig(resource: String): RuntimeConfig = {
    val source = ConfigSource.resources(resource).loadOrThrow[ConsumerAppConfig]
    val consumerProps = Map[String, AnyRef](
      "bootstrap.servers" -> source.consumer.bootstrapServers,
      "group.id" -> source.consumer.groupId,
      "auto.offset.reset" -> source.consumer.autoOffsetReset,
      "enable.auto.commit" -> java.lang.Boolean.valueOf(source.consumer.enableAutoCommit),
      "key.deserializer" -> source.consumer.keyDeserializer,
      "value.deserializer" -> source.consumer.valueDeserializer
    )
    val producerProps = Map[String, AnyRef](
      "bootstrap.servers" -> source.producer.bootstrapServers,
      "key.serializer" -> source.producer.keySerializer,
      "value.serializer" -> source.producer.valueSerializer
    )
    RuntimeConfig(consumerProps, producerProps, source.topics, source.metrics, source.mongo, source.spark)
  }
}
