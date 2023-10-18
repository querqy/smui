package controllers

import org.pac4j.core.profile.UserProfile
import org.pac4j.play.scala.{Security, SecurityComponents}
import play.api.Logging
import play.api.http.HttpErrorHandler
import play.api.mvc._

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class FrontendController @Inject()(val controllerComponents: SecurityComponents,
                                   assets: Assets,
                                   errorHandler: HttpErrorHandler)
                                  (implicit executionContext: ExecutionContext)
  extends Security [UserProfile] with play.api.i18n.I18nSupport with Logging {

  def index(): Action[AnyContent] = Action.async { request =>
    assets.at("index.html")(request)
  }

  def assetOrDefault(resource: String): Action[AnyContent] = Action.async { request =>
    if (resource.startsWith("api")) {
      errorHandler.onClientError(request, NOT_FOUND, "Not found")
    } else {
      if (resource.contains(".")) {
        assets.at(resource)(request)
      } else {
        assets.at("index.html")(request)
      }
    }
  }
}
