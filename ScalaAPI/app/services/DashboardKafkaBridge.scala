package scalapark.api.services

import com.typesafe.config.ConfigFactory
import javax.inject.{Inject, Singleton}
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer}
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import play.api.Logging
import play.api.libs.json.{JsObject, JsValue, Json}
import scalapark.api.models.*

import java.time.{Duration => JDuration, Instant}
import java.util.Properties
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.duration.*
import scala.jdk.CollectionConverters.*

@Singleton
class DashboardKafkaBridge @Inject() () extends Logging:

  private val config = ConfigFactory.load()

  private val bootstrapServers = setting("dashboard.kafka.bootstrap-servers", "KAFKA_BOOTSTRAP_SERVERS", "localhost:29092")
  private val metricsTopic = setting("dashboard.kafka.metrics-topic", "METRICS_TOPIC", "orders-validation-metrics")
  private val dailyReportTopic = setting("dashboard.kafka.daily-report-topic", "DAILY_REPORT_TOPIC", "daily_report")
  private val validatedOrdersTopic = setting("dashboard.kafka.validated-orders-topic", "VALIDATED_ORDERS_TOPIC", "orders-validated")
  private val groupId = setting("dashboard.kafka.group-id", "DASHBOARD_GROUP_ID", "scala-dashboard-api")

  private val latestMetricsRef = new AtomicReference[Option[ValidationWindowMetrics]](None)
  private val latestDailyStatsRef = new AtomicReference[Option[AnalystDailyStats]](None)
  private val latestReportRef = new AtomicReference[Option[AnalystLatestReport]](None)

  private val metricsHistory = new ConcurrentLinkedDeque[ValidationHistoryPoint]()
  private val validatedOrders = new ConcurrentLinkedDeque[JsObject]()
  private val revenueHistory = new ConcurrentLinkedDeque[Long]()

  private val maxMetricsHistory = 300
  private val maxOrdersHistory = 200
  private val maxRevenueHistory = 365

  private val consumer = buildConsumer()
  consumer.subscribe(java.util.Arrays.asList(metricsTopic, dailyReportTopic, validatedOrdersTopic))

  logger.info(s"DashboardKafkaBridge subscribed to topics: $metricsTopic, $dailyReportTopic, $validatedOrdersTopic")

  private val consumerThread = new Thread(
    () => runConsumerLoop(),
    "dashboard-kafka-consumer"
  )
  consumerThread.setDaemon(true)
  consumerThread.start()

  def latestMetrics: Option[ValidationWindowMetrics] = latestMetricsRef.get()

  def metricsHistorySnapshot(limit: Int): Seq[ValidationHistoryPoint] =
    metricsHistory.asScala.toSeq.takeRight(Math.max(1, limit))

  def latestValidatedOrders(limit: Int): Seq[JsObject] =
    validatedOrders.asScala.toSeq.take(Math.max(1, limit))

  def latestDailyStats: Option[AnalystDailyStats] = latestDailyStatsRef.get()

  def latestAnalystReport: Option[AnalystLatestReport] = latestReportRef.get()

  def revenueTrend(days: Int): Seq[RevenuePoint] =
    val safeDays = Math.max(1, Math.min(365, days))
    val points = revenueHistory.asScala.toSeq.takeRight(safeDays)
    points.zipWithIndex.map { case (revenue, idx) =>
      RevenuePoint(id = s"day-${idx + 1}", day = idx + 1, revenue = revenue)
    }

  def metricsEventStream: Source[ByteString, NotUsed] =
    Source
      .tick(500.millis, 2.seconds, ())
      .statefulMapConcat { () =>
        var lastKey = ""
        _ =>
          latestMetricsRef.get() match
            case Some(m) =>
              val key = s"${m.windowStart}|${m.windowEnd}|${m.total}|${m.invalid}|${m.deserializationErrors}"
              if key != lastKey then
                lastKey = key
                List(ByteString(s"data: ${Json.stringify(Json.toJson(m))}\n\n"))
              else Nil
            case None => Nil
      }
      .mapMaterializedValue(_ => NotUsed)

  def analystDailyStream: Source[ByteString, NotUsed] =
    Source
      .tick(1.second, 3.seconds, ())
      .statefulMapConcat { () =>
        var lastKey = ""
        _ =>
          latestDailyStatsRef.get() match
            case Some(stats) =>
              val key = s"${stats.totalRevenue}|${stats.avgOrderValue}|${stats.totalOrders}|${stats.newCustomers}"
              if key != lastKey then
                lastKey = key
                List(ByteString(s"data: ${Json.stringify(Json.toJson(stats))}\n\n"))
              else Nil
            case None => Nil
      }
      .mapMaterializedValue(_ => NotUsed)

  private def runConsumerLoop(): Unit =
    while true do
      try
        val records = consumer.poll(JDuration.ofSeconds(2))
        records.asScala.foreach { rec =>
          rec.topic() match
            case t if t == metricsTopic => handleMetrics(rec.value(), rec.offset())
            case t if t == dailyReportTopic => handleDailyReport(rec.value())
            case t if t == validatedOrdersTopic => handleValidatedOrder(rec.value())
            case other => logger.warn(s"Received message from unexpected topic '$other'")
        }
      catch
        case ex: Exception =>
          logger.error(s"DashboardKafkaBridge consumer loop error: ${ex.getMessage}")

  private def handleMetrics(payload: String, offset: Long): Unit =
    try
      val js = Json.parse(payload)
      val metric = ValidationWindowMetrics(
        windowStart = fieldString(js, "windowStart"),
        windowEnd = fieldString(js, "windowEnd"),
        total = fieldLong(js, "total"),
        valid = fieldLong(js, "valid"),
        invalid = fieldLong(js, "invalid"),
        deserializationErrors = fieldLong(js, "deserializationErrors")
      )

      latestMetricsRef.set(Some(metric))

      val point = ValidationHistoryPoint(
        id = s"metric-${Instant.now().toEpochMilli}-$offset",
        time = metric.windowEnd,
        total = metric.total,
        valid = metric.valid,
        invalid = metric.invalid,
        errors = metric.deserializationErrors
      )
      appendWithLimit(metricsHistory, point, maxMetricsHistory)
    catch
      case ex: Exception => logger.warn(s"Could not parse metrics payload: ${ex.getMessage}")

  private def handleDailyReport(payload: String): Unit =
    try
      val js = Json.parse(payload)
      val totalOrders = fieldLong(js, "totalOrders")
      val totalRevenue = fieldLong(js, "totalRevenue")
      val avgOrderValue = fieldDouble(js, "averageRevenuePerOrder")

      val stats = AnalystDailyStats(
        totalRevenue = totalRevenue,
        avgOrderValue = avgOrderValue,
        newCustomers = Math.round(totalOrders * 0.2d),
        totalOrders = totalOrders
      )
      latestDailyStatsRef.set(Some(stats))
      appendWithLimit(revenueHistory, totalRevenue, maxRevenueHistory)

      val topProducts = fieldArray(js, "topProducts").zipWithIndex.map {
        case (p, idx) =>
          TopProductView(
            id = s"prod-${idx + 1}",
            name = fieldString(p, "name", s"Product-${idx + 1}"),
            sales = fieldLong(p, "totalQuantity")
          )
      }

      val docDist = fieldArray(js, "docTypeDistribution")
      val totalDocs = docDist.map(o => fieldLong(o, "orders")).sum
      val palette = Seq("#00ff88", "#ff4fd8", "#00d4ff", "#fbbf24", "#60a5fa")
      val segments = docDist.zipWithIndex.map { case (d, idx) =>
        val count = fieldLong(d, "orders")
        val pct = if totalDocs == 0 then 0 else Math.round((count.toDouble / totalDocs.toDouble) * 100.0).toInt
        SegmentView(
          id = s"seg-${idx + 1}",
          name = fieldString(d, "docType", s"Segment-${idx + 1}"),
          value = pct,
          color = palette(idx % palette.size)
        )
      }

      val report = AnalystLatestReport(
        generatedAt = fieldString(js, "generatedAt"),
        totalOrders = totalOrders,
        totalRevenue = totalRevenue,
        topProducts = topProducts,
        customerSegments = segments
      )
      latestReportRef.set(Some(report))
    catch
      case ex: Exception => logger.warn(s"Could not parse daily report payload: ${ex.getMessage}")

  private def handleValidatedOrder(payload: String): Unit =
    try
      val js = Json.parse(payload).as[JsObject]
      appendWithLimitHead(validatedOrders, js, maxOrdersHistory)
    catch
      case ex: Exception => logger.warn(s"Could not parse validated order payload: ${ex.getMessage}")

  private def appendWithLimit[A](deque: ConcurrentLinkedDeque[A], value: A, maxSize: Int): Unit =
    deque.addLast(value)
    while deque.size() > maxSize do deque.pollFirst()

  private def appendWithLimitHead[A](deque: ConcurrentLinkedDeque[A], value: A, maxSize: Int): Unit =
    deque.addFirst(value)
    while deque.size() > maxSize do deque.pollLast()

  private def buildConsumer(): KafkaConsumer[String, String] =
    val props = new Properties()
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId)
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
    new KafkaConsumer[String, String](props)

  private def setting(configKey: String, envName: String, defaultValue: String): String =
    sys.env.get(envName)
      .orElse(if config.hasPath(configKey) then Some(config.getString(configKey)) else None)
      .getOrElse(defaultValue)

  private def fieldString(js: JsValue, name: String, default: String = ""): String =
    (js \ name).asOpt[String].getOrElse(default)

  private def fieldLong(js: JsValue, name: String, default: Long = 0L): Long =
    (js \ name).asOpt[Long].getOrElse(default)

  private def fieldDouble(js: JsValue, name: String, default: Double = 0.0): Double =
    (js \ name).asOpt[Double].getOrElse(default)

  private def fieldArray(js: JsValue, name: String): Seq[JsObject] =
    (js \ name).asOpt[Seq[JsObject]].getOrElse(Seq.empty)
