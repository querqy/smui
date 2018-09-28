package controllers

import javax.inject.Inject

import play.api.Configuration
import play.api.mvc._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}

import models.FeatureToggleModel._

class HomeController @Inject()(cc: MessagesControllerComponents,
                               appConfig: Configuration,
                               featureToggleService: FeatureToggleService)(implicit executionContext: ExecutionContext)
  extends MessagesAbstractController(cc) {

  private val logger = play.api.Logger

  def index(urlPath: String) = Action.async {
    Future {
      logger.debug("In HomeController :: index");
      Ok(
        views.html.home(
          featureToggleService.getJsFrontendToogleList
        )
      )
    }(executionContext) // TODO eval withSecurity ... because of play.filters.headers.contentSecurityPolicy (and resolve general setup in application.conf)
  }

}
