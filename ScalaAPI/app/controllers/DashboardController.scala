package scalapark.api.controllers

import javax.inject.{Inject, Singleton}
import play.api.http.ContentTypes
import play.api.libs.json.Json
import play.api.mvc.{BaseController, ControllerComponents}
import scalapark.api.models.*
import scalapark.api.services.DashboardKafkaBridge

import scala.concurrent.ExecutionContext

@Singleton
class DashboardController @Inject() (
  val controllerComponents: ControllerComponents,
  dashboardBridge: DashboardKafkaBridge
)(using ec: ExecutionContext) extends BaseController:

  def operatorValidationWindow() = Action:
    val payload = dashboardBridge.latestMetrics.getOrElse(
      ValidationWindowMetrics("", "", 0L, 0L, 0L, 0L)
    )
    Ok(Json.toJson(payload))

  def operatorValidationHistory(limit: Int) = Action:
    val safeLimit = Math.max(1, Math.min(200, limit))
    Ok(Json.toJson(dashboardBridge.metricsHistorySnapshot(safeLimit)))

  def operatorValidationStream() = Action:
    Ok.chunked(dashboardBridge.metricsEventStream).as(ContentTypes.EVENT_STREAM)
      .withHeaders(
        CACHE_CONTROL -> "no-cache",
        CONNECTION -> "keep-alive"
      )

  def operatorValidatedOrders(limit: Int) = Action:
    val safeLimit = Math.max(1, Math.min(100, limit))
    Ok(Json.toJson(dashboardBridge.latestValidatedOrders(safeLimit)))

  def analystDaily() = Action:
    val payload = dashboardBridge.latestDailyStats.getOrElse(
      AnalystDailyStats(totalRevenue = 0L, avgOrderValue = 0.0, newCustomers = 0L, totalOrders = 0L)
    )
    Ok(Json.toJson(payload))

  def analystRevenueTrend(days: Int) = Action:
    val safeDays = Math.max(7, Math.min(365, days))
    Ok(Json.toJson(dashboardBridge.revenueTrend(safeDays)))

  def analystLatestReport() = Action:
    val payload = dashboardBridge.latestAnalystReport.getOrElse(
      AnalystLatestReport(
        generatedAt = "",
        totalOrders = 0L,
        totalRevenue = 0L,
        topProducts = Seq.empty,
        customerSegments = Seq.empty
      )
    )
    Ok(Json.toJson(payload))

  def analystDailyStream() = Action:
    Ok.chunked(dashboardBridge.analystDailyStream).as(ContentTypes.EVENT_STREAM)
      .withHeaders(
        CACHE_CONTROL -> "no-cache",
        CONNECTION -> "keep-alive"
      )
