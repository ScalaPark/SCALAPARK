package app.models

case class Header(orderId: String, timestamp: String, sourceApp: String, correlationId: String)

case class Customer(email: String, docType: String, docNumber: Int, phone: Long, ipAddress: String)

case class Location(market: String, city: String, department: String, zipCode: Int, address: String)

case class Payment(cardBin: Int, cVV: Int, expirationDate: String, currency: String, installments: Int)

case class Item(productId: Int, name: String, price: Int, size: Int, quantity: Int, category: String)

case class OrderRecord(header: Header, customer: Customer, location: Location, payment: Payment, items: Seq[Item], totalAmount: Int)
