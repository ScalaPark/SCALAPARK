package app.models

case class ValidationMetricsEvent(
  windowStart: String,
  windowEnd: String,
  total: Long,
  valid: Long,
  invalid: Long,
  deserializationErrors: Long
)
