// @GENERATOR:play-routes-compiler
// @SOURCE:conf/routes

package router

import play.core.routing._
import play.core.routing.HandlerInvokerFactory._

import play.api.mvc._

import _root_.controllers.Assets.Asset

class Routes(
  override val errorHandler: play.api.http.HttpErrorHandler, 
  // @LINE:1
  HealthController_0: scalapark.api.controllers.HealthController,
  // @LINE:3
  DashboardController_1: scalapark.api.controllers.DashboardController,
  val prefix: String
) extends GeneratedRouter {

  @javax.inject.Inject()
  def this(errorHandler: play.api.http.HttpErrorHandler,
    // @LINE:1
    HealthController_0: scalapark.api.controllers.HealthController,
    // @LINE:3
    DashboardController_1: scalapark.api.controllers.DashboardController
  ) = this(errorHandler, HealthController_0, DashboardController_1, "/")

  def withPrefix(addPrefix: String): Routes = {
    val prefix = play.api.routing.Router.concatPrefix(addPrefix, this.prefix)
    router.RoutesPrefix.setPrefix(prefix)
    new Routes(errorHandler, HealthController_0, DashboardController_1, prefix)
  }

  private val defaultPrefix: String = {
    if (this.prefix.endsWith("/")) "" else "/"
  }

  def documentation = List(
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """health""", """scalapark.api.controllers.HealthController.health()"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """api/operator/validation/window""", """scalapark.api.controllers.DashboardController.operatorValidationWindow()"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """api/operator/validation/history""", """scalapark.api.controllers.DashboardController.operatorValidationHistory(limit:Int ?= 20)"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """api/operator/validation/stream""", """scalapark.api.controllers.DashboardController.operatorValidationStream()"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """api/operator/orders/validated""", """scalapark.api.controllers.DashboardController.operatorValidatedOrders(limit:Int ?= 10)"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """api/analyst/daily""", """scalapark.api.controllers.DashboardController.analystDaily()"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """api/analyst/revenue/trend""", """scalapark.api.controllers.DashboardController.analystRevenueTrend(days:Int ?= 30)"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """api/analyst/report/latest""", """scalapark.api.controllers.DashboardController.analystLatestReport()"""),
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """api/analyst/daily/stream""", """scalapark.api.controllers.DashboardController.analystDailyStream()"""),
    Nil
  ).foldLeft(Seq.empty[(String, String, String)]) { (s,e) => e.asInstanceOf[Any] match {
    case r @ (_,_,_) => s :+ r.asInstanceOf[(String, String, String)]
    case l => s ++ l.asInstanceOf[List[(String, String, String)]]
  }}


  // @LINE:1
  private lazy val scalapark_api_controllers_HealthController_health0_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("health")))
  )
  private lazy val scalapark_api_controllers_HealthController_health0_invoker = createInvoker(
    HealthController_0.health(),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "scalapark.api.controllers.HealthController",
      "health",
      Nil,
      "GET",
      this.prefix + """health""",
      """""",
      Seq()
    )
  )

  // @LINE:3
  private lazy val scalapark_api_controllers_DashboardController_operatorValidationWindow1_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("api/operator/validation/window")))
  )
  private lazy val scalapark_api_controllers_DashboardController_operatorValidationWindow1_invoker = createInvoker(
    DashboardController_1.operatorValidationWindow(),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "scalapark.api.controllers.DashboardController",
      "operatorValidationWindow",
      Nil,
      "GET",
      this.prefix + """api/operator/validation/window""",
      """""",
      Seq()
    )
  )

  // @LINE:4
  private lazy val scalapark_api_controllers_DashboardController_operatorValidationHistory2_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("api/operator/validation/history")))
  )
  private lazy val scalapark_api_controllers_DashboardController_operatorValidationHistory2_invoker = createInvoker(
    DashboardController_1.operatorValidationHistory(fakeValue[Int]),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "scalapark.api.controllers.DashboardController",
      "operatorValidationHistory",
      Seq(classOf[Int]),
      "GET",
      this.prefix + """api/operator/validation/history""",
      """""",
      Seq()
    )
  )

  // @LINE:5
  private lazy val scalapark_api_controllers_DashboardController_operatorValidationStream3_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("api/operator/validation/stream")))
  )
  private lazy val scalapark_api_controllers_DashboardController_operatorValidationStream3_invoker = createInvoker(
    DashboardController_1.operatorValidationStream(),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "scalapark.api.controllers.DashboardController",
      "operatorValidationStream",
      Nil,
      "GET",
      this.prefix + """api/operator/validation/stream""",
      """""",
      Seq()
    )
  )

  // @LINE:6
  private lazy val scalapark_api_controllers_DashboardController_operatorValidatedOrders4_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("api/operator/orders/validated")))
  )
  private lazy val scalapark_api_controllers_DashboardController_operatorValidatedOrders4_invoker = createInvoker(
    DashboardController_1.operatorValidatedOrders(fakeValue[Int]),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "scalapark.api.controllers.DashboardController",
      "operatorValidatedOrders",
      Seq(classOf[Int]),
      "GET",
      this.prefix + """api/operator/orders/validated""",
      """""",
      Seq()
    )
  )

  // @LINE:8
  private lazy val scalapark_api_controllers_DashboardController_analystDaily5_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("api/analyst/daily")))
  )
  private lazy val scalapark_api_controllers_DashboardController_analystDaily5_invoker = createInvoker(
    DashboardController_1.analystDaily(),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "scalapark.api.controllers.DashboardController",
      "analystDaily",
      Nil,
      "GET",
      this.prefix + """api/analyst/daily""",
      """""",
      Seq()
    )
  )

  // @LINE:9
  private lazy val scalapark_api_controllers_DashboardController_analystRevenueTrend6_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("api/analyst/revenue/trend")))
  )
  private lazy val scalapark_api_controllers_DashboardController_analystRevenueTrend6_invoker = createInvoker(
    DashboardController_1.analystRevenueTrend(fakeValue[Int]),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "scalapark.api.controllers.DashboardController",
      "analystRevenueTrend",
      Seq(classOf[Int]),
      "GET",
      this.prefix + """api/analyst/revenue/trend""",
      """""",
      Seq()
    )
  )

  // @LINE:10
  private lazy val scalapark_api_controllers_DashboardController_analystLatestReport7_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("api/analyst/report/latest")))
  )
  private lazy val scalapark_api_controllers_DashboardController_analystLatestReport7_invoker = createInvoker(
    DashboardController_1.analystLatestReport(),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "scalapark.api.controllers.DashboardController",
      "analystLatestReport",
      Nil,
      "GET",
      this.prefix + """api/analyst/report/latest""",
      """""",
      Seq()
    )
  )

  // @LINE:11
  private lazy val scalapark_api_controllers_DashboardController_analystDailyStream8_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("api/analyst/daily/stream")))
  )
  private lazy val scalapark_api_controllers_DashboardController_analystDailyStream8_invoker = createInvoker(
    DashboardController_1.analystDailyStream(),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "router",
      "scalapark.api.controllers.DashboardController",
      "analystDailyStream",
      Nil,
      "GET",
      this.prefix + """api/analyst/daily/stream""",
      """""",
      Seq()
    )
  )


  def routes: PartialFunction[RequestHeader, Handler] = {
  
    // @LINE:1
    case scalapark_api_controllers_HealthController_health0_route(params@_) =>
      call { 
        scalapark_api_controllers_HealthController_health0_invoker.call(HealthController_0.health())
      }
  
    // @LINE:3
    case scalapark_api_controllers_DashboardController_operatorValidationWindow1_route(params@_) =>
      call { 
        scalapark_api_controllers_DashboardController_operatorValidationWindow1_invoker.call(DashboardController_1.operatorValidationWindow())
      }
  
    // @LINE:4
    case scalapark_api_controllers_DashboardController_operatorValidationHistory2_route(params@_) =>
      call(params.fromQuery[Int]("limit", Some(20))) { (limit) =>
        scalapark_api_controllers_DashboardController_operatorValidationHistory2_invoker.call(DashboardController_1.operatorValidationHistory(limit))
      }
  
    // @LINE:5
    case scalapark_api_controllers_DashboardController_operatorValidationStream3_route(params@_) =>
      call { 
        scalapark_api_controllers_DashboardController_operatorValidationStream3_invoker.call(DashboardController_1.operatorValidationStream())
      }
  
    // @LINE:6
    case scalapark_api_controllers_DashboardController_operatorValidatedOrders4_route(params@_) =>
      call(params.fromQuery[Int]("limit", Some(10))) { (limit) =>
        scalapark_api_controllers_DashboardController_operatorValidatedOrders4_invoker.call(DashboardController_1.operatorValidatedOrders(limit))
      }
  
    // @LINE:8
    case scalapark_api_controllers_DashboardController_analystDaily5_route(params@_) =>
      call { 
        scalapark_api_controllers_DashboardController_analystDaily5_invoker.call(DashboardController_1.analystDaily())
      }
  
    // @LINE:9
    case scalapark_api_controllers_DashboardController_analystRevenueTrend6_route(params@_) =>
      call(params.fromQuery[Int]("days", Some(30))) { (days) =>
        scalapark_api_controllers_DashboardController_analystRevenueTrend6_invoker.call(DashboardController_1.analystRevenueTrend(days))
      }
  
    // @LINE:10
    case scalapark_api_controllers_DashboardController_analystLatestReport7_route(params@_) =>
      call { 
        scalapark_api_controllers_DashboardController_analystLatestReport7_invoker.call(DashboardController_1.analystLatestReport())
      }
  
    // @LINE:11
    case scalapark_api_controllers_DashboardController_analystDailyStream8_route(params@_) =>
      call { 
        scalapark_api_controllers_DashboardController_analystDailyStream8_invoker.call(DashboardController_1.analystDailyStream())
      }
  }
}
