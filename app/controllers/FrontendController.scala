package controllers

import controllers.LoginForm.UserData
import controllers.auth.AuthActionFactory
import models.FeatureToggleModel.FeatureToggleService
import models.{FeatureToggleModel, SessionDAO, User, UserDAO}
import play.api.Logging
import play.api.data.Form
import play.api.http.HttpErrorHandler
import play.api.libs.json.{Format, Json}
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

  def login_or_register() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.login_or_register(LoginForm.form, featureToggleService.getSmuiHeadline))
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


  def loginoff(username: String, pass: String) = Action { implicit request: Request[AnyContent] =>
    if (isValidLogin(username, pass)) {
      val token = SessionDAO.generateToken(username)

      Redirect(routes.FrontendController.index()).withSession(request.session + ("sessionToken" -> token))
    } else {
      // we should redirect to login page
      Unauthorized(views.html.defaultpages.unauthorized()).withNewSession
    }
  }

  //def loginsss = Action { implicit request: MessagesRequest[AnyContent] =>

    //logger.info("We are in login()")
//
//    val errorFunction = { formWithErrors: Form[LoginForm.UserData] =>
//      logger.info("CAME INTO errorFunction")
//      // this is the bad case, where the form had validation errors.
//      // show the user the form again, with the errors highlighted.
//      BadRequest(views.html.login_or_register(formValidationResult, featureToggleService.getSmuiHeadline)).withNewSession.flashing("failure"->"Please try again")
//    }
//
//    val successFunction = { userData: UserData =>
//      logger.info("CAME INTO successFunction")
//      // this is the SUCCESS case, where the form was successfully parsed as a BlogPost
//
//      //Redirect(routes.FrontendController.index()).flashing("info" -> "Blog post added (trust me)")
//      if (isValidLogin(userData.email, userData.password)) {
//        val token = SessionDAO.generateToken(userData.email)
//
//        Redirect(routes.FrontendController.index()).withSession(request.session + ("sessionToken" -> token))
//      } else {
//        logger.info("not Valid Login, so now what")
//        Console.println(LoginForm.form)
//        //LoginForm.form.s
//        // we should redirect to login page
//        //Unauthorized(views.html.defaultpages.unauthorized()).withNewSession
//        //BadRequest(views.html.login_or_register(LoginForm.form, featureToggleService.getSmuiHeadline))
//        //Redirect(routes.FrontendController.login_or_register(LoginForm.form, featureToggleService.getSmuiHeadline)).flashing("hi")
//        Results.Redirect("/login_or_register")
//      }
//    }
//
//    val formValidationResult: Form[LoginForm.UserData] = LoginForm.form.bindFromRequest
//    formValidationResult.fold(
//      errorFunction,   // sad case
//      successFunction  // happy case
//    )
//
//    val formValidationResult: Form[LoginForm.UserData] = LoginForm.form.bindFromRequest.fold(
//      formWithErrors => {
//        // binding failure, you retrieve the form containing errors:
//        BadRequest(views.html.user(formWithErrors))
//      },
//      userData => {
//        /* binding success, you get the actual value. */
//        val newUser = models.User(userData.name, userData.age)
//        val id      = models.User.create(newUser)
//        Redirect(routes.Application.home(id))
//      }
//    )
//  }

  def login = Action { implicit request =>
    LoginForm.form.bindFromRequest.fold(
      formWithErrors => {
        logger.info("CAME INTO error")
        BadRequest(views.html.login_or_register(formWithErrors,featureToggleService.getSmuiHeadline))
      },
      userData => {
        logger.info("CAME INTO successFunction")
        if (isValidLogin(userData.email, userData.password)) {
          val token = SessionDAO.generateToken(userData.email)

          Redirect(routes.FrontendController.index()).withSession(request.session + ("sessionToken" -> token))
        }
        else {
         //Redirect(routes.FrontendController.login_or_register(LoginForm.form, featureToggleService.getSmuiHeadline))
          BadRequest(views.html.login_or_register(LoginForm.form,featureToggleService.getSmuiHeadline))
        }
        //Redirect(routes.Application.showContact(contactId)).flashing("success" -> "Contact saved!")
      }
    )
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

