// @GENERATOR:play-routes-compiler
// @SOURCE:conf/routes

import play.api.mvc.Call


import _root_.controllers.Assets.Asset

// @LINE:1
package scalapark.api.controllers {

  // @LINE:1
  class ReverseHealthController(_prefix: => String) {
    def _defaultPrefix: String = {
      if (_prefix.endsWith("/")) "" else "/"
    }

  
    // @LINE:1
    def health(): Call = {
      
      Call("GET", _prefix + { _defaultPrefix } + "health")
    }
  
  }

  // @LINE:3
  class ReverseDashboardController(_prefix: => String) {
    def _defaultPrefix: String = {
      if (_prefix.endsWith("/")) "" else "/"
    }

  
    // @LINE:5
    def operatorValidationStream(): Call = {
      
      Call("GET", _prefix + { _defaultPrefix } + "api/operator/validation/stream")
    }
  
    // @LINE:10
    def analystLatestReport(): Call = {
      
      Call("GET", _prefix + { _defaultPrefix } + "api/analyst/report/latest")
    }
  
    // @LINE:11
    def analystDailyStream(): Call = {
      
      Call("GET", _prefix + { _defaultPrefix } + "api/analyst/daily/stream")
    }
  
    // @LINE:3
    def operatorValidationWindow(): Call = {
      
      Call("GET", _prefix + { _defaultPrefix } + "api/operator/validation/window")
    }
  
    // @LINE:4
    def operatorValidationHistory(limit:Int = 20): Call = {
      
      Call("GET", _prefix + { _defaultPrefix } + "api/operator/validation/history" + play.core.routing.queryString(List(if(limit == 20) None else Some(implicitly[play.api.mvc.QueryStringBindable[Int]].unbind("limit", limit)))))
    }
  
    // @LINE:6
    def operatorValidatedOrders(limit:Int = 10): Call = {
      
      Call("GET", _prefix + { _defaultPrefix } + "api/operator/orders/validated" + play.core.routing.queryString(List(if(limit == 10) None else Some(implicitly[play.api.mvc.QueryStringBindable[Int]].unbind("limit", limit)))))
    }
  
    // @LINE:9
    def analystRevenueTrend(days:Int = 30): Call = {
      
      Call("GET", _prefix + { _defaultPrefix } + "api/analyst/revenue/trend" + play.core.routing.queryString(List(if(days == 30) None else Some(implicitly[play.api.mvc.QueryStringBindable[Int]].unbind("days", days)))))
    }
  
    // @LINE:8
    def analystDaily(): Call = {
      
      Call("GET", _prefix + { _defaultPrefix } + "api/analyst/daily")
    }
  
  }


}
