package scala.ecommerce.controllers

import javax.inject.*
import play.api.mvc.*
import play.api.Logging
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.ecommerce.services.OrderService
import scala.ecommerce.services.ExcelOrderReader

@Singleton
class OrderController @Inject() (
  val controllerComponents: ControllerComponents,
  orderService: OrderService,
  excelOrderReader: ExcelOrderReader
)(using ec: ExecutionContext) extends BaseController with Logging:

  private val intervalSeconds = 15

  def createOrder() = Action:
    Future {
      excelOrderReader.readBatch().zipWithIndex.foreach { case ((order, isValid), i) =>
        val index = i + 1
        orderService.produce(order) match
          case Right(orderId) =>
            logger.info(s"Order batch sent. index=$index orderId=$orderId validCandidate=$isValid")
          case Left(error) =>
            logger.error(s"Order batch send failed. index=$index orderId=${order.header.orderId} reason=$error")

        if index < excelOrderReader.totalOrdersToSend then
          Thread.sleep(intervalSeconds * 1000L)
      }
    }

    Accepted(
      s"Batch started: sending ${excelOrderReader.totalOrdersToSend} orders every $intervalSeconds seconds."
    )
