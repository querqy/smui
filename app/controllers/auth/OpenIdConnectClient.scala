package controllers.auth

import pdi.jwt.{JwtClaim, JwtJson, JwtOptions}
import play.api.Configuration
import play.api.libs.json.{Json, OFormat}
import play.api.libs.ws.WSClient

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationInt

class OpenIdConnectClient @Inject()(ws: WSClient, appConfig: Configuration)
                                   (implicit ec: ExecutionContext) {

  val DISCOVERY_URL: String = appConfig.get[String]("smui.OpenIdConnect.discovery.url")

  val CALLBACK_URL: String = appConfig.get[String]("smui.OpenIdConnect.callback.url")

  val CLIENT_ID: String = appConfig.get[String]("smui.OpenIdConnect.client")

  def discoverAuthorizationUrl(): Future[String] = {
    discoverEndpoints().map { endpoints =>
      ws.url(endpoints.authorization_endpoint)
        .withQueryStringParameters(
          "client_id" -> CLIENT_ID,
          "response_type" -> "code",
          "redirect_uri" -> CALLBACK_URL
        )
        .uri
        .toString
    }
  }

  def disoverEndSessionUrl(): Future[String] = {
    discoverEndpoints().map(_.end_session_endpoint)
  }

  def obtainAccessToken(code: String): Future[JwtClaim] = {
    for {
      endpoints <- discoverEndpoints()
      tokenEndpoint = endpoints.token_endpoint
      accessTokenResponse <- getAccessToken(tokenEndpoint, code)
      accessToken = accessTokenResponse.access_token
      // decode without checking signature as we have received the claim from a trusted source
      claim <- Future.fromTry(JwtJson.decode(accessToken, JwtOptions(signature = false)))
    } yield claim
  }

  private def getAccessToken(tokenEndpoint: String, code: String): Future[AccessTokenResponse] = {
    ws.url(tokenEndpoint)
      .withRequestTimeout(10.seconds)
      .post(Map(
        "client_id"->  CLIENT_ID,
        "grant_type" -> "authorization_code",
        // redirect_uri is required for "verification" according to https://www.oauth.com/oauth2-servers/redirect-uris/redirect-uri-validation/
        "redirect_uri" -> CALLBACK_URL,
        "code" -> code))
      .map(_.json.as[AccessTokenResponse])
  }

  private def discoverEndpoints(): Future[DiscoveryResponse] = {
    ws.url(DISCOVERY_URL)
      .withRequestTimeout(10.seconds)
      .get()
      .map(_.json.as[DiscoveryResponse])
  }
}

// Selected OpenID Provider Metadata fields from an OpenID Connect Discovery response
case class DiscoveryResponse(authorization_endpoint: String,
                             token_endpoint: String,
                             end_session_endpoint: String)

object DiscoveryResponse {
  implicit val jsonFormat: OFormat[DiscoveryResponse] = Json.format[DiscoveryResponse]
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

