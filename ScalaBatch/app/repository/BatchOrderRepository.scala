package app.repository

import app.config.MongoCfg
import app.models._
import com.mongodb.client.MongoClients
import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.ReplaceOptions
import org.bson.Document
import org.slf4j.LoggerFactory

import scala.jdk.CollectionConverters._
import scala.util.Try

class BatchOrderRepository(mongoConfig: MongoCfg) {
  private val logger = LoggerFactory.getLogger(getClass)
  private val client = MongoClients.create(mongoConfig.uri)
  private val database = client.getDatabase(mongoConfig.database)
  private val ordersCollection = database.getCollection(mongoConfig.ordersCollection)
  private val reportsCollection = database.getCollection(mongoConfig.reportsCollection)

  initializeIndexes()

  // Double-checked locking so multiple threads or repeated instantiations don't race on index creation.
  private def initializeIndexes(): Unit = {
    if (!BatchOrderRepository.indexesInitialized) {
      BatchOrderRepository.synchronized {
        if (!BatchOrderRepository.indexesInitialized) {
          Try {
            ordersCollection.createIndex(
              Indexes.ascending("orderId"),
              new IndexOptions().unique(true)
            )
            reportsCollection.createIndex(
              Indexes.ascending("generatedAt"),
              new IndexOptions().unique(true)
            )
          }.recover {
            case e: Exception => logger.warn(s"Index initialization skipped: ${e.getMessage}")
          }
          BatchOrderRepository.indexesInitialized = true
        }
      }
    }
  }

  def saveValidOrder(record: BatchOrderRecord): Unit = {
    val itemsBson = record.items.map { i =>
      val d = new Document()
      d.put("productId", Int.box(i.productId))
      d.put("name", i.name)
      d.put("price", Int.box(i.price))
      d.put("size", Int.box(i.size))
      d.put("quantity", Int.box(i.quantity))
      d.put("category", i.category)
      d
    }.asJava

    val customer = new Document()
    customer.put("email", record.customer.email)
    customer.put("docType", record.customer.docType)
    customer.put("docNumber", Int.box(record.customer.docNumber))
    customer.put("city", record.customer.city)
    customer.put("department", record.customer.department)
    customer.put("market", record.customer.market)

    val payment = new Document()
    payment.put("currency", record.payment.currency)
    payment.put("installments", Int.box(record.payment.installments))
    payment.put("cardBin", Int.box(record.payment.cardBin))

    val doc = new Document()
    doc.put("orderId", record.orderId)
    doc.put("correlationId", record.correlationId)
    doc.put("processedAt", record.processedAt)
    doc.put("revenue", Long.box(record.revenue))
    doc.put("customer", customer)
    doc.put("payment", payment)
    doc.put("items", itemsBson)

    Try {
      ordersCollection.replaceOne(
        Filters.eq("orderId", record.orderId),
        doc,
        new ReplaceOptions().upsert(true)
      )
    }.recover {
      case e: Exception => logger.error(s"Failed to save batch order ${record.orderId}: ${e.getMessage}")
    }
  }

  // Pulls every persisted valid order so the Spark batch job can aggregate over them.
  // We map BSON → case class manually to avoid extended-JSON round-tripping via Document.toJson.
  def fetchAllOrders(): Seq[BatchOrderRecord] = {
    val cursor = ordersCollection.find().iterator()
    val buffer = scala.collection.mutable.ListBuffer.empty[BatchOrderRecord]
    try {
      while (cursor.hasNext) {
        val d = cursor.next()
        Try(documentToRecord(d)).toOption.foreach(buffer += _)
      }
    } finally cursor.close()
    buffer.toList
  }

  private def documentToRecord(d: Document): BatchOrderRecord = {
    val customer = d.get("customer", classOf[Document])
    val payment = d.get("payment", classOf[Document])
    val rawItems = d.getList("items", classOf[Document]).asScala.toList
    val items = rawItems.map { i =>
      BatchItem(
        productId = i.getInteger("productId"),
        name = i.getString("name"),
        price = i.getInteger("price"),
        size = i.getInteger("size"),
        quantity = i.getInteger("quantity"),
        category = i.getString("category")
      )
    }
    BatchOrderRecord(
      orderId = d.getString("orderId"),
      correlationId = d.getString("correlationId"),
      processedAt = d.getString("processedAt"),
      revenue = Option(d.getLong("revenue")).map(_.longValue()).getOrElse(0L),
      customer = BatchCustomer(
        email = customer.getString("email"),
        docType = customer.getString("docType"),
        docNumber = customer.getInteger("docNumber"),
        city = customer.getString("city"),
        department = customer.getString("department"),
        market = customer.getString("market")
      ),
      payment = BatchPayment(
        currency = payment.getString("currency"),
        installments = payment.getInteger("installments"),
        cardBin = payment.getInteger("cardBin")
      ),
      items = items
    )
  }

  def saveReport(doc: Document): Unit = {
    Try(reportsCollection.insertOne(doc)).recover {
      case e: Exception => logger.error(s"Failed to save daily report: ${e.getMessage}")
    }
  }

  def close(): Unit = Try(client.close())
}

object BatchOrderRepository {
  @volatile private var indexesInitialized: Boolean = false
}
