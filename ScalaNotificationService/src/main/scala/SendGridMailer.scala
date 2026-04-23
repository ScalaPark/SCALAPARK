import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xddf.usermodel.chart._
import org.apache.poi.xssf.usermodel.{XSSFDrawing, XSSFSheet, XSSFWorkbook}

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64
import scala.io.Source
import scala.util.Using

case class SendGridSettings(enabled: Boolean, apiKey: String, from: String, to: String)

case class AlertEmailContext(
  windowStart: String,
  windowEnd: String,
  total: Long,
  valid: Long,
  invalid: Long,
  deserializationErrors: Long,
  errorRatePercent: String,
  thresholdPercent: String
)

case class RenderedEmail(subject: String, textBody: String, htmlBody: String)
case class EmailAttachment(filename: String, mimeType: String, contentBase64: String)

case class DailyReportEmailContext(
  generatedAt: String,
  windowFrom: String,
  windowTo: String,
  totalOrders: Long,
  totalRevenue: Long,
  averageRevenuePerOrder: Double,
  averageItemsPerOrder: Double,
  creditPurchaseRatio: Double
)

object NotificationTemplates {
  private val subjectTemplate = "[PFSD Alert] High order validation error rate"
  private val batchSubjectTemplate = "[PFSD Report] Daily batch analytics report"

  def buildAlertEmail(context: AlertEmailContext): Either[String, RenderedEmail] = {
    val values = Map(
      "windowStart" -> context.windowStart,
      "windowEnd" -> context.windowEnd,
      "total" -> context.total.toString,
      "valid" -> context.valid.toString,
      "invalid" -> context.invalid.toString,
      "deserializationErrors" -> context.deserializationErrors.toString,
      "errorRatePercent" -> context.errorRatePercent,
      "thresholdPercent" -> context.thresholdPercent
    )

    for {
      htmlTemplate <- loadResource("email-templates/alert-high-error-rate.html")
      textTemplate <- loadResource("email-templates/alert-high-error-rate.txt")
    } yield {
      RenderedEmail(
        subject = subjectTemplate,
        textBody = render(textTemplate, values),
        htmlBody = render(htmlTemplate, values)
      )
    }
  }

  def buildDailyReportEmail(context: DailyReportEmailContext): RenderedEmail = {
    val ratioPercent = f"${context.creditPurchaseRatio * 100.0}%.2f"
    RenderedEmail(
      subject = batchSubjectTemplate,
      textBody =
        s"""Daily batch report generated.
           |
           |Generated At: ${context.generatedAt}
           |Window: ${context.windowFrom} -> ${context.windowTo}
           |Total Orders: ${context.totalOrders}
           |Total Revenue: ${context.totalRevenue}
           |Average Revenue / Order: ${f"${context.averageRevenuePerOrder}%.2f"}
           |Average Items / Order: ${f"${context.averageItemsPerOrder}%.2f"}
           |Credit Purchase Ratio: $ratioPercent%
           |
           |Attached file: daily-report.xlsx
           |""".stripMargin,
      htmlBody =
        s"""
           |<h2>Daily batch report generated</h2>
           |<p><strong>Generated At:</strong> ${context.generatedAt}</p>
           |<p><strong>Window:</strong> ${context.windowFrom} -> ${context.windowTo}</p>
           |<ul>
           |  <li><strong>Total Orders:</strong> ${context.totalOrders}</li>
           |  <li><strong>Total Revenue:</strong> ${context.totalRevenue}</li>
           |  <li><strong>Average Revenue / Order:</strong> ${f"${context.averageRevenuePerOrder}%.2f"}</li>
           |  <li><strong>Average Items / Order:</strong> ${f"${context.averageItemsPerOrder}%.2f"}</li>
           |  <li><strong>Credit Purchase Ratio:</strong> $ratioPercent%</li>
           |</ul>
           |<p>See attached file <strong>daily-report.xlsx</strong> for the detailed report.</p>
           |""".stripMargin
    )
  }

  private def loadResource(path: String): Either[String, String] = {
    val streamOpt = Option(getClass.getClassLoader.getResourceAsStream(path))
    streamOpt match {
      case None => Left(s"Template not found: $path")
      case Some(stream) =>
        Using.resource(Source.fromInputStream(stream, "UTF-8")) { source =>
          Right(source.mkString)
        }
    }
  }

  private def render(template: String, values: Map[String, String]): String = {
    values.foldLeft(template) { case (acc, (key, value)) =>
      acc.replace(s"{{$key}}", value)
    }
  }
}

object SendGridMailer {
  private val httpClient = HttpClient.newHttpClient()
  private val mapper = new ObjectMapper().registerModule(DefaultScalaModule)

  private def attachmentEntry(att: EmailAttachment): Map[String, String] =
    Map(
      "content" -> att.contentBase64,
      "type" -> att.mimeType,
      "filename" -> att.filename,
      "disposition" -> "attachment"
    )

  def send(
    settings: SendGridSettings,
    subject: String,
    textBody: String,
    htmlBody: String,
    attachments: Seq[EmailAttachment] = Seq.empty
  ): Either[String, Int] = {
    if (settings.apiKey.isBlank) return Left("Missing SendGrid API key")
    if (settings.from.isBlank) return Left("Missing sender email (notification.email.from)")
    if (settings.to.isBlank) return Left("Missing recipient email (notification.email.to)")

    val basePayload = Map(
      "personalizations" -> List(
        Map(
          "to" -> List(Map("email" -> settings.to))
        )
      ),
      "from" -> Map("email" -> settings.from),
      "subject" -> subject,
      "content" -> List(
        Map("type" -> "text/plain", "value" -> textBody),
        Map("type" -> "text/html", "value" -> htmlBody)
      )
    )

    val payload =
      if (attachments.nonEmpty) basePayload + ("attachments" -> attachments.map(attachmentEntry))
      else basePayload

    val requestBody = mapper.writeValueAsString(payload)

    val request = HttpRequest.newBuilder()
      .uri(URI.create("https://api.sendgrid.com/v3/mail/send"))
      .header("Authorization", s"Bearer ${settings.apiKey}")
      .header("Content-Type", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
      .build()

    try {
      val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
      val status = response.statusCode()
      if (status >= 200 && status < 300) Right(status)
      else Left(s"HTTP $status body=${response.body()}")
    } catch {
      case ex: Exception => Left(ex.getMessage)
    }
  }
}

object BatchReportExcelBuilder {
  private val dateFmt = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

  def attachmentFor(report: DailyReportEvent): Either[String, EmailAttachment] = {
    try {
      val workbook = new XSSFWorkbook()

      val dashboard = workbook.createSheet("Dashboard")
      writeDashboard(dashboard, report)

      val summary = workbook.createSheet("Summary")
      writeSummary(summary, report)

      val topProducts = workbook.createSheet("TopProducts")
      writeRows(topProducts, Seq("productId", "name", "totalQuantity"), report.topProducts.map(r => Seq(r.productId.toString, r.name, r.totalQuantity.toString)))

      val topCategories = workbook.createSheet("TopCategories")
      writeRows(topCategories, Seq("category", "revenue"), report.topCategoriesByRevenue.map(r => Seq(r.category, r.revenue.toString)))

      val topCities = workbook.createSheet("TopCities")
      writeRows(topCities, Seq("city", "orders"), report.topCitiesByOrders.map(r => Seq(r.city, r.orders.toString)))

      val departments = workbook.createSheet("TopDepartments")
      writeRows(departments, Seq("department", "revenue"), report.topDepartmentsByRevenue.map(r => Seq(r.department, r.revenue.toString)))

      val hourly = workbook.createSheet("HourlyDistribution")
      writeRows(hourly, Seq("hour", "orders"), report.hourlyDistribution.map(r => Seq(r.hour.toString, r.orders.toString)))

      val docType = workbook.createSheet("DocTypeDistribution")
      writeRows(docType, Seq("docType", "orders"), report.docTypeDistribution.map(r => Seq(r.docType, r.orders.toString)))

      val installments = workbook.createSheet("InstallmentsDistribution")
      writeRows(installments, Seq("installments", "orders"), report.installmentsDistribution.map(r => Seq(r.installments.toString, r.orders.toString)))

      val currency = workbook.createSheet("CurrencyDistribution")
      writeRows(currency, Seq("currency", "orders"), report.currencyDistribution.map(r => Seq(r.currency, r.orders.toString)))

      val out = new ByteArrayOutputStream()
      workbook.write(out)
      workbook.close()

      val encoded = Base64.getEncoder.encodeToString(out.toByteArray)
      val generatedAtSafe = safeTimestamp(report.generatedAt)
      Right(
        EmailAttachment(
          filename = s"daily-report-$generatedAtSafe.xlsx",
          mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
          contentBase64 = encoded
        )
      )
    } catch {
      case ex: Exception => Left(ex.getMessage)
    }
  }

  private def writeSummary(sheet: org.apache.poi.ss.usermodel.Sheet, report: DailyReportEvent): Unit = {
    val rows = Seq(
      "generatedAt" -> report.generatedAt,
      "windowFrom" -> report.windowFrom,
      "windowTo" -> report.windowTo,
      "totalOrders" -> report.totalOrders.toString,
      "totalRevenue" -> report.totalRevenue.toString,
      "averageRevenuePerOrder" -> f"${report.averageRevenuePerOrder}%.2f",
      "averageItemsPerOrder" -> f"${report.averageItemsPerOrder}%.2f",
      "averageTicketSize" -> f"${report.averageTicketSize}%.2f",
      "creditPurchaseRatio" -> f"${report.creditPurchaseRatio * 100.0}%.2f%%"
    )

    rows.zipWithIndex.foreach { case ((k, v), idx) =>
      val row = sheet.createRow(idx)
      row.createCell(0).setCellValue(k)
      row.createCell(1).setCellValue(v)
    }
    sheet.autoSizeColumn(0)
    sheet.autoSizeColumn(1)
  }

  private def writeDashboard(sheet: org.apache.poi.ss.usermodel.Sheet, report: DailyReportEvent): Unit = {
    val titleRow = sheet.createRow(0)
    titleRow.createCell(0).setCellValue("PFSD - Daily Executive Report")

    val metaRows = Seq(
      "Generated at" -> report.generatedAt,
      "Window" -> s"${report.windowFrom} -> ${report.windowTo}"
    )
    metaRows.zipWithIndex.foreach { case ((k, v), idx) =>
      val row = sheet.createRow(idx + 2)
      row.createCell(0).setCellValue(k)
      row.createCell(1).setCellValue(v)
    }

    val kpis = Seq(
      "Total Orders" -> report.totalOrders.toDouble,
      "Total Revenue" -> report.totalRevenue.toDouble,
      "Avg Revenue / Order" -> report.averageRevenuePerOrder,
      "Avg Items / Order" -> report.averageItemsPerOrder,
      "Avg Ticket Size" -> report.averageTicketSize,
      "Credit Purchase Ratio (%)" -> (report.creditPurchaseRatio * 100.0)
    )

    kpis.zipWithIndex.foreach { case ((name, value), idx) =>
      val row = sheet.createRow(idx + 6)
      row.createCell(0).setCellValue(name)
      row.createCell(1).setCellValue(value)
    }

    val categoryStart = 6
    writeTable(
      sheet,
      startRow = categoryStart,
      startCol = 4,
      headers = Seq("Category", "Revenue"),
      rows = report.topCategoriesByRevenue.map(r => Seq(r.category, r.revenue.toString))
    )

    val citiesStart = 6
    writeTable(
      sheet,
      startRow = citiesStart,
      startCol = 8,
      headers = Seq("City", "Orders"),
      rows = report.topCitiesByOrders.map(r => Seq(r.city, r.orders.toString))
    )

    val currencyStart = 16
    writeTable(
      sheet,
      startRow = currencyStart,
      startCol = 4,
      headers = Seq("Currency", "Orders"),
      rows = report.currencyDistribution.map(r => Seq(r.currency, r.orders.toString))
    )

    val xssfSheet = sheet.asInstanceOf[XSSFSheet]

    createBarChart(
      sheet = xssfSheet,
      title = "Revenue by Top Categories",
      categoryCol = 4,
      valueCol = 5,
      firstDataRow = categoryStart + 1,
      lastDataRow = categoryStart + report.topCategoriesByRevenue.size,
      anchor = (0, 18, 7, 34),
      seriesTitle = "Revenue"
    )

    createBarChart(
      sheet = xssfSheet,
      title = "Orders by Top Cities",
      categoryCol = 8,
      valueCol = 9,
      firstDataRow = citiesStart + 1,
      lastDataRow = citiesStart + report.topCitiesByOrders.size,
      anchor = (8, 18, 15, 34),
      seriesTitle = "Orders"
    )

    createPieChart(
      sheet = xssfSheet,
      title = "Orders by Currency",
      categoryCol = 4,
      valueCol = 5,
      firstDataRow = currencyStart + 1,
      lastDataRow = currencyStart + report.currencyDistribution.size,
      anchor = (16, 18, 23, 34)
    )

    0.to(12).foreach(sheet.autoSizeColumn)
  }

  private def writeTable(
    sheet: org.apache.poi.ss.usermodel.Sheet,
    startRow: Int,
    startCol: Int,
    headers: Seq[String],
    rows: Seq[Seq[String]]
  ): Unit = {
    val header = sheet.createRow(startRow)
    headers.zipWithIndex.foreach { case (h, idx) =>
      header.createCell(startCol + idx).setCellValue(h)
    }

    rows.zipWithIndex.foreach { case (rowValues, rowOffset) =>
      val row = sheet.createRow(startRow + 1 + rowOffset)
      rowValues.zipWithIndex.foreach { case (value, colOffset) =>
        val cell = row.createCell(startCol + colOffset)
        if (colOffset == headers.size - 1) {
          val maybeNumber = scala.util.Try(value.toDouble).toOption
          maybeNumber match {
            case Some(n) => cell.setCellValue(n)
            case None => cell.setCellValue(value)
          }
        } else {
          cell.setCellValue(value)
        }
      }
    }
  }

  private def createBarChart(
    sheet: XSSFSheet,
    title: String,
    categoryCol: Int,
    valueCol: Int,
    firstDataRow: Int,
    lastDataRow: Int,
    anchor: (Int, Int, Int, Int),
    seriesTitle: String
  ): Unit = {
    if (lastDataRow < firstDataRow) return

    val drawing = sheet.createDrawingPatriarch().asInstanceOf[XSSFDrawing]
    val chartAnchor = drawing.createAnchor(0, 0, 0, 0, anchor._1, anchor._2, anchor._3, anchor._4)
    val chart = drawing.createChart(chartAnchor)

    chart.setTitleText(title)
    chart.setTitleOverlay(false)

    val bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM)
    val leftAxis = chart.createValueAxis(AxisPosition.LEFT)
    leftAxis.setCrosses(AxisCrosses.AUTO_ZERO)

    val categories = XDDFDataSourcesFactory.fromStringCellRange(
      sheet,
      new CellRangeAddress(firstDataRow, lastDataRow, categoryCol, categoryCol)
    )
    val values = XDDFDataSourcesFactory.fromNumericCellRange(
      sheet,
      new CellRangeAddress(firstDataRow, lastDataRow, valueCol, valueCol)
    )

    val data = chart.createData(ChartTypes.BAR, bottomAxis, leftAxis).asInstanceOf[XDDFBarChartData]
    data.setBarDirection(BarDirection.COL)
    val series = data.addSeries(categories, values)
    series.setTitle(seriesTitle, null)
    chart.plot(data)
  }

  private def createPieChart(
    sheet: XSSFSheet,
    title: String,
    categoryCol: Int,
    valueCol: Int,
    firstDataRow: Int,
    lastDataRow: Int,
    anchor: (Int, Int, Int, Int)
  ): Unit = {
    if (lastDataRow < firstDataRow) return

    val drawing = sheet.createDrawingPatriarch().asInstanceOf[XSSFDrawing]
    val chartAnchor = drawing.createAnchor(0, 0, 0, 0, anchor._1, anchor._2, anchor._3, anchor._4)
    val chart = drawing.createChart(chartAnchor)

    chart.setTitleText(title)
    chart.setTitleOverlay(false)

    val categories = XDDFDataSourcesFactory.fromStringCellRange(
      sheet,
      new CellRangeAddress(firstDataRow, lastDataRow, categoryCol, categoryCol)
    )
    val values = XDDFDataSourcesFactory.fromNumericCellRange(
      sheet,
      new CellRangeAddress(firstDataRow, lastDataRow, valueCol, valueCol)
    )

    val data = chart.createData(ChartTypes.PIE, null, null).asInstanceOf[XDDFPieChartData]
    data.addSeries(categories, values)
    chart.plot(data)
  }

  private def writeRows(sheet: org.apache.poi.ss.usermodel.Sheet, headers: Seq[String], rows: Seq[Seq[String]]): Unit = {
    val headerRow = sheet.createRow(0)
    headers.zipWithIndex.foreach { case (header, idx) =>
      headerRow.createCell(idx).setCellValue(header)
    }

    rows.zipWithIndex.foreach { case (values, rowIdx) =>
      val row = sheet.createRow(rowIdx + 1)
      values.zipWithIndex.foreach { case (value, colIdx) =>
        row.createCell(colIdx).setCellValue(value)
      }
    }

    headers.indices.foreach(sheet.autoSizeColumn)
  }

  private def safeTimestamp(raw: String): String = {
    scala.util.Try(LocalDateTime.parse(raw)).map(_.format(dateFmt)).getOrElse(raw.replace(":", "-").replace("T", "_"))
  }
}
