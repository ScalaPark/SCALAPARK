package app.models

case class ProductRank(productId: Int, name: String, totalQuantity: Long)
case class CategoryRank(category: String, revenue: Long)
case class CityRank(city: String, orders: Long)
case class DepartmentRank(department: String, revenue: Long)
case class HourBucket(hour: Int, orders: Long)
case class DocTypeBucket(docType: String, orders: Long)
case class InstallmentsBucket(installments: Int, orders: Long)
case class CurrencyBucket(currency: String, orders: Long)

case class DailyReport(
  generatedAt: String,
  windowFrom: String,
  windowTo: String,
  totalOrders: Long,
  totalRevenue: Long,
  averageRevenuePerOrder: Double,
  averageItemsPerOrder: Double,
  averageTicketSize: Double,
  creditPurchaseRatio: Double,
  topProducts: Seq[ProductRank],
  topCategoriesByRevenue: Seq[CategoryRank],
  topCitiesByOrders: Seq[CityRank],
  topDepartmentsByRevenue: Seq[DepartmentRank],
  hourlyDistribution: Seq[HourBucket],
  docTypeDistribution: Seq[DocTypeBucket],
  installmentsDistribution: Seq[InstallmentsBucket],
  currencyDistribution: Seq[CurrencyBucket]
)
