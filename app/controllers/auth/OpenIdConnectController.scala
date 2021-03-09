package controllers.auth

import com.jayway.jsonpath.JsonPath
import pdi.jwt.{JwtClaim, JwtJson, JwtOptions}
import play.api.libs.json.{Json, OFormat}
import play.api.libs.ws.WSClient
import play.api.mvc._
import play.api.{Configuration, Logging}

import javax.inject.Inject
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
 * Endpoint for callback of OpenID Authentication Grant
 */
class OpenidController @Inject()(override val controllerComponents: ControllerComponents,
                                 ws: WSClient,
                                 appConfig: Configuration,
                                 playSessionAuthenticatedAction: PlaySessionAuthenticatedAction)
                                (implicit ec: ExecutionContext) extends AbstractController(controllerComponents) with Logging {

  // The URL parameter from which this controller is taking its one-time authorization code
  val URL_PARAMETER_AUTH_CODE: String = appConfig.get[String]("smui.OpenIdConnect.authorization.codeparam")

  // The endpoint providing a code to auth token exchange
  val URL_TOKEN_ENDPOINT: String = appConfig.get[String]("smui.OpenIdConnect.token.url")

  val CLIENT_ID: String = appConfig.get[String]("smui.OpenIdConnect.client")

  // The redirect_uri parameter for the token request, only used for verification against the
  // redirect_uri parameter from the initial auth request
  val REDIRECT_URL: String = appConfig.get[String]("smui.OpenIdConnect.redirect.url")

  // The JSONPath within the access token containing the roles of the user
  val JWT_ROLES_JSON_PATH: String = appConfig.get[String]("smui.OpenIdConnect.authorization.json.path")

  def callback(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    val maybeCode = request.getQueryString(URL_PARAMETER_AUTH_CODE)
    logger.info(s"Authorization requested with code ${maybeCode.getOrElse("")}")

    maybeCode match {
      case Some(code) =>
        exchangeCodeForAccessToken(code).map { accessToken =>
          val roles = readRolesFromAccessTokenResponse(accessToken).toSet
          if (playSessionAuthenticatedAction.isAuthorized(roles)) {
            Redirect("/").withSession(
              playSessionAuthenticatedAction.withRoles(request.session, roles)
            )
          } else {
            Unauthorized(s"Access for roles ${roles.mkString(",")} rejected.")
          }
        }
      case None =>
        Future.successful(Results.BadRequest)
    }
  }

  private def readRolesFromAccessTokenResponse(token: AccessTokenResponse): Seq[String] = {
    // no need to JWKS-verify a token we just received from a trusted source
    JwtJson.decode(token.access_token, JwtOptions(signature = false)) match {
      case Success(claim) => readRolesFromClaim(claim)
      case Failure(e) =>
        logger.error(s"Error decoding JWT (was: $token)", e)
        Seq.empty
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

  private def exchangeCodeForAccessToken(code: String): Future[AccessTokenResponse] = {
    ws.url(URL_TOKEN_ENDPOINT)
      .withRequestTimeout(10.seconds)
      .post(Map(
        "client_id"->  CLIENT_ID,
        "grant_type" -> "authorization_code",
        "redirect_uri" -> REDIRECT_URL,
        "code" -> code))
      .map(_.json.as[AccessTokenResponse])
  }
}

// An OAuth2 server Access Token Response
case class AccessTokenResponse(access_token: String,
                               token_type: String,
                               expires_in: Option[Long],
                               refresh_token: Option[String],
                               refresh_expires_in: Option[Long],
                               scope: Option[String])

object AccessTokenResponse {
  implicit val jsonFormat: OFormat[AccessTokenResponse] = Json.format[AccessTokenResponse]
}
