package app.models

case class BatchCustomer(
  email: String,
  docType: String,
  docNumber: Int,
  city: String,
  department: String,
  market: String
)

case class BatchPayment(
  currency: String,
  installments: Int,
  cardBin: Int
)

case class BatchItem(
  productId: Int,
  name: String,
  price: Int,
  size: Int,
  quantity: Int,
  category: String
)

case class BatchOrderRecord(
  orderId: String,
  correlationId: String,
  processedAt: String,
  revenue: Long,
  customer: BatchCustomer,
  payment: BatchPayment,
  items: Seq[BatchItem]
)

object BatchOrderRecord {
  // Flattens the full OrderRecord (from orders-validated) into the document we persist for analytics.
  // Also precomputes `revenue` so Spark aggregations don't need to re-sum items every run.
  def from(envelope: ValidatedOrderEnvelope): Option[BatchOrderRecord] =
    envelope.order.map { order =>
      val items = order.items.map { i =>
        BatchItem(i.productId, i.name, i.price, i.size, i.quantity, i.category)
      }.toList
      val revenue = items.foldLeft(0L)((acc, i) => acc + i.price.toLong * i.quantity.toLong)
      BatchOrderRecord(
        orderId = envelope.orderId,
        correlationId = envelope.correlationId,
        processedAt = envelope.processedAt,
        revenue = revenue,
        customer = BatchCustomer(
          email = order.customer.email,
          docType = order.customer.docType,
          docNumber = order.customer.docNumber,
          city = order.location.city,
          department = order.location.department,
          market = order.location.market
        ),
        payment = BatchPayment(
          currency = order.payment.currency,
          installments = order.payment.installments,
          cardBin = order.payment.cardBin
        ),
        items = items
      )
    }
}
