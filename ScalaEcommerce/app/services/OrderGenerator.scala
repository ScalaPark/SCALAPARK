package scala.ecommerce.services

import javax.inject.Singleton
import scala.util.Random
import java.time.LocalDateTime
import java.util.UUID
import scala.ecommerce.models.*

@Singleton
class OrderGenerator:

  private val random = new Random()
  val totalOrdersToSend = 15
  private val validOrderRatio = 0.7

  private val emails = Vector("maria", "juan", "laura", "daniel", "sara", "carlos", "camila")
  private val cities = Vector(
    ("Bogota", "Cundinamarca", 110111),
    ("Medellin", "Antioquia", 50001),
    ("Cali", "Valle", 760001),
    ("Barranquilla", "Atlantico", 80001)
  )
  private val categories = Vector("Ropa", "Tecnologia", "Hogar", "Deportes")

  def generateBatch(): List[(Order, Boolean)] =
    (1 to totalOrdersToSend).toList.map { index =>
      val isValid = random.nextDouble() < validOrderRatio
      (buildRandomOrder(index, isValid), isValid)
    }

  private def buildRandomOrder(index: Int, isValid: Boolean): Order =
    val now = LocalDateTime.now()
    val orderId = s"ORD-${System.currentTimeMillis()}-$index"
    val (city, department, zipCode) = cities(random.nextInt(cities.length))
    val emailUser = emails(random.nextInt(emails.length))
    val itemCount = 1 + random.nextInt(3)

    val baseItems = (1 to itemCount).toList.map { itemIndex =>
      Item(
        productId = 5000 + random.nextInt(999),
        name = s"Producto-$itemIndex",
        price = 10000 + random.nextInt(190000),
        size = 36 + random.nextInt(9),
        quantity = 1 + random.nextInt(4),
        category = categories(random.nextInt(categories.length))
      )
    }

    val validOrder = Order(
      header = Header(
        orderId = orderId,
        timestamp = now,
        sourceApp = "ecommerce-web-client",
        correlationId = UUID.randomUUID()
      ),
      customer = Customer(
        email = s"$emailUser.$index@mail.com",
        docType = Vector(DocType.CE, DocType.CN, DocType.NIT)(random.nextInt(3)),
        docNumber = 100000000 + random.nextInt(899999999),
        phone = 3000000000L + random.nextInt(99999999),
        ipAddress = s"186.155.${10 + random.nextInt(200)}.${1 + random.nextInt(254)}"
      ),
      location = Location(
        market = Market.COLOMBIA,
        city = city,
        department = department,
        zipCode = zipCode,
        address = s"Calle ${1 + random.nextInt(120)} #${1 + random.nextInt(50)}-${1 + random.nextInt(80)}"
      ),
      payment = Payment(
        cardBin = 400000 + random.nextInt(99999),
        cVV = 100 + random.nextInt(899),
        expirationDate = now.plusMonths(6 + random.nextInt(30)),
        currency = Vector(Currency.COP, Currency.EUR, Currency.US)(random.nextInt(3)),
        installments = 1 + random.nextInt(12)
      ),
      items = baseItems
    )

    if isValid then validOrder
    else buildInvalidOrderVariant(validOrder, now)

  private def buildInvalidOrderVariant(order: Order, now: LocalDateTime): Order =
    random.nextInt(4) match
      case 0 => order.copy(customer = order.customer.copy(email = "correo-invalido"))
      case 1 => order.copy(items = List.empty)
      case 2 =>
        val updatedItems = order.items match
          case head :: tail => head.copy(quantity = 0) :: tail
          case Nil => List(Item(5001, "Producto", 50000, 40, 0, "Ropa"))
        order.copy(items = updatedItems)
      case _ => order.copy(payment = order.payment.copy(expirationDate = now.minusDays(1)))
