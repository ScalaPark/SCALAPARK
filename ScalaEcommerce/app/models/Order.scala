package scala.ecommerce.models

import java.time.LocalDateTime
import java.util.UUID

case class Header(orderId: String, timestamp: LocalDateTime, sourceApp: String, correlationId: UUID)

case class Customer(email: String, docType: DocType, docNumber: Int, phone: Long, ipAddress: String)

case class Location(market: Market, city: String, department: String, zipCode: Int, address: String)

case class Payment(cardBin: Int, cVV: Int, expirationDate: LocalDateTime, currency: Currency, installments: Int)

case class Item(productId: Int, name: String, price: Int, size: Int, quantity: Int, category: String)

case class Order(header: Header, customer: Customer, location: Location, payment: Payment, items: List[Item])

enum DocType:
  case CE, CN, NIT

enum Currency:
  case COP, EUR, US

enum Market:
  case COLOMBIA, USA
