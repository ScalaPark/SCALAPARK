# ScalaBatch

Batch analytics service for the **SCALAPARK** e-commerce pipeline. Consumes validated orders, accumulates them in MongoDB, and every N minutes runs a Spark batch job that produces a business-metrics report published to Kafka and persisted to MongoDB.

---

## Role in the Pipeline

SCALAPARK has three services that together form an event-driven order-analysis pipeline:

| Service | Responsibility |
|---|---|
| **ScalaEcommerce** (Play Framework, Scala 3) | Generates mock orders (70% valid, 30% intentionally broken) and publishes them to Kafka topic `orders`. |
| **ScalaStreaming** (Spark Structured Streaming, Scala 2.13) | Validates each order in real-time; publishes per-order results to `orders-validated` and 30-second aggregates to `orders-validation-metrics`. |
| **ScalaBatch** (Spark batch, Scala 2.13) — **this service** | Accumulates only `VALID` orders, then periodically runs a Spark batch job that computes business KPIs and publishes a `daily_report`. |

```
ScalaEcommerce ──▶ orders ──▶ ScalaStreaming ──▶ orders-validated ──▶ ScalaBatch ──▶ daily_report
                                                                          │
                                                                          └──▶ MongoDB
```

---

## Architecture

ScalaBatch runs as a single JVM with two concurrent workers:

```
┌─ ScalaBatch (single JVM) ────────────────────────────────────────┐
│                                                                  │
│  Thread A: continuous ingestion     Thread B: scheduled batch    │
│  ───────────────────────────        ────────────────────────     │
│  KafkaConsumer poll loop            ScheduledExecutor (N min)    │
│  topic: "orders-validated"            │                           │
│    │                                  ▼                           │
│    ▼ filter status == "VALID"       Spark batch job               │
│  upsert by orderId                    │ reads batch_orders        │
│    │                                  ▼ computes DailyReport      │
│    ▼                                  │                           │
│  MongoDB "batch_orders" ◀─── read ────┤                           │
│                                       ├──▶ Kafka "daily_report"   │
│                                       └──▶ MongoDB "batch_reports"│
└──────────────────────────────────────────────────────────────────┘
```

**Concurrency:** `MainBatchApp` starts a daemon thread running the Kafka consumer loop and a `ScheduledExecutorService` that fires the Spark job at a fixed rate (default: every 5 minutes). Both share a single `MongoClient`, a single `ObjectMapper`, and a single `SparkSession` (created once at startup and reused across runs).

**Idempotency:** Kafka offsets are committed manually (`enable.auto.commit = false`, `commitSync` after each poll batch). MongoDB upserts on a unique `orderId` index, so replays after a crash are safe. No local state — MongoDB is the single source of truth.

---

## Prerequisites

- **Docker** + **Docker Compose** (for the containerized pipeline) **OR**
- **JDK 17** + **sbt 1.12.9** + a reachable Kafka broker + a reachable MongoDB (for local `sbt run`).

---

## Quick Start

### Option A — Unified Compose (recommended)

Runs the whole pipeline (ScalaEcommerce + ScalaStreaming + ScalaBatch + Kafka + Zookeeper + MongoDB + Kafka UI) from the SCALAPARK root:

```bash
cd /home/alejo/Desktop/SCALAPARK
cp .env.example .env      # only needed once; fill in the values
docker compose up -d
```

Trigger order generation:

```bash
curl -X POST http://localhost:9000/order/create
```

Watch ScalaBatch logs:

```bash
docker compose logs -f scalabatch
```

Inspect the `daily_report` topic:

```bash
# Kafka UI
open http://localhost:8088

# Or CLI
docker compose exec kafka kafka-console-consumer \
  --bootstrap-server kafka:9092 --topic daily_report --from-beginning
```

Inspect MongoDB:

```bash
docker compose exec mongo mongosh
> use <DATABASE_NAME>
> db.batch_orders.countDocuments()
> db.batch_reports.find().sort({generatedAt: -1}).limit(1).pretty()
```

### Option B — Standalone Compose (ScalaBatch + its own infra only)

Useful if you want an isolated environment with nothing else running:

```bash
cd ScalaBatch
cp .env.example .env
docker compose up -d
```

Note: with no upstream ScalaEcommerce/ScalaStreaming producing into this isolated Kafka, you'd need to publish test messages to `orders-validated` manually to exercise the ingestion path.

### Option C — Local `sbt run`

For fast iteration on the Scala code without rebuilding Docker:

```bash
cd ScalaBatch
cp .env.example .env      # set KAFKA_BOOTSTRAP_SERVERS=localhost:29092 etc.
sbt run
```

---

## Configuration

All config is in `conf/batch.conf` (HOCON, loaded by PureConfig). Values with `${?VAR}` come from environment variables.

### Environment variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `KAFKA_BOOTSTRAP_SERVERS` | yes | — | Kafka brokers (`kafka:9092` inside Compose, `localhost:29092` for host) |
| `MONGO_URI` | yes | — | MongoDB connection string |
| `DATABASE_NAME` | yes | — | MongoDB database name |
| `BATCH_ORDERS_COLLECTION` | yes | — | Collection for accumulated valid orders (e.g. `batch_orders`) |
| `BATCH_REPORTS_COLLECTION` | yes | — | Collection for generated reports (e.g. `batch_reports`) |
| `BATCH_INTERVAL_MINUTES` | no | `5` | Batch job interval in minutes |

The unified `docker-compose.yml` sets `BATCH_ORDERS_COLLECTION`, `BATCH_REPORTS_COLLECTION`, and `BATCH_INTERVAL_MINUTES` inline with sane defaults, so you only need `KAFKA_BOOTSTRAP_SERVERS`, `MONGO_URI`, and `DATABASE_NAME` in the root `.env`.

### Kafka topics

| Topic | Direction | Format |
|---|---|---|
| `orders-validated` | consume | JSON envelope from ScalaStreaming `{ orderId, status, errors, processedAt, order: {...} }` |
| `daily_report` | produce | JSON-serialized `DailyReport`, keyed by `report-<epoch-millis>` |

### MongoDB collections

| Collection | Purpose | Key index |
|---|---|---|
| `batch_orders` | One document per valid order; upserted continuously | `{ orderId: 1 }` unique |
| `batch_reports` | One document per batch run | `{ generatedAt: 1 }` unique |

---

## Metrics Computed

Every batch run produces a `DailyReport` with:

| # | Metric | Field |
|---|---|---|
| 1 | Total valid orders | `totalOrders` |
| 2 | Total revenue | `totalRevenue` |
| 3 | Average revenue per order | `averageRevenuePerOrder` |
| 4 | Top 5 products by quantity sold | `topProducts` |
| 5 | Top 5 categories by revenue | `topCategoriesByRevenue` |
| 6 | Top 5 cities by order count | `topCitiesByOrders` |
| 7 | Top 3 departments by revenue | `topDepartmentsByRevenue` |
| 8 | Hourly distribution of orders | `hourlyDistribution` |
| 9 | Distribution by customer `docType` | `docTypeDistribution` |
| 10 | Distribution by `installments` | `installmentsDistribution` |
| 11 | Average items per order | `averageItemsPerOrder` |
| — | **Extra:** credit-purchase ratio (`installments > 1`) | `creditPurchaseRatio` |
| — | **Extra:** average ticket size | `averageTicketSize` |
| — | **Extra:** currency distribution | `currencyDistribution` |

All metrics are computed with Spark DataFrame / SQL operations over the `batch_orders` collection.

---

## Project Structure

```
ScalaBatch/
├── app/
│   ├── MainBatchApp.scala            # Entry point: consumer thread + scheduler + shutdown hook
│   ├── batch/
│   │   └── DailyReportJob.scala      # Spark batch job computing all metrics
│   ├── config/
│   │   ├── BatchConfig.scala         # PureConfig loader for batch.conf
│   │   ├── KafkaConsumerClient.scala # Poll loop with commitSync (at-least-once)
│   │   └── KafkaJsonProducer.scala   # Jackson-based Kafka producer wrapper
│   ├── models/
│   │   ├── Order.scala               # JSON contract for orders-validated.order
│   │   ├── ValidatedOrderEnvelope.scala  # Outer envelope from ScalaStreaming
│   │   ├── BatchOrderRecord.scala    # Flat MongoDB document model (+ precomputed revenue)
│   │   └── DailyReport.scala         # Output report model
│   └── repository/
│       └── BatchOrderRepository.scala  # MongoDB persistence, unique orderId index
├── conf/
│   ├── batch.conf                    # HOCON with ${?VAR} env interpolation
│   └── logback.xml                   # Logging config (quiet Kafka/Spark/Mongo at WARN)
├── project/
│   ├── build.properties              # sbt 1.12.9
│   └── plugins.sbt                   # sbt-assembly, sbt-scalafmt
├── build.sbt                         # Scala 2.13.16 + Spark 3.5.1 + deps
├── Dockerfile                        # Two-stage: sbtscala/scala-sbt → temurin-jre-alpine
├── docker-compose.yml                # Standalone dev stack (Kafka/ZK/Mongo + app)
├── .env.example                      # Template for .env
├── .dockerignore
├── .gitignore
├── .sbtopts
├── CLAUDE.md                         # Instructions for Claude Code (AI assistant)
└── README.md
```

---

## Development

### Commands

```bash
sbt compile              # compile all sources
sbt run                  # run locally (reads .env automatically)
sbt assembly             # build fat JAR (target/scala-2.13/ScalaBatch-assembly-*.jar)
sbt clean                # clean build outputs

docker build -t scalabatch:latest .
docker run --env-file .env scalabatch:latest
```

### Tech Stack

- **Scala 2.13.16** — Spark 3.5 doesn't yet support Scala 3
- **Apache Spark 3.5.1** — `spark-sql` in batch mode (`SparkSession`, no streaming)
- **Apache Kafka 3.9.2** (kafka-clients) — plain consumer + producer, no Spark-Kafka connector needed
- **MongoDB 5.1.4** (mongodb-driver-sync) — direct BSON `Document` API, no ODM
- **PureConfig 0.17.8** — HOCON loading with typed case classes
- **Jackson 2.19.1** + Scala module + JavaTime module
- **Logback 1.5.18** — logging
- **Dotenv-java 3.0.2** — `.env` loader for local sbt runs

### Runtime requirements

Spark 3.5 on JDK 17 needs the following JVM flags, configured in `build.sbt` (for `sbt run`) and in the `Dockerfile` entrypoint:

```
--add-exports=java.base/sun.nio.ch=ALL-UNNAMED
--add-opens=java.base/sun.nio.ch=ALL-UNNAMED
--add-exports=java.base/sun.security.action=ALL-UNNAMED
--add-opens=java.base/sun.security.action=ALL-UNNAMED
```

---

## Troubleshooting

**Compose port conflicts** (e.g. `0.0.0.0:27017 already allocated`): another Compose stack or local service owns the port. Either stop the other stack, or run ScalaBatch via the unified `SCALAPARK/docker-compose.yml` (which attaches to the shared `kafka_net` without duplicating infra).

**`batch_reports` stays empty**: the first batch run only fires after `BATCH_INTERVAL_MINUTES`, and it's skipped when `batch_orders` is empty. Trigger ScalaEcommerce (`curl -X POST http://localhost:9000/order/create`) and wait one full interval.

**`Unable to deserialize message`** in consumer logs: a message on `orders-validated` didn't match the `ValidatedOrderEnvelope` schema. The loop continues without stopping (by design); the message is skipped.

**Spark errors about `sun.nio.ch`**: the four `--add-exports` / `--add-opens` flags aren't being applied. Check `build.sbt`'s `javaOptions` block (for `sbt run`) or the `Dockerfile` entrypoint (for Docker).

**Duplicate `orderId` rows**: shouldn't happen — the repository upserts on a unique `orderId` index. If you see them, check that `BatchOrderRepository.initializeIndexes()` actually ran (look for the index via `db.batch_orders.getIndexes()`).
