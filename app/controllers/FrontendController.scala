package controllers

import javax.inject.Inject
import controllers.auth.AuthActionFactory
import play.api.{Configuration, Logging}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import models.FeatureToggleModel._
import javax.inject._
import play.api.Configuration
import play.api.http.HttpErrorHandler
import play.api.libs.json.Json
import play.api.mvc._

class FrontendController @Inject()(cc: MessagesControllerComponents,
                                   assets: Assets,
                                   errorHandler: HttpErrorHandler,
                                   authActionFactory: AuthActionFactory)(implicit executionContext: ExecutionContext)
  extends MessagesAbstractController(cc) with Logging {

  def index(): Action[AnyContent] = authActionFactory.getAuthenticatedAction(Action).async { request =>
    assets.at("index.html")(request)
  }

  def assetOrDefault(resource: String): Action[AnyContent] = authActionFactory.getAuthenticatedAction(Action).async { request =>
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
