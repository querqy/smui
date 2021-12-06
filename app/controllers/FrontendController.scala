package controllers

import controllers.LoginForm.UserData
import controllers.auth.AuthActionFactory
import models.FeatureToggleModel.FeatureToggleService
import models.{FeatureToggleModel, SessionDAO, SolrIndexId, User, UserDAO}
import play.api.Logging
import play.api.data.Form
import play.api.http.HttpErrorHandler
import play.api.libs.json.{Format, JsValue, Json}
import play.api.mvc._

import java.time.{LocalDateTime, ZoneOffset}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FrontendController @Inject()(cc: MessagesControllerComponents,
                                   assets: Assets,
                                   featureToggleService: FeatureToggleService,
                                   errorHandler: HttpErrorHandler,
                                   authActionFactory: AuthActionFactory)(implicit executionContext: ExecutionContext)
  extends MessagesAbstractController(cc) with Logging with play.api.i18n.I18nSupport {

  object UserInfo {
    // Use a JSON format to automatically convert between case class and JsObject
    implicit val format: Format[User] = Json.format[User]
  }





  def index(): Action[AnyContent] = authActionFactory.getAuthenticatedAction(Action).async { request =>
    assets.at("index.html")(request)
  }


  def public() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.public())
  }

  def login_or_signup() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.login_or_signup(LoginForm.form, SignupForm.form, featureToggleService.getSmuiHeadline))
  }

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


  def signup() = Action { implicit request: Request[AnyContent] =>

    SignupForm.form.bindFromRequest.fold(
      formWithErrors => {
        logger.info("CAME INTO error while signing up")
        BadRequest(views.html.login_or_signup(LoginForm.form, formWithErrors,featureToggleService.getSmuiHeadline))
      },
      userData => {
        logger.info("CAME INTO successFunction for signup")
        UserDAO.addUser(userData.name, userData.email, userData.password)
          .map(_ => Redirect(routes.FrontendController.index()).withSession(request.session + ("sessionToken" -> SessionDAO.generateToken(userData.email))))
          .getOrElse(BadRequest(views.html.login_or_signup(LoginForm.form,SignupForm.form,featureToggleService.getSmuiHeadline))
          )
      }
    )
  }

  def login = Action { implicit request =>
    LoginForm.form.bindFromRequest.fold(
      formWithErrors => {
        logger.info("CAME INTO error for login")
        BadRequest(views.html.login_or_signup(formWithErrors,SignupForm.form, featureToggleService.getSmuiHeadline))
      },
      userData => {
        logger.info("CAME INTO successFunction for login")
        if (isValidLogin(userData.email, userData.password)) {
          val token = SessionDAO.generateToken(userData.email)

          Redirect(routes.FrontendController.index()).withSession(request.session + ("sessionToken" -> token))
        }
        else {
         //Redirect(routes.FrontendController.login_or_register(LoginForm.form, featureToggleService.getSmuiHeadline))
          BadRequest(views.html.login_or_signup(LoginForm.form,SignupForm.form,featureToggleService.getSmuiHeadline))
        }
        //Redirect(routes.Application.showContact(contactId)).flashing("success" -> "Contact saved!")
      }
    )
  }


  def logout() = Action { implicit request: Request[AnyContent] =>
    Redirect(routes.FrontendController.login_or_signup()).withNewSession
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

  private def isValidLogin(email: String, password: String): Boolean = {
    UserDAO.getUser(email).exists(_.password == password)
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

