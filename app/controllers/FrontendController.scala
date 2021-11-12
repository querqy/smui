package controllers

import controllers.auth.AuthActionFactory
import models.{SessionDAO, User, UserDAO}
import play.api.Logging
import play.api.http.HttpErrorHandler
import play.api.mvc._

import java.time.{LocalDateTime, ZoneOffset}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FrontendController @Inject()(cc: MessagesControllerComponents,
                                   assets: Assets,
                                   errorHandler: HttpErrorHandler,
                                   authActionFactory: AuthActionFactory)(implicit executionContext: ExecutionContext)
  extends MessagesAbstractController(cc) with Logging {

  def index(): Action[AnyContent] = authActionFactory.getAuthenticatedAction(Action).async { request =>
    assets.at("index.html")(request)
  }


  def public() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.public())
  }

  def login_or_register() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.login_or_register())
  }


  // private page controller method
  //def priv() = Action { implicit request: Request[AnyContent] =>
   // withUser(user => Ok(views.html.priv(user)))
  //}
  def priv() = Action { implicit request: Request[AnyContent] =>

    val userOpt = extractUser(request)

    userOpt
      .map(user => Ok(views.html.priv(user)))
      .getOrElse(Unauthorized(views.html.defaultpages.unauthorized()))
  }

  def priv2(): Action[AnyContent] = authActionFactory.getAuthenticatedAction(Action).async { request =>
    Future(Ok(views.html.priv2()))
    //assets.at("index.html")(request)
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

  def register(username: String, password: String) = Action { implicit request: Request[AnyContent] =>
    UserDAO.addUser(username, password)
      .map(_ => Ok(views.html.index()))
      .getOrElse(Conflict(views.html.defaultpages.unauthorized()))
  }

  def login(username: String, pass: String) = Action { implicit request: Request[AnyContent] =>
    if (isValidLogin(username, pass)) {
      val token = SessionDAO.generateToken(username)

      Redirect(routes.FrontendController.index()).withSession(request.session + ("sessionToken" -> token))
    } else {
      // we should redirect to login page
      Unauthorized(views.html.defaultpages.unauthorized()).withNewSession
    }
  }

  def logout() = Action { implicit request: Request[AnyContent] =>
    Redirect(routes.FrontendController.index()).withNewSession
  }

  //def priv() = Action { implicit request: Request[AnyContent] =>
  //  withUser(user => Ok(views.html.priv(user)))
 // }

  def privPlay() = withPlayUser { user =>
    Ok(views.html.priv(user))
  }

  //def privAction() = userAction { user: UserRequest[AnyContent] =>
  //  Ok(views.html.priv(user.user.get))
  //}

  private def isValidLogin(username: String, password: String): Boolean = {
    UserDAO.getUser(username).exists(_.password == password)
  }

  private def withPlayUser[T](block: User => Result): EssentialAction = {
    Security.WithAuthentication(extractUser)(user => Action(block(user)))
  }

  private def withUser[T](block: User => Result)(implicit request: Request[AnyContent]): Result = {
    val user = extractUser(request)

    user
      .map(block)
      .getOrElse(Unauthorized(views.html.defaultpages.unauthorized())) // 401, but 404 could be better from a security point of view
  }

  private def extractUser(req: RequestHeader): Option[User] = {

    val sessionTokenOpt = req.session.get("sessionToken")

    sessionTokenOpt
      .flatMap(token => SessionDAO.getSession(token))
      .filter(_.expiration.isAfter(LocalDateTime.now(ZoneOffset.UTC)))
      .map(_.username)
      .flatMap(UserDAO.getUser)
  }
}
