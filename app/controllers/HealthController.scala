package controllers

import javax.inject.Inject
import play.api.libs.json.Json
import play.api.mvc._


class HealthController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  val health = Action {
    Ok(Json.parse(models.buildInfo.BuildInfo.toJson))
  }

}
