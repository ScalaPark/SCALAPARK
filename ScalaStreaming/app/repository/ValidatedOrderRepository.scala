package app.repository

import app.config.MongoConfig
import app.models.ValidatedOrderEvent
import app.models.ValidationMetricsEvent
import com.mongodb.DuplicateKeyException
import com.mongodb.client.MongoClients
import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.ReplaceOptions
import org.bson.Document
import org.slf4j.LoggerFactory

import scala.util.Try

class ValidatedOrderRepository(mongoConfig: MongoConfig) {
  private val logger = LoggerFactory.getLogger(getClass)
  private val client = MongoClients.create(mongoConfig.uri)
  private val database = client.getDatabase(mongoConfig.database)
  private val validatedOrdersCollection = database.getCollection(mongoConfig.collection)
  private val metricsCollection = database.getCollection(mongoConfig.metricsCollection)

  initializeIndexes()

  private def initializeIndexes(): Unit = {
    if (!ValidatedOrderRepository.indexesInitialized) {
      ValidatedOrderRepository.synchronized {
        if (!ValidatedOrderRepository.indexesInitialized) {
          try {
            validatedOrdersCollection.createIndex(
              Indexes.ascending("topic", "partition", "offset"),
              new IndexOptions()
                .unique(true)
                .partialFilterExpression(
                  Filters.and(
                    Filters.exists("topic", true),
                    Filters.exists("partition", true),
                    Filters.exists("offset", true)
                  )
                )
            )

            metricsCollection.createIndex(
              Indexes.ascending("windowStart", "windowEnd"),
              new IndexOptions().unique(true)
            )
          } catch {
            case _: DuplicateKeyException =>

          }

          ValidatedOrderRepository.indexesInitialized = true
        }
      }
    }
  }

  def saveValidatedOrder(event: ValidatedOrderEvent): Unit = {
    val doc = new Document()
    doc.put("orderId", event.orderId)
    doc.put("correlationId", event.correlationId)
    doc.put("status", event.status)
    doc.put("errors", event.errors.mkString("; "))
    doc.put("processedAt", event.processedAt)
    doc.put("rawPayload", event.rawPayload.getOrElse(""))
    doc.put("order", event.order.map(_.toString).getOrElse("N/A"))
    Try(validatedOrdersCollection.insertOne(doc)).recover {
      case e: Exception => logger.error(s"Failed to save validated order ${event.orderId}: ${e.getMessage}")
    }
  }

  def saveMetrics(event: ValidationMetricsEvent): Unit = {
    val doc = new Document()
    doc.put("windowStart", event.windowStart)
    doc.put("windowEnd", event.windowEnd)
    doc.put("total", Long.box(event.total))
    doc.put("valid", Long.box(event.valid))
    doc.put("invalid", Long.box(event.invalid))
    doc.put("deserializationErrors", Long.box(event.deserializationErrors))
    Try(metricsCollection.insertOne(doc)).recover {
      case e: Exception => logger.error(s"Failed to save metrics for window ${event.windowStart}: ${e.getMessage}")
    }
  }

  def saveValidatedDocument(doc: Document): Unit = {
    val filter = Filters.and(
      Filters.eq("topic", doc.getString("topic")),
      Filters.eq("partition", doc.getInteger("partition")),
      Filters.eq("offset", doc.getLong("offset"))
    )
    validatedOrdersCollection.replaceOne(filter, doc, new ReplaceOptions().upsert(true))
  }

  def saveMetricsDocument(doc: Document): Unit = {
    val filter = Filters.and(
      Filters.eq("windowStart", doc.getString("windowStart")),
      Filters.eq("windowEnd", doc.getString("windowEnd"))
    )
    metricsCollection.replaceOne(filter, doc, new ReplaceOptions().upsert(true))
  }
}

object ValidatedOrderRepository {
  @volatile private var indexesInitialized: Boolean = false
}
