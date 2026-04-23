package app.models

case class ValidatedOrderEvent(
  orderId: String,
  correlationId: String,
  status: String,
  errors: List[String],
  processedAt: String,
  order: Option[OrderRecord],
  rawPayload: Option[String]
)
