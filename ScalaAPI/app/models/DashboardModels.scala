package scalapark.api.models

import play.api.libs.json.{Json, OFormat}

case class ValidationWindowMetrics(
  windowStart: String,
  windowEnd: String,
  total: Long,
  valid: Long,
  invalid: Long,
  deserializationErrors: Long
)
object ValidationWindowMetrics:
  given OFormat[ValidationWindowMetrics] = Json.format[ValidationWindowMetrics]

case class ValidationHistoryPoint(
  id: String,
  time: String,
  total: Long,
  valid: Long,
  invalid: Long,
  errors: Long
)
object ValidationHistoryPoint:
  given OFormat[ValidationHistoryPoint] = Json.format[ValidationHistoryPoint]

case class AnalystDailyStats(
  totalRevenue: Long,
  avgOrderValue: Double,
  newCustomers: Long,
  totalOrders: Long
)
object AnalystDailyStats:
  given OFormat[AnalystDailyStats] = Json.format[AnalystDailyStats]

case class RevenuePoint(
  id: String,
  day: Int,
  revenue: Long
)
object RevenuePoint:
  given OFormat[RevenuePoint] = Json.format[RevenuePoint]

case class TopProductView(
  id: String,
  name: String,
  sales: Long
)
object TopProductView:
  given OFormat[TopProductView] = Json.format[TopProductView]

case class SegmentView(
  id: String,
  name: String,
  value: Int,
  color: String
)
object SegmentView:
  given OFormat[SegmentView] = Json.format[SegmentView]

case class AnalystLatestReport(
  generatedAt: String,
  totalOrders: Long,
  totalRevenue: Long,
  topProducts: Seq[TopProductView],
  customerSegments: Seq[SegmentView]
)
object AnalystLatestReport:
  given OFormat[AnalystLatestReport] = Json.format[AnalystLatestReport]
case class AnalystLatestReport(
  generatedAt: String,
  totalOrders: Long,
  totalRevenue: Long,
  averageRevenuePerOrder: Double,
  averageItemsPerOrder: Double,
  averageTicketSize: Double,
  creditPurchaseRatio: Double,
  topProducts: Seq[TopProductView],
  customerSegments: Seq[SegmentView],
  topCategoriesByRevenue: Seq[CategoryView],
  topCitiesByOrders: Seq[CityView],
  topDepartmentsByRevenue: Seq[DepartmentView],
  hourlyDistribution: Seq[HourBucketView],
  installmentsDistribution: Seq[InstallmentsBucketView],
  currencyDistribution: Seq[CurrencyBucketView]
)
object AnalystLatestReport:
  given OFormat[AnalystLatestReport] = Json.format[AnalystLatestReport]
