package app.config

import pureconfig.ConfigSource
import pureconfig.generic.auto._

case class KafkaConsumerCfg(
  bootstrapServers: String,
  groupId: String,
  autoOffsetReset: String,
  enableAutoCommit: Boolean,
  keyDeserializer: String,
  valueDeserializer: String,
  pollTimeoutMs: Long
)

case class KafkaProducerCfg(
  bootstrapServers: String,
  keySerializer: String,
  valueSerializer: String
)

case class TopicsCfg(
  input: String,
  reportOutput: String
)

case class MongoCfg(
  uri: String,
  database: String,
  ordersCollection: String,
  reportsCollection: String
)

case class SparkCfg(
  appName: String,
  master: String,
  checkpointBasePath: String
)

case class BatchCfg(
  intervalMinutes: Int
)

case class BatchAppConfig(
  consumer: KafkaConsumerCfg,
  producer: KafkaProducerCfg,
  topics: TopicsCfg,
  mongo: MongoCfg,
  spark: SparkCfg,
  batch: BatchCfg
)

case class RuntimeConfig(
  consumerProps: java.util.Map[String, AnyRef],
  producerProps: java.util.Map[String, AnyRef],
  pollTimeoutMs: Long,
  topics: TopicsCfg,
  mongo: MongoCfg,
  spark: SparkCfg,
  batch: BatchCfg
)

object BatchConfig {
  def getConfig(resource: String): RuntimeConfig = {
    val source = ConfigSource.resources(resource).loadOrThrow[BatchAppConfig]

    val consumerProps = new java.util.HashMap[String, AnyRef]()
    consumerProps.put("bootstrap.servers", source.consumer.bootstrapServers)
    consumerProps.put("group.id", source.consumer.groupId)
    consumerProps.put("auto.offset.reset", source.consumer.autoOffsetReset)
    consumerProps.put("enable.auto.commit", java.lang.Boolean.valueOf(source.consumer.enableAutoCommit))
    consumerProps.put("key.deserializer", source.consumer.keyDeserializer)
    consumerProps.put("value.deserializer", source.consumer.valueDeserializer)

    val producerProps = new java.util.HashMap[String, AnyRef]()
    producerProps.put("bootstrap.servers", source.producer.bootstrapServers)
    producerProps.put("key.serializer", source.producer.keySerializer)
    producerProps.put("value.serializer", source.producer.valueSerializer)

    RuntimeConfig(
      consumerProps = consumerProps,
      producerProps = producerProps,
      pollTimeoutMs = source.consumer.pollTimeoutMs,
      topics = source.topics,
      mongo = source.mongo,
      spark = source.spark,
      batch = source.batch
    )
  }
}
