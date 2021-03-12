package controllers.auth

import com.jayway.jsonpath.JsonPath
import pdi.jwt.JwtClaim
import play.api.http.Status.TEMPORARY_REDIRECT
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.Results.{Redirect, Unauthorized}
import play.api.mvc._
import play.api.{Configuration, Logging}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
 * An action that reads a well-known value key from the Play session cookie and only lets authorized configured groups pass.
 * If no value was found an OpenID Connect authorization flow is triggered by redirecting to an auth URL.
 */
class OpenIdConnectAuthenticatedAction @Inject()(parser: BodyParsers.Default,
                                                 openIdConnectClient: OpenIdConnectClient,
                                                 authHelper: AuthHelper)
                                                (implicit ec: ExecutionContext) extends ActionBuilderImpl(parser) with Logging {

  // authorization check based on the SMUI Play session information
  override def invokeBlock[A](request: Request[A], continuation: Request[A] => Future[Result]): Future[Result] = {
    authHelper.rolesFromSession(request.session) match {
      case Some(rolesAsString) =>
        if (authHelper.isAuthorized(rolesAsString)) {
          logger.debug(s"Access for roles $rolesAsString granted.")
          continuation(request)
        } else {
          logger.info(s"Access for roles $rolesAsString rejected.")
          Future.successful(Unauthorized(s"Not authorized for $rolesAsString, please ask your administrator."))
        }
      case None =>
        logger.info("Access denied with redirect to login URL as no roles found in session cookie")
        redirectToAuthUrl(request)
    }
  }

  private def redirectToAuthUrl[A](request: Request[A]): Future[Result] = {
    openIdConnectClient.discoverAuthorizationUrl().map { authUrl =>
      if (request.path.startsWith("/api")) {
        // For API requests: send a response that can trigger a client-side redirect
        Unauthorized(Json.toJson(SmuiAuthViolation.redirect(authUrl)))
      } else {
        // For non-API requests: trigger the redirect directly
        Redirect(authUrl, TEMPORARY_REDIRECT)
      }
    }
  }
}

/**
 * Controller providing an endpoint for an Open ID Connect Authorization grant.
 */
class OpenIdAuthCallbackController @Inject()(override val controllerComponents: ControllerComponents,
                                             appConfig: Configuration,
                                             openIdConnectClient: OpenIdConnectClient,
                                             authHelper: AuthHelper)
                                            (implicit ec: ExecutionContext) extends AbstractController(controllerComponents) with Logging {

  // The URL parameter from which the callback endpoint is taking its one-time authorization code
  val URL_PARAMETER_AUTH_CODE: String = appConfig.get[String]("smui.OpenIdConnect.authorization.codeparam")

  // The JSONPath within the access token containing the roles of the user
  val JWT_ROLES_JSON_PATH: String = appConfig.get[String]("smui.OpenIdConnect.authorization.json.path")

  def openIdAuthCallback(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    val maybeCode = request.getQueryString(URL_PARAMETER_AUTH_CODE)
    logger.info(s"Authorization requested with code ${maybeCode.getOrElse("")}")

    maybeCode match {
      case Some(code) =>
        for {
          claim <- openIdConnectClient.obtainAccessToken(code)
          roles = readRolesFromClaim(claim).toSet
        } yield {
          if (authHelper.isAuthorized(roles)) {
            Redirect("/", TEMPORARY_REDIRECT).withSession(authHelper.sessionWithRoles(request.session, roles))
          } else {
            Unauthorized(s"Access for roles ${roles.mkString(",")} rejected.")
          }
        }
      case None =>
        Future.successful(Results.BadRequest)
    }
  }

  def logout(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    // remove the cookie and redirect to the OIDC logout URL
    openIdConnectClient.disoverEndSessionUrl().map { url =>
      Redirect(url, TEMPORARY_REDIRECT).withSession(authHelper.sessionWithoutRoles(request.session))
    }
  }

  private def readRolesFromClaim(claim: JwtClaim): Seq[String] = {
    import scala.collection.JavaConverters._

    Try(JsonPath.read[java.util.List[String]](claim.content, JWT_ROLES_JSON_PATH)) match {
      case Success(roles) => roles.asScala
      case Failure(e) =>
        logger.error("Error reading roles from claim", e)
        Seq.empty
    }
  }

}

// Some code shared between OpenIdConnectAuthenticatedAction and OpenIdAuthCallbackController
class AuthHelper @Inject()(appConfig: Configuration) {

  // The authorized roles
  val AUTHORIZED_ROLES: Set[String] = splitCommaSeparatedString(appConfig.get[String]("smui.OpenIdConnect.authorizedroles")).toSet

  // The key for retrieving roles from the Play Session cookie
  val SESSION_KEY_SMUI_ROLE: String = "smui-roles"

  def isAuthorized(givenRoles: String): Boolean = isAuthorized(splitCommaSeparatedString(givenRoles).toSet)

  def isAuthorized(givenRoles: Set[String]): Boolean = givenRoles.intersect(AUTHORIZED_ROLES).nonEmpty

  def rolesFromSession(session: Session): Option[String] = session.get(SESSION_KEY_SMUI_ROLE)

  def sessionWithRoles(session: Session, roles: Iterable[String]): Session = session + (SESSION_KEY_SMUI_ROLE -> roles.mkString(","))

  def sessionWithoutRoles(session: Session): Session = session - SESSION_KEY_SMUI_ROLE

  private def splitCommaSeparatedString(s: String) = s.replaceAll("\\s", "").split(",").toSeq

}

// Payload understood by http-auth-interceptors.ts
case class SmuiAuthViolation(action: String, params: String)

object SmuiAuthViolation {
  implicit val jsonFormat: OFormat[SmuiAuthViolation] = Json.format[SmuiAuthViolation]

  def redirect(uri: String): SmuiAuthViolation = SmuiAuthViolation("redirect", uri)
}
