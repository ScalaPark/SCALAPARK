package scala.ecommerce.services

import javax.inject.Singleton
import scala.util.Random
import java.time.{LocalDateTime, ZoneId, ZonedDateTime}
import java.util.UUID
import scala.ecommerce.models.*

@Singleton
class OrderGenerator:

  private val random = new Random()
  val totalOrdersToSend = 50
  private val validOrderRatio = 0.6
  private val colombiaZone = ZoneId.of("America/Bogota")

  private val emails = Vector(
    "sofia", "valeria", "isabella", "camila", "daniela", "mariana", "natalia",
    "alejandra", "paula", "andrea", "laura", "sara", "maria", "carolina", "juliana",
    "alejandro", "santiago", "sebastian", "nicolas", "juan", "carlos", "daniel",
    "andres", "david", "felipe", "jorge", "miguel", "luis", "gabriel", "mateo",
    "samuel", "simon", "ivan", "oscar", "ricardo", "sergio", "fabian", "julian",
    "cristian", "diego", "manuela", "valentina", "luisa", "paola", "monica",
    "tatiana", "jennifer", "yesenia", "lina", "viviana"
  )

  private val cities = Vector(
    ("Bogota", "Cundinamarca", 110111),
    ("Medellin", "Antioquia", 50001),
    ("Cali", "Valle del Cauca", 760001),
    ("Barranquilla", "Atlantico", 80001),
    ("Cartagena", "Bolivar", 130001),
    ("Cucuta", "Norte de Santander", 540001),
    ("Bucaramanga", "Santander", 680001),
    ("Pereira", "Risaralda", 660001),
    ("Manizales", "Caldas", 170001),
    ("Ibague", "Tolima", 730001),
    ("Santa Marta", "Magdalena", 470001),
    ("Villavicencio", "Meta", 500001),
    ("Armenia", "Quindio", 630001),
    ("Valledupar", "Cesar", 200001),
    ("Monteria", "Cordoba", 230001),
    ("Neiva", "Huila", 410001),
    ("Pasto", "Narino", 520001),
    ("Palmira", "Valle del Cauca", 763001),
    ("Buenaventura", "Valle del Cauca", 764001),
    ("Bello", "Antioquia", 50002),
    ("Soledad", "Atlantico", 80002),
    ("Itagui", "Antioquia", 50003),
    ("Soacha", "Cundinamarca", 110002),
    ("Dosquebradas", "Risaralda", 661001),
    ("Floridablanca", "Santander", 681001),
    ("Sincelejo", "Sucre", 700001),
    ("Popayan", "Cauca", 190001),
    ("Tunja", "Boyaca", 150001),
    ("Envigado", "Antioquia", 50004),
    ("Rionegro", "Antioquia", 50005),
    ("Barrancabermeja", "Santander", 682001),
    ("Girardot", "Cundinamarca", 111001),
    ("Tulua", "Valle del Cauca", 765001),
    ("Duitama", "Boyaca", 151001),
    ("Sogamoso", "Boyaca", 152001),
    ("Zipaquira", "Cundinamarca", 112001),
    ("Facatativa", "Cundinamarca", 113001),
    ("Espinal", "Tolima", 731001),
    ("Florencia", "Caqueta", 180001),
    ("Mocoa", "Putumayo", 860001),
    ("Leticia", "Amazonas", 910001),
    ("Mitú", "Vaupés", 970001),
    ("Puerto Inirida", "Guainia", 940001),
    ("San Jose del Guaviare", "Guaviare", 950001),
    ("Arauca", "Arauca", 810001),
    ("Yopal", "Casanare", 850001),
    ("Quibdo", "Choco", 270001),
    ("Turbo", "Antioquia", 50006),
    ("Maicao", "La Guajira", 440001),
    ("Riohacha", "La Guajira", 441001)
  )

  private val categories = Vector(
    "Ropa", "Tecnologia", "Hogar", "Deportes", "Belleza",
    "Juguetes", "Libros", "Mascotas", "Automotriz", "Alimentos",
    "Salud", "Muebles", "Electrodomesticos", "Joyeria", "Calzado",
    "Accesorios", "Papeleria", "Herramientas", "Jardineria", "Viajes",
    "Musica", "Cine", "Videojuegos", "Arte", "Bebes"
  )

  private val productsByCategory: Map[String, Vector[String]] = Map(
    "Ropa"             -> Vector("Camiseta polo", "Jean slim", "Chaqueta impermeable", "Blusa floral", "Vestido casual", "Pantalon cargo"),
    "Tecnologia"       -> Vector("Audifonos Bluetooth", "Cargador USB-C", "Mouse inalambrico", "Teclado mecanico", "Lampara LED escritorio", "Hub USB"),
    "Hogar"            -> Vector("Juego de sabanas", "Cojin decorativo", "Organizador de closet", "Porta velas", "Cuberteria acero", "Alfombra sala"),
    "Deportes"         -> Vector("Tenis running", "Maletín gym", "Botella deportiva", "Guantes de boxeo", "Banda elastica", "Cuerda para saltar"),
    "Belleza"          -> Vector("Serum vitamina C", "Crema hidratante", "Paleta de sombras", "Labial mate", "Mascara de pestanas", "Perfume floral"),
    "Juguetes"         -> Vector("Set de Lego", "Muneca interactiva", "Carro a control remoto", "Rompecabezas 500 piezas", "Juego de mesa familiar", "Pelota de futbol"),
    "Libros"           -> Vector("Habitos atomicos", "El principito", "Sapiens", "Cien anos de soledad", "El alquimista", "Piense y hagase rico"),
    "Mascotas"         -> Vector("Alimento premium perro", "Arena para gatos", "Correa retractil", "Juguete mordedor", "Cama para mascotas", "Shampoo antipulgas"),
    "Automotriz"       -> Vector("Aspiradora para carro", "Funda para asientos", "Cargador para carro", "Aceite motor 5W-30", "Desengrasante multiusos", "Tapetes para carro"),
    "Alimentos"        -> Vector("Cafe organico 500g", "Miel de abeja 350g", "Proteina en polvo", "Granola artesanal", "Aceite de oliva extra virgen", "Avena integral"),
    "Salud"            -> Vector("Vitamina D3", "Termometro digital", "Pulsioximetro", "Tensiometro digital", "Mascarilla KN95", "Gel antibacterial"),
    "Muebles"          -> Vector("Escritorio plegable", "Silla ergonomica", "Estante de madera", "Mesa de centro", "Sofa de dos puestos", "Cajonera organizadora"),
    "Electrodomesticos"-> Vector("Licuadora 600W", "Cafetera de goteo", "Tostadora 2 ranuras", "Freidora de aire", "Aspiradora de mano", "Plancha de vapor"),
    "Joyeria"          -> Vector("Collar de plata", "Aretes de oro", "Pulsera de cuero", "Anillo de acero", "Dije de plata", "Cadena unisex"),
    "Calzado"          -> Vector("Botas de cuero", "Sandalias de playa", "Zapatos formales", "Pantuflas de peluche", "Zapatillas casual", "Alpargatas artesanales"),
    "Accesorios"       -> Vector("Billetera de cuero", "Cinturon tejido", "Gorra visera", "Bufanda de lana", "Gafas de sol", "Mochila urbana"),
    "Papeleria"        -> Vector("Cuaderno cuadriculado", "Set de marcadores", "Agenda 2026", "Lapices de colores 24u", "Resaltadores pastel", "Post-it colores"),
    "Herramientas"     -> Vector("Taladro percutor", "Juego de destornilladores", "Cinta metrica 5m", "Nivel laser", "Llave inglesa", "Cortador multiusos"),
    "Jardineria"       -> Vector("Kit de jardineria", "Tierra negra 10kg", "Maceta ceramica", "Regadera metalica", "Abono organico", "Semillas de hierbas"),
    "Viajes"           -> Vector("Maleta de cabina", "Almohada de viaje", "Organizador de maleta", "Candado TSA", "Porta documentos", "Adaptador universal"),
    "Musica"           -> Vector("Guitarra acustica", "Ukulele soprano", "Flauta traversa", "Cajón peruano", "Harmónica diatónica", "Cuerdas de guitarra"),
    "Cine"             -> Vector("Proyector portatil", "Pantalla de proyeccion", "Bocina Bluetooth", "Palomitas gourmet", "Lentes 3D", "Control universal"),
    "Videojuegos"      -> Vector("Control inalambrico", "Tarjeta de regalo PSN", "Memoria USB 64GB", "Auricular gamer", "Silla gamer", "Alfombrilla XXL"),
    "Arte"             -> Vector("Set de acuarelas", "Lienzo 40x50cm", "Pinceles profesionales", "Arcilla para modelar", "Lapices de grafito", "Marcadores alcohol"),
    "Bebes"            -> Vector("Coche compacto", "Bañera plegable", "Monitor de bebe", "Portabebe ergonomico", "Cuna viajera", "Set de biberones")
  )

  def generateBatch(): List[(Order, Boolean)] =
    (1 to totalOrdersToSend).toList.map { index =>
      val isValid = random.nextDouble() < validOrderRatio
      (buildRandomOrder(index, isValid), isValid)
    }

  private def buildRandomOrder(index: Int, isValid: Boolean): Order =
    val now = ZonedDateTime.now(colombiaZone).toLocalDateTime
    val orderId = s"ORD-${System.currentTimeMillis()}-$index"
    val (city, department, zipCode) = cities(random.nextInt(cities.length))
    val emailUser = emails(random.nextInt(emails.length))
    val itemCount = 1 + random.nextInt(3)

    val baseItems = (1 to itemCount).toList.map { _ =>
      val category = categories(random.nextInt(categories.length))
      val products = productsByCategory(category)
      val productName = products(random.nextInt(products.length))
      Item(
        productId = 5000 + random.nextInt(999),
        name = productName,
        price = 10000 + random.nextInt(190000),
        size = 36 + random.nextInt(9),
        quantity = 1 + random.nextInt(4),
        category = category
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
        installments = 1 + random.nextInt(100)
      ),
      items = baseItems,
      totalAmount = baseItems.map(i => i.price * i.quantity).sum
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
          case Nil => Item(5001, "Camiseta polo", 50000, 40, 0, "Ropa")  :: Nil
        order.copy(items = updatedItems)
      case _ => order.copy(payment = order.payment.copy(expirationDate = now.minusDays(1)))
