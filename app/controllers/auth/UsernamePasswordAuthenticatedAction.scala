package controllers.auth

import controllers.Assets.Redirect
import controllers.routes
import models.{SearchManagementRepository, SessionDAO}
import play.api.Logger.logger
import play.api.{Configuration, Logging}
import play.api.mvc._

import java.time.{LocalDateTime, ZoneOffset}
import scala.concurrent.{ExecutionContext, Future}

class UsernamePasswordAuthenticatedAction (searchManagementRepository: SearchManagementRepository, parser: BodyParsers.Default, appConfig: Configuration)(implicit ec: ExecutionContext)
  extends ActionBuilderImpl(parser) with Logging {

  logger.debug("In UsernamePasswordAuthenticatedAction")


  private def redirectToLoginOrSignupPage(): Future[Result] = {
    Future {
      // Having some challenges overriding the behavior of the main Angular app with the redirect, as the init that
      // looks up the features and solrs etc runs, preventing the redirect at times.  The 401 DOES work for that
      //Results.Redirect("/login_or_signup").flashing(("failure" -> "Unknown email/password combo. Double check you have the correct email address and password, or sign up for a new account."))
      Results.Unauthorized("401 Unauthorized you must either login or signup by visiting /login_or_signup")
    }
  }

  def requestAuthenticated(session: Session): Boolean = {

    val sessionTokenOpt = session.get("sessionToken")

    val user = sessionTokenOpt
      .flatMap(token => SessionDAO.getSession(token))
      .filter(_.expiration.isAfter(LocalDateTime.now(ZoneOffset.UTC)))
      .map(_.tokenData)
      .flatMap(searchManagementRepository.lookupUserByEmail)

    user match {
      case None => false
      case Some(user) => true
      case _ => false
    }
  }


  /**
   * Helper method to verify, that the request is basic authenticated with configured user/pass.
   * Code is adopted from: https://github.com/pedrorijo91/play-auth-example
   *
   * @param request
   * @return {{true}}, for user is authenticated.
   */
  override def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {
    logger.debug(s":: invokeBlock :: request.path = ${request.path}")
    val sessionTokenOpt = request.session.get("sessionToken")
    logger.info("invokeBlock for: " + request.path + " (" + sessionTokenOpt + ")")
    if (requestAuthenticated(request.session)) {
        logger.info("Request authed for: " + request.path + " (token = " + sessionTokenOpt + ")")
        block(request)
    } else {
      if(request.path.equals("/")) {
        block(request)
      } else {
        logger.info("lets take you to the login screen from " + request.path + " (" + sessionTokenOpt + ")")
        // TODO return error JSON with authorization violation details, redirect target eventually (instead of empty 401 body)
        //Results.Unauthorized("401 Unauthorized").withHeaders(("WWW-Authenticate", "Basic realm=SMUI"))
        //Redirect(routes.FrontendController.login_or_signup)
        redirectToLoginOrSignupPage()
      }
    }
  }
}
