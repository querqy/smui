package controllers.auth

import org.apache.commons.codec.binary.Base64.decodeBase64
import play.api.Configuration
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.Exception.allCatch

class BasicAuthAuthenticatedAction(parser: BodyParsers.Default,
                                   appConfig: Configuration)(implicit ec: ExecutionContext) extends ActionBuilderImpl(parser) {

  private val logger = play.api.Logger

  logger.debug("In BasicAuthAuthenticatedAction")

  val BASIC_AUTH_USER = appConfig.getString("smui.BasicAuthAuthenticatedAction.user") match {
    case Some(strUser: String) => {
      strUser
    }
    case None => {
      logger.error(":: No value for smui.BasicAuthAuthenticatedAction.user found. Setting user to super-default.")
      "smui"
    }
  }

  val BASIC_AUTH_PASS = appConfig.getString("smui.BasicAuthAuthenticatedAction.pass") match {
    case Some(strUser: String) => {
      strUser
    }
    case None => {
      logger.error(":: No value for smui.BasicAuthAuthenticatedAction.pass found. Setting pass to super-default.")
      "smui"
    }
  }

  override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {

    logger.debug(s":: invokeBlock :: request.path = ${request.path}")

    /**
      * Helper method to verify, that the request is basic authenticated with configured user/pass.
      * Code is adopted from: https://dzone.com/articles/play-basic-authentication
      *
      * @param request
      * @return {{true}}, for user is authenticated.
      */
    def requestAuthenticated(request: Request[A]): Boolean = {

      request.headers.get("Authorization") match {
        case Some(authorization: String) => {
          authorization.split(" ").drop(1).headOption.filter { encoded =>
            val authInfo = new String(decodeBase64(encoded.getBytes)).split(":").toList
            allCatch.opt {
              val (username, password) = (authInfo.head, authInfo(1))
              (username.equals(BASIC_AUTH_USER) && password.equals(BASIC_AUTH_PASS))
            } getOrElse false
          }.map(_ => true).getOrElse(false)
        }
        case None => false
      }
    }

    if( requestAuthenticated(request) ) {
      block(request)
    } else {
      Future {
        // TODO return error JSON with authorization violation details, redirect target eventually (instead of empty 401 body)
        Results.Unauthorized("401 Unauthorized").withHeaders(("WWW-Authenticate", "Basic realm=SMUI"))
      }
    }
  }
}
