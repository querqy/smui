package controllers

import play.api.libs.json.Json
import play.api.mvc._


class HealthController(cc: ControllerComponents)
  extends AbstractController(cc) {

  val health = Action {
    Ok(Json.parse(models.buildInfo.BuildInfo.toJson))
  }

}
