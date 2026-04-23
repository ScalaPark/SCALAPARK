package app.models

case class ValidatedOrderEnvelope(
  orderId: String,
  correlationId: String,
  status: String,
  errors: List[String],
  processedAt: String,
  order: Option[OrderRecord]
)
