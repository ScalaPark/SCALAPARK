# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Compile
sbt compile

# Run locally (requires Kafka at localhost:29092)
sbt run

# Stage for production
sbt stage

# Build Docker image
docker build -t scalaecommerce:latest .
```

## Architecture

ScalaEcommerce is a single Play Framework microservice that generates mock e-commerce order events and produces them to Kafka. It is part of the SCALAPARK umbrella alongside ScalaStreaming.

**Request flow:**
```
POST /order/create
  → OrderController
    → OrderGenerator (creates 15 orders, ~70% valid / ~30% intentionally invalid)
      → OrderService (serializes to JSON, publishes to Kafka topic "orders")
```

**Intentional invalid orders** are generated for testing downstream validation: malformed emails, empty item lists, zero-quantity items, and expired payment cards.

**Kafka producer** is a singleton (`OrderService`) configured via PureConfig reading from `conf/kafka-intro.conf`. The Kafka bootstrap servers are set via the `KAFKA_BOOTSTRAP_SERVERS` env var (default: `localhost:29092`). Orders are keyed by `orderId`.

## Key Files

- `app/models/Order.scala` — all domain models (Order, Header, Customer, Location, Payment, Item)
- `app/services/OrderGenerator.scala` — mock data generation with valid/invalid variants
- `app/services/OrderService.scala` — Kafka producer wrapper
- `app/config/ProducerConfig.scala` — PureConfig-based Kafka config loading
- `conf/kafka-intro.conf` — Kafka configuration (imported by `application.conf`)
- `conf/routes` — single route: `POST /order/create`

## Tech Stack

- **Scala 3.3.4** / **Play Framework 3.0.10**
- **Apache Kafka 3.9.2** (kafka-clients)
- **Jackson 2.19.1** with Scala module for JSON serialization
- **PureConfig 0.17.8** for HOCON-based config
- **OpenJDK 17** (eclipse-temurin) in Docker
- **ScalaTest+ Play** added as dependency but no tests are implemented yet

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `APPLICATION_SECRET` | — | Play application secret (required in prod) |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:29092` | Kafka bootstrap servers |
