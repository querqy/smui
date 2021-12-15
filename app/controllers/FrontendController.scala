package controllers

import controllers.LoginForm.UserData
import controllers.auth.AuthActionFactory
import controllers.helpers.UserAction
import controllers.helpers.UserRequest
import models.FeatureToggleModel.FeatureToggleService
import models.SearchManagementRepository
import models.{FeatureToggleModel, SessionDAO, SolrIndexId, User}
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
                                   val userAction: UserAction,
                                   searchManagementRepository: SearchManagementRepository,
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

  def login_or_signup(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.login_or_signup(LoginForm.form, SignupForm.form, featureToggleService.getSmuiHeadline))
  }

  def priv(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    withUser(user => Ok(views.html.priv(user)))
  }

  def privPlay(): EssentialAction = withPlayUser { user =>
    Ok(views.html.priv(user))
  }

  def privAction(): Action[AnyContent] = userAction { user: UserRequest[AnyContent] =>
    Ok(views.html.priv(user.user.get))
  }

  def priv2(): Action[AnyContent] = authActionFactory.getAuthenticatedAction(Action).async { request =>
    Future(Ok(views.html.priv2()))
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
        Option(searchManagementRepository.addUser(
          User.create(username = userData.name, email = userData.email, password = userData.password, admin = false)
        ))
          .map(_ => Redirect(routes.FrontendController.index()).withSession(request.session + ("sessionToken" -> SessionDAO.generateToken(userData.email))))
          .getOrElse(BadRequest(views.html.login_or_signup(LoginForm.form,SignupForm.form,featureToggleService.getSmuiHeadline)))
      }
    )
  }

  def login = Action { implicit request: Request[AnyContent] =>
    LoginForm.form.bindFromRequest.fold(
      formWithErrors => {
        BadRequest(views.html.login_or_signup(formWithErrors,SignupForm.form, featureToggleService.getSmuiHeadline))
      },
      userData => {
        getValidLoginUser(userData.email, userData.password)
          .map(user => Redirect(routes.FrontendController.index()).withSession(request.session + ("sessionToken" -> SessionDAO.generateToken(user.email))))
          .getOrElse(Unauthorized(views.html.defaultpages.unauthorized()))
      }
    )
  }

  def logout() = Action { implicit request: Request[AnyContent] =>
    Redirect(routes.FrontendController.login_or_signup()).withNewSession
  }

  private def getValidLoginUser(email: String, password: String): Option[User] = {
    searchManagementRepository.lookupUserByEmail(email).filter(_.password == password)
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
      .map(_.tokenData)
      .flatMap(searchManagementRepository.lookupUserByEmail)
  }


}

