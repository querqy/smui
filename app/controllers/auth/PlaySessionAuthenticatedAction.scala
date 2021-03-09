package controllers.auth

import play.api.http.Status.TEMPORARY_REDIRECT
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.Results.{Redirect, Unauthorized}
import play.api.mvc.{ActionBuilderImpl, BodyParsers, Request, Result, Session}
import play.api.{Configuration, Logging}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

/**
 * An action that reads a well-known value key from the Play session cookie and only lets authorized configured groups pass.
 */
class PlaySessionAuthenticatedAction @Inject()(parser: BodyParsers.Default, appConfig: Configuration)
                                              (implicit ec: ExecutionContext) extends ActionBuilderImpl(parser) with Logging {

  // The key for retrieving roles from the Play Session cookie
  val SESSION_KEY_SMUI_ROLE: String = appConfig.get[String]("smui.PlaySessionAuthenticatedAction.key")

  // The authorized roles
  val AUTHORIZED_ROLES: Set[String] = splitCommaSeparatedString(appConfig.get[String]("smui.PlaySessionAuthenticatedAction.authorizedroles")).toSet

  // The login URL to redirect to in case of missing authentication
  val LOGIN_REDIRECT_URL: String = appConfig.get[String]("smui.PlaySessionAuthenticatedAction.loginurl")

  override def invokeBlock[A](request: Request[A], continuation: Request[A] => Future[Result]): Future[Result] = {
    readRoles(request.session) match {
      case Some(rolesAsString) =>
        if (isAuthorized(rolesAsString)) {
          logger.debug(s"Access for roles $rolesAsString granted.")
          continuation(request)
        } else {
          logger.info(s"Access for roles $rolesAsString rejected.")
          unauthorized(rolesAsString)
        }
      case None =>
        logger.info("Access denied with redirect as no roles found in session cookie")
        redirect(request)
    }
  }

  def readRoles(session: Session): Option[String] = session.get(SESSION_KEY_SMUI_ROLE)

  def withRoles(session: Session, roles: Set[String]): Session = session + (SESSION_KEY_SMUI_ROLE -> roles.mkString(","))

  def isAuthorized(givenRoles: String): Boolean = {
    isAuthorized(splitCommaSeparatedString(givenRoles).toSet)
  }

  def isAuthorized(givenRoles: Set[String]): Boolean = {
    givenRoles.intersect(AUTHORIZED_ROLES).nonEmpty
  }

  private def redirect[A](request: Request[A]): Future[Result] = Future.successful(
    if (request.path.startsWith("/api")) {
      // For API requests: send a response that can trigger a client-side redirect
      Unauthorized(Json.toJson(SmuiAuthViolation.redirect(LOGIN_REDIRECT_URL)))
    } else {
      // For non-API requests: trigger the redirect directly
      Redirect(LOGIN_REDIRECT_URL, TEMPORARY_REDIRECT)
    }
  )

  private def unauthorized(givenRoles: String): Future[Result] = Future.successful(
    Unauthorized(s"Not authorized for $givenRoles, please ask your administrator.")
  )

  private def splitCommaSeparatedString(s: String) = s.replaceAll("\\s", "").split(",").toSeq
}

// Payload understood by http-auth-interceptors.ts
case class SmuiAuthViolation(action: String, params: String)

object SmuiAuthViolation {
  implicit val jsonFormat: OFormat[SmuiAuthViolation] = Json.format[SmuiAuthViolation]

  def redirect(uri: String): SmuiAuthViolation = SmuiAuthViolation("redirect", uri)
}
