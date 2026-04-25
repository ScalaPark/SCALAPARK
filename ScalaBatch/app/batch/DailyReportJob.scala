package app.batch

import app.models._
import app.repository.BatchOrderRepository
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.Dataset
import org.apache.spark.sql.Row
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.slf4j.LoggerFactory

import java.time.LocalDateTime

// Runs the daily analytics pass over everything currently persisted in `batch_orders`.
// Expected to be triggered on a fixed schedule; one execution builds one DailyReport.
class DailyReportJob(spark: SparkSession, repo: BatchOrderRepository) {
  import spark.implicits._

  private val logger = LoggerFactory.getLogger(getClass)

  def run(): DailyReport = {
    val data = repo.fetchAllOrders()
    val generatedAt = LocalDateTime.now().toString

    if (data.isEmpty) {
      logger.info("No persisted orders available; returning empty report")
      return DailyReport(
        generatedAt = generatedAt,
        windowFrom = generatedAt,
        windowTo = generatedAt,
        totalOrders = 0L,
        totalRevenue = 0L,
        averageRevenuePerOrder = 0d,
        averageItemsPerOrder = 0d,
        averageTicketSize = 0d,
        creditPurchaseRatio = 0d,
        topProducts = Seq.empty,
        topCategoriesByRevenue = Seq.empty,
        topCitiesByOrders = Seq.empty,
        topDepartmentsByRevenue = Seq.empty,
        hourlyDistribution = Seq.empty,
        docTypeDistribution = Seq.empty,
        installmentsDistribution = Seq.empty,
        currencyDistribution = Seq.empty
      )
    }

    val ds: Dataset[BatchOrderRecord] = spark.createDataset(data)
    ds.cache() 

    val totalOrders = ds.count()
    val totalRevenue = ds.agg(sum("revenue")).first().getLong(0)
    val avgRevenue = if (totalOrders == 0) 0d else totalRevenue.toDouble / totalOrders.toDouble

    val avgItems = ds
      .agg(avg(size(col("items"))).as("avgItems"))
      .first()
      .getAs[Double]("avgItems")

    val creditCount = ds.filter(col("payment.installments") > 1).count()
    val creditRatio = if (totalOrders == 0) 0d else creditCount.toDouble / totalOrders.toDouble

    val windowFrom = ds.agg(min("processedAt")).first().getString(0)
    val windowTo = ds.agg(max("processedAt")).first().getString(0)

    val itemsExploded = ds.select(col("revenue"), explode(col("items")).as("item"))

    val topProducts = itemsExploded
      .groupBy(col("item.productId"), col("item.name"))
      .agg(sum(col("item.quantity")).as("totalQuantity"))
      .orderBy(col("totalQuantity").desc)
      .limit(5)
      .collect()
      .toSeq
      .map(r => ProductRank(r.getAs[Int]("productId"), r.getAs[String]("name"), r.getAs[Long]("totalQuantity")))

    val topCategories = itemsExploded
      .withColumn("lineRevenue", col("item.price") * col("item.quantity"))
      .groupBy(col("item.category").as("category"))
      .agg(sum("lineRevenue").as("revenue"))
      .orderBy(col("revenue").desc)
      .limit(5)
      .collect()
      .toSeq
      .map(r => CategoryRank(r.getAs[String]("category"), r.getAs[Long]("revenue")))

    val topCities = ds
      .groupBy(col("customer.city").as("city"))
      .agg(count(lit(1)).as("orders"))
      .orderBy(col("orders").desc)
      .limit(5)
      .collect()
      .toSeq
      .map(r => CityRank(r.getAs[String]("city"), r.getAs[Long]("orders")))

    val topDepartments = ds
      .groupBy(col("customer.department").as("department"))
      .agg(sum("revenue").as("revenue"))
      .orderBy(col("revenue").desc)
      .limit(3)
      .collect()
      .toSeq
      .map(r => DepartmentRank(r.getAs[String]("department"), r.getAs[Long]("revenue")))

    val hourly = ds
      .withColumn("hour", hour(to_timestamp(col("processedAt"))))
      .groupBy(col("hour"))
      .agg(count(lit(1)).as("orders"))
      .orderBy(col("hour"))
      .collect()
      .toSeq
      .map(r => HourBucket(r.getAs[Int]("hour"), r.getAs[Long]("orders")))

    val docType = ds
      .groupBy(col("customer.docType").as("docType"))
      .agg(count(lit(1)).as("orders"))
      .orderBy(col("orders").desc)
      .collect()
      .toSeq
      .map(r => DocTypeBucket(r.getAs[String]("docType"), r.getAs[Long]("orders")))

    val installments = ds
      .groupBy(col("payment.installments").as("installments"))
      .agg(count(lit(1)).as("orders"))
      .orderBy(col("installments"))
      .collect()
      .toSeq
      .map(r => InstallmentsBucket(r.getAs[Int]("installments"), r.getAs[Long]("orders")))

    val currency = ds
      .groupBy(col("payment.currency").as("currency"))
      .agg(count(lit(1)).as("orders"))
      .orderBy(col("orders").desc)
      .collect()
      .toSeq
      .map(r => CurrencyBucket(r.getAs[String]("currency"), r.getAs[Long]("orders")))

    ds.unpersist()

    DailyReport(
      generatedAt = generatedAt,
      windowFrom = windowFrom,
      windowTo = windowTo,
      totalOrders = totalOrders,
      totalRevenue = totalRevenue,
      averageRevenuePerOrder = avgRevenue,
      averageItemsPerOrder = avgItems,
      averageTicketSize = avgRevenue,
      creditPurchaseRatio = creditRatio,
      topProducts = topProducts,
      topCategoriesByRevenue = topCategories,
      topCitiesByOrders = topCities,
      topDepartmentsByRevenue = topDepartments,
      hourlyDistribution = hourly,
      docTypeDistribution = docType,
      installmentsDistribution = installments,
      currencyDistribution = currency
    )
  }
}
