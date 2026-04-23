package scalapark.api.controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{BaseController, ControllerComponents}

@Singleton
class HealthController @Inject() (
  val controllerComponents: ControllerComponents
) extends BaseController:

  def health() = Action:
    Ok(Json.obj("status" -> "ok"))
