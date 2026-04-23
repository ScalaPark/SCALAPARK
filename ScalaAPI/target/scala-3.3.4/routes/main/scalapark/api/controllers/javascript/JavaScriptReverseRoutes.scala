// @GENERATOR:play-routes-compiler
// @SOURCE:conf/routes

import play.api.routing.JavaScriptReverseRoute


import _root_.controllers.Assets.Asset

// @LINE:1
package scalapark.api.controllers.javascript {

  // @LINE:1
  class ReverseHealthController(_prefix: => String) {

    def _defaultPrefix: String = {
      if (_prefix.endsWith("/")) "" else "/"
    }

  
    // @LINE:1
    def health: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "scalapark.api.controllers.HealthController.health",
      """
        function() {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "health"})
        }
      """
    )
  
  }

  // @LINE:3
  class ReverseDashboardController(_prefix: => String) {

    def _defaultPrefix: String = {
      if (_prefix.endsWith("/")) "" else "/"
    }

  
    // @LINE:5
    def operatorValidationStream: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "scalapark.api.controllers.DashboardController.operatorValidationStream",
      """
        function() {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "api/operator/validation/stream"})
        }
      """
    )
  
    // @LINE:10
    def analystLatestReport: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "scalapark.api.controllers.DashboardController.analystLatestReport",
      """
        function() {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "api/analyst/report/latest"})
        }
      """
    )
  
    // @LINE:11
    def analystDailyStream: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "scalapark.api.controllers.DashboardController.analystDailyStream",
      """
        function() {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "api/analyst/daily/stream"})
        }
      """
    )
  
    // @LINE:3
    def operatorValidationWindow: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "scalapark.api.controllers.DashboardController.operatorValidationWindow",
      """
        function() {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "api/operator/validation/window"})
        }
      """
    )
  
    // @LINE:4
    def operatorValidationHistory: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "scalapark.api.controllers.DashboardController.operatorValidationHistory",
      """
        function(limit0) {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "api/operator/validation/history" + _qS([(limit0 == null ? null : (""" + implicitly[play.api.mvc.QueryStringBindable[Int]].javascriptUnbind + """)("limit", limit0))])})
        }
      """
    )
  
    // @LINE:6
    def operatorValidatedOrders: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "scalapark.api.controllers.DashboardController.operatorValidatedOrders",
      """
        function(limit0) {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "api/operator/orders/validated" + _qS([(limit0 == null ? null : (""" + implicitly[play.api.mvc.QueryStringBindable[Int]].javascriptUnbind + """)("limit", limit0))])})
        }
      """
    )
  
    // @LINE:9
    def analystRevenueTrend: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "scalapark.api.controllers.DashboardController.analystRevenueTrend",
      """
        function(days0) {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "api/analyst/revenue/trend" + _qS([(days0 == null ? null : (""" + implicitly[play.api.mvc.QueryStringBindable[Int]].javascriptUnbind + """)("days", days0))])})
        }
      """
    )
  
    // @LINE:8
    def analystDaily: JavaScriptReverseRoute = JavaScriptReverseRoute(
      "scalapark.api.controllers.DashboardController.analystDaily",
      """
        function() {
          return _wA({method:"GET", url:"""" + _prefix + { _defaultPrefix } + """" + "api/analyst/daily"})
        }
      """
    )
  
  }


}
