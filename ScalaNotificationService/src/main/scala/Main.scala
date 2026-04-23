import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer

import java.time.Duration
import java.time.Instant
import java.util.Arrays
import java.util.Properties

case class ValidationMetricsEvent(
  windowStart: String,
  windowEnd: String,
  total: Long,
  valid: Long,
  invalid: Long,
  deserializationErrors: Long
)

case class ProductRank(productId: Int, name: String, totalQuantity: Long)
case class CategoryRank(category: String, revenue: Long)
case class CityRank(city: String, orders: Long)
case class DepartmentRank(department: String, revenue: Long)
case class HourBucket(hour: Int, orders: Long)
case class DocTypeBucket(docType: String, orders: Long)
case class InstallmentsBucket(installments: Int, orders: Long)
case class CurrencyBucket(currency: String, orders: Long)

case class DailyReportEvent(
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

object Main {
  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()

    val bootstrapServers = setting(config, "notification.kafka.bootstrap-servers", "KAFKA_BOOTSTRAP_SERVERS", "localhost:29092")
    val metricsTopic = setting(config, "notification.kafka.metrics-topic", "METRICS_TOPIC", "orders-validation-metrics")
    val dailyReportTopic = setting(config, "notification.kafka.daily-report-topic", "DAILY_REPORT_TOPIC", "daily_report")
    val consumerGroup = setting(config, "notification.kafka.group-id", "NOTIFICATION_GROUP_ID", "scala-notification-service")
    val alertThresholdPercent = settingDouble(config, "notification.alert.error-rate-percent", "ALERT_ERROR_RATE_PERCENT", 20.0)
    val minTotalForAlert = settingLong(config, "notification.alert.min-total", "ALERT_MIN_TOTAL", 20)
    val cooldownSeconds = settingLong(config, "notification.alert.cooldown-seconds", "ALERT_COOLDOWN_SECONDS", 600)
    val emailEnabled = settingBoolean(config, "notification.email.enabled", "NOTIFICATION_EMAIL_ENABLED", false)
    val sendgridApiKey = setting(config, "notification.email.sendgrid.api-key", "SENDGRID_API_KEY", "")
    val emailFrom = setting(config, "notification.email.from", "NOTIFICATION_EMAIL_FROM", "")
    val emailTo = setting(config, "notification.email.to", "NOTIFICATION_EMAIL_TO", "")

    val emailSettings = SendGridSettings(
      enabled = emailEnabled,
      apiKey = sendgridApiKey,
      from = emailFrom,
      to = emailTo
    )

    val props = new Properties()
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup)
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, classOf[StringDeserializer].getName)
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")

    val consumer = new KafkaConsumer[String, String](props)
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    var lastAlertAt: Option[Instant] = None

    consumer.subscribe(Arrays.asList(metricsTopic, dailyReportTopic))
    println(s"Notification service listening on topics '$metricsTopic' and '$dailyReportTopic' (broker=$bootstrapServers)")
    println(s"Alert config: threshold=${alertThresholdPercent}%% minTotal=$minTotalForAlert cooldown=${cooldownSeconds}s")
    println(s"Email config: enabled=${emailSettings.enabled} to=${maskEmail(emailSettings.to)} from=${maskEmail(emailSettings.from)}")

    while (true) {
      val records = consumer.poll(Duration.ofSeconds(2))
      val iterator = records.iterator()

      while (iterator.hasNext) {
        val rec = iterator.next()
        val rawJson = rec.value()

        rec.topic() match {
          case t if t == metricsTopic =>
            try {
              val event = mapper.readValue(rawJson, classOf[ValidationMetricsEvent])

              val errorCount = event.invalid + event.deserializationErrors
              val errorRate = if (event.total > 0) (errorCount.toDouble / event.total.toDouble) * 100.0 else 0.0

              println(
                f"[metrics] window=${event.windowStart} -> ${event.windowEnd} total=${event.total}%d valid=${event.valid}%d invalid=${event.invalid}%d deser=${event.deserializationErrors}%d errorRate=${errorRate}%.2f%%"
              )

              val meetsMinTotal = event.total >= minTotalForAlert
              val meetsThreshold = errorRate >= alertThresholdPercent
              val cooldownReady = shouldSendAlert(lastAlertAt, cooldownSeconds)

              if (meetsMinTotal && meetsThreshold && cooldownReady) {
                sendAlert(event, errorRate, alertThresholdPercent, emailSettings)
                lastAlertAt = Some(Instant.now())
              } else {
                val secondsSinceLastAlert = lastAlertAt.map(last => Duration.between(last, Instant.now()).getSeconds).getOrElse(-1L)
                println(
                  s"[SKIP] No alert. meetsMinTotal=$meetsMinTotal (total=${event.total} min=$minTotalForAlert) meetsThreshold=$meetsThreshold (errorRate=$errorRate threshold=$alertThresholdPercent) cooldownReady=$cooldownReady (secondsSinceLast=$secondsSinceLastAlert cooldown=$cooldownSeconds)"
                )
              }
            } catch {
              case ex: Exception =>
                println(s"[warn] Could not parse metrics event: ${ex.getMessage}; payload=$rawJson")
            }

          case t if t == dailyReportTopic =>
            try {
              val report = mapper.readValue(rawJson, classOf[DailyReportEvent])
              println(s"[report] Received daily report generatedAt=${report.generatedAt} totalOrders=${report.totalOrders} totalRevenue=${report.totalRevenue}")
              sendDailyReport(report, emailSettings)
            } catch {
              case ex: Exception =>
                println(s"[warn] Could not parse daily report event: ${ex.getMessage}; payload=$rawJson")
            }

          case otherTopic =>
            println(s"[warn] Ignoring message from unexpected topic '$otherTopic'")
        }
      }
    }
  }

  private def sendDailyReport(report: DailyReportEvent, emailSettings: SendGridSettings): Unit = {
    val context = DailyReportEmailContext(
      generatedAt = report.generatedAt,
      windowFrom = report.windowFrom,
      windowTo = report.windowTo,
      totalOrders = report.totalOrders,
      totalRevenue = report.totalRevenue,
      averageRevenuePerOrder = report.averageRevenuePerOrder,
      averageItemsPerOrder = report.averageItemsPerOrder,
      creditPurchaseRatio = report.creditPurchaseRatio
    )

    val email = NotificationTemplates.buildDailyReportEmail(context)

    BatchReportExcelBuilder.attachmentFor(report) match {
      case Left(attachmentError) =>
        println(s"[warn] Could not build Excel attachment: $attachmentError")
      case Right(attachment) =>
        if (!emailSettings.enabled) {
          println(s"[email] Email disabled. Subject='${email.subject}' with attachment='${attachment.filename}'")
        } else {
          SendGridMailer.send(
            settings = emailSettings,
            subject = email.subject,
            textBody = email.textBody,
            htmlBody = email.htmlBody,
            attachments = Seq(attachment)
          ) match {
            case Right(statusCode) => println(s"[email] Daily report email sent via SendGrid. status=$statusCode")
            case Left(sendError) => println(s"[warn] SendGrid daily report email failed: $sendError")
          }
        }
    }
  }

  private def shouldSendAlert(lastAlertAt: Option[Instant], cooldownSeconds: Long): Boolean = {
    lastAlertAt match {
      case None => true
      case Some(last) => Duration.between(last, Instant.now()).getSeconds >= cooldownSeconds
    }
  }

  private def sendAlert(
    event: ValidationMetricsEvent,
    errorRate: Double,
    thresholdPercent: Double,
    emailSettings: SendGridSettings
  ): Unit = {
    println(
      f"[ALERT] High error rate detected. window=${event.windowStart} -> ${event.windowEnd} total=${event.total}%d invalid=${event.invalid}%d deser=${event.deserializationErrors}%d errorRate=${errorRate}%.2f%%"
    )

    val context = AlertEmailContext(
      windowStart = event.windowStart,
      windowEnd = event.windowEnd,
      total = event.total,
      valid = event.valid,
      invalid = event.invalid,
      deserializationErrors = event.deserializationErrors,
      errorRatePercent = f"$errorRate%.2f",
      thresholdPercent = f"$thresholdPercent%.2f"
    )

    NotificationTemplates.buildAlertEmail(context) match {
      case Left(templateError) =>
        println(s"[warn] Could not build email template: $templateError")
      case Right(email) =>
        if (!emailSettings.enabled) {
          println(s"[email] Email disabled. Subject='${email.subject}'")
        } else {
          SendGridMailer.send(emailSettings, email.subject, email.textBody, email.htmlBody) match {
            case Right(statusCode) => println(s"[email] Alert email sent via SendGrid. status=$statusCode")
            case Left(sendError) => println(s"[warn] SendGrid email failed: $sendError")
          }
        }
    }
  }

  private def setting(config: com.typesafe.config.Config, key: String, envName: String, defaultValue: String): String = {
    sys.env.get(envName)
      .orElse(if (config.hasPath(key)) Some(config.getString(key)) else None)
      .getOrElse(defaultValue)
  }

  private def settingDouble(config: com.typesafe.config.Config, key: String, envName: String, defaultValue: Double): Double = {
    sys.env.get(envName)
      .flatMap(value => scala.util.Try(value.toDouble).toOption)
      .orElse(if (config.hasPath(key)) scala.util.Try(config.getDouble(key)).toOption else None)
      .getOrElse(defaultValue)
  }

  private def settingLong(config: com.typesafe.config.Config, key: String, envName: String, defaultValue: Long): Long = {
    sys.env.get(envName)
      .flatMap(value => scala.util.Try(value.toLong).toOption)
      .orElse(if (config.hasPath(key)) scala.util.Try(config.getLong(key)).toOption else None)
      .getOrElse(defaultValue)
  }

  private def settingBoolean(config: com.typesafe.config.Config, key: String, envName: String, defaultValue: Boolean): Boolean = {
    sys.env.get(envName)
      .flatMap(value => scala.util.Try(value.toBoolean).toOption)
      .orElse(if (config.hasPath(key)) scala.util.Try(config.getBoolean(key)).toOption else None)
      .getOrElse(defaultValue)
  }

  private def maskEmail(value: String): String = {
    if (value == null || value.isBlank) "(not-set)"
    else {
      val parts = value.split("@")
      if (parts.length != 2) "(invalid-format)"
      else {
        val local = parts(0)
        val domain = parts(1)
        val maskedLocal = if (local.length <= 2) "**" else local.take(2) + "***"
        maskedLocal + "@" + domain
      }
    }
  }
}
