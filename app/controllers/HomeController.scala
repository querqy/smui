package controllers

import javax.inject.Inject
import controllers.auth.AuthActionFactory
import play.api.{Configuration, Logging}
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import models.FeatureToggleModel._

class HomeController @Inject()(cc: MessagesControllerComponents,
                               appConfig: Configuration,
                               featureToggleService: FeatureToggleService,
                               authActionFactory: AuthActionFactory)(implicit executionContext: ExecutionContext)
  extends MessagesAbstractController(cc) with Logging {

  def index(urlPath: String) = authActionFactory.getAuthenticatedAction(Action).async {
    Future {
      logger.debug("In HomeController :: index")
      Ok(
        views.html.home(
          featureToggleService.getJsFrontendToogleList
        )
      )
    }(executionContext) // TODO eval withSecurity ... because of play.filters.headers.contentSecurityPolicy (and resolve general setup in application.conf)
  }

  // TODO refactor authorizationTestControllerAction into a proper controller behaviour test
  /*
  def authorizationTestControllerAction = Action.async {
    Future {
      Unauthorized(
        "{ \"action\":\"redirect\"," +
          "\"params\":\"https://www.example.com/loginService/?urlCallback={{CURRENT_SMUI_URL}}\"" +
          " }")
    }
  }
  */

}
