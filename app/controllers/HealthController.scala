package controllers

import play.api.libs.json.Json
import play.api.mvc._



class HealthController extends ControllerHelpers {

  val health = Action {
    Ok(Json.parse(models.buildInfo.BuildInfo.toJson))
  }

}
