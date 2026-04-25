package scala.ecommerce.services

import javax.inject.{Inject, Singleton}
import play.api.{Environment, Logging}
import org.apache.poi.ss.usermodel.{CellType, Row, WorkbookFactory}
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import scala.util.Try
import scala.ecommerce.models.*

@Singleton
class ExcelOrderReader @Inject() (environment: Environment) extends Logging:

  val totalOrdersToSend: Int = 50
  private val batchSize      = totalOrdersToSend
  private val totalRows      = 10_000
  private val tsFormatter    = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")

  // Workbook cargado una sola vez al arrancar — @Singleton garantiza una sola instancia
  private val workbook: XSSFWorkbook =
    val file = environment.getFile("dataset/scalapark_orders_test_dataset.xlsx")
    WorkbookFactory.create(file).asInstanceOf[XSSFWorkbook]

  private val sheet  = workbook.getSheetAt(0)
  private val offset = new AtomicInteger(0)

  def readBatch(): List[(Order, Boolean)] =
    val start = offset.getAndUpdate(cur => (cur + batchSize) % totalRows)
    (0 until batchSize).toList.flatMap { i =>
      val sheetRow = ((start + i) % totalRows) + 1   // +1 para saltar la fila de encabezado
      Option(sheet.getRow(sheetRow)) match
        case None =>
          logger.warn(s"Fila nula en índice $sheetRow — omitida")
          None
        case Some(row) =>
          Try(parseRow(row)).fold(
            err  => { logger.error(s"Error parseando fila $sheetRow: ${err.getMessage}"); None },
            some => Some(some)
          )
    }

  private def parseRow(row: Row): (Order, Boolean) =
    val header = Header(
      orderId       = str(row, 0),
      timestamp     = LocalDateTime.parse(str(row, 1), tsFormatter),
      sourceApp     = str(row, 2),
      correlationId = UUID.fromString(str(row, 3))
    )
    val customer = Customer(
      email     = str(row, 4),
      docType   = DocType.valueOf(str(row, 5)),
      docNumber = str(row, 6).toDouble.toInt,
      phone     = str(row, 7).toDouble.toLong,
      ipAddress = str(row, 8)
    )
    val location = Location(
      market     = Market.valueOf(str(row, 9)),
      city       = str(row, 10),
      department = str(row, 11),
      zipCode    = str(row, 12).toDouble.toInt,
      address    = str(row, 13)
    )
    val payment = Payment(
      cardBin        = str(row, 14).toDouble.toInt,
      cVV            = str(row, 15).toDouble.toInt,
      expirationDate = LocalDateTime.parse(str(row, 16), tsFormatter),
      currency       = Currency.valueOf(str(row, 17)),
      installments   = str(row, 18).toDouble.toInt
    )
    val items   = List(optItem(row, 19), optItem(row, 25), optItem(row, 31)).flatten
    val isValid = str(row, 38) == "VALID"
    val order   = Order(
      header      = header,
      customer    = customer,
      location    = location,
      payment     = payment,
      items       = items,
      totalAmount = str(row, 37).toDouble.toInt
    )
    (order, isValid)

  // Ítem opcional: columnas 25–30 (item2) o 31–36 (item3). Vacío si productId está en blanco.
  private def optItem(row: Row, startCol: Int): Option[Item] =
    val pid = str(row, startCol)
    if pid.isEmpty then None
    else Some(Item(
      productId = pid.toDouble.toInt,
      name      = str(row, startCol + 1),
      price     = str(row, startCol + 2).toDouble.toInt,
      size      = str(row, startCol + 3).toDouble.toInt,
      quantity  = str(row, startCol + 4).toDouble.toInt,
      category  = str(row, startCol + 5)
    ))

  // Normaliza cualquier celda a String. Celdas NUMERIC usan toLong para evitar notación
  // científica en valores enteros grandes como números de teléfono (ej: 3001234567).
  private def str(row: Row, col: Int): String =
    val cell = row.getCell(col)
    if cell == null then ""
    else cell.getCellType match
      case CellType.STRING  => cell.getStringCellValue.trim
      case CellType.NUMERIC =>
        val d = cell.getNumericCellValue
        if d == math.floor(d) && !java.lang.Double.isInfinite(d) then d.toLong.toString
        else d.toString
      case CellType.BLANK   => ""
      case CellType.FORMULA =>
        cell.getCachedFormulaResultType match
          case CellType.STRING  => cell.getStringCellValue.trim
          case CellType.NUMERIC =>
            val d = cell.getNumericCellValue
            if d == math.floor(d) && !java.lang.Double.isInfinite(d) then d.toLong.toString
            else d.toString
          case _ => ""
      case _ => ""
