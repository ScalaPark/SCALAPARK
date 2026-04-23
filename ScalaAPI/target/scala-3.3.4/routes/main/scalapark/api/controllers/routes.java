// @GENERATOR:play-routes-compiler
// @SOURCE:conf/routes

package scalapark.api.controllers;

import router.RoutesPrefix;

public class routes {
  
  public static final scalapark.api.controllers.ReverseHealthController HealthController = new scalapark.api.controllers.ReverseHealthController(RoutesPrefix.byNamePrefix());
  public static final scalapark.api.controllers.ReverseDashboardController DashboardController = new scalapark.api.controllers.ReverseDashboardController(RoutesPrefix.byNamePrefix());

  public static class javascript {
    
    public static final scalapark.api.controllers.javascript.ReverseHealthController HealthController = new scalapark.api.controllers.javascript.ReverseHealthController(RoutesPrefix.byNamePrefix());
    public static final scalapark.api.controllers.javascript.ReverseDashboardController DashboardController = new scalapark.api.controllers.javascript.ReverseDashboardController(RoutesPrefix.byNamePrefix());
  }

}
