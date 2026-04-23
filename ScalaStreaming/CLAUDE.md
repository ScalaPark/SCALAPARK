# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

ScalaStreaming is a microservice that consumes orders from Kafka, validates them using Apache Spark Structured Streaming, and writes results to both Kafka output topics and MongoDB.

**Tech stack**: Scala 2.13, Apache Spark 3.5.1, Apache Kafka, MongoDB, PureConfig, SBT.

## Commands

```bash
# Compile
sbt compile

# Run locally (requires .env file and running Kafka/MongoDB)
sbt run

# Build fat JAR for deployment
sbt assembly
# Output: target/scala-2.13/ScalaStreaming-assembly-0.1.0-SNAPSHOT.jar

# Format code
sbt scalafmt

# Docker build & run
docker build -t scalastreaming:latest .
docker run --env-file .env scalastreaming:latest
```

There is no test suite at this time.

## Architecture

### Data Flow

```
Kafka topic: orders
    → Spark Structured Streaming (deserialization + validation)
        → Kafka topic: orders-validated
        → Kafka topic: orders-validation-metrics  (30s windows)
        → MongoDB: validated orders collection
        → MongoDB: metrics collection
```

### Key Files

- `app/streaming/OrderSparkStreamingJob.scala` — main entry point; sets up all streaming queries and sinks
- `app/config/ConsumerConfig.scala` — PureConfig-based config loader; all settings come from `conf/kafka-intro.conf` + env vars
- `app/config/KafkaJsonProducer.scala` — Kafka producer utilities
- `app/config/JsonStringDeSerializer.scala` — JSON serialization/deserialization helpers
- `app/models/Order.scala` — domain models (Header, Customer, Location, Payment, Item, OrderRecord)
- `app/models/ValidatedOrderEvent.scala` / `ValidationMetricsEvent.scala` — output event types
- `app/repository/ValidatedOrderRepository.scala` — MongoDB persistence; creates unique indexes on (topic, partition, offset)

### Configuration

Runtime config lives in `conf/kafka-intro.conf` (HOCON). Sensitive values are injected via environment variables — copy `.env.example` to `.env` before running:

| Variable | Description |
|---|---|
| `KAFKA_BOOTSTRAP_SERVERS` | e.g. `localhost:29092` |
| `MONGO_URI` | MongoDB connection string |
| `DATABASE_NAME` | MongoDB database |
| `COLLECTION_NAME` | Collection for validated orders |
| `COLLECTION_METRICS` | Collection for validation metrics |

Spark checkpoints are stored at `/tmp/pfsd-spark-checkpoints` by default.

### Validation Logic

Orders are validated in `OrderSparkStreamingJob.scala` against: email format, non-empty items list, positive quantities/prices, and future payment expiration date. Results flow into 30-second tumbling windows for metrics aggregation.

### JVM Requirements

The build and Dockerfile both configure required JVM flags for Spark's NIO usage:
```
--add-exports=java.base/sun.nio.ch=ALL-UNNAMED
--add-opens=java.base/sun.nio.ch=ALL-UNNAMED
--add-exports=java.base/sun.security.action=ALL-UNNAMED
--add-opens=java.base/sun.security.action=ALL-UNNAMED
```
These are already set in `build.sbt` (`javaOptions`) and the Dockerfile — do not remove them.
