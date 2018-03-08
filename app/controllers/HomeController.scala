package controllers

import javax.inject.Inject

import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

class HomeController @Inject()(cc: MessagesControllerComponents)(implicit executionContext: ExecutionContext)
  extends MessagesAbstractController(cc) {

  private val logger = play.api.Logger

  def index(urlPath: String) = Action.async {
    Future {
      logger.debug("index-Controller called delivering views.html.home()")
      Ok( views.html.home() )
    }(executionContext) // TODO withSecurity ... because of play.filters.headers.contentSecurityPolicy (and resolve general setup in application.conf)
  }

}
