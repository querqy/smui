package controllers

import javax.inject.Inject
import play.api.libs.json.Json
import play.api.mvc._
import play.api.http.HttpErrorHandler
import play.api.{Configuration, Logging}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scalaj.http.{Http, HttpOptions}
import pdi.jwt.{Jwt, JwtOptions, JwtAlgorithm, JwtClaim, JwtJson}



class OpenidController @Inject()(override val controllerComponents: ControllerComponents, errorHandler: HttpErrorHandler, appConfig: Configuration)(implicit ec: ExecutionContext) extends AbstractController(controllerComponents) with Logging {

  private val JWT_COOKIE = getValueFromConfigWithFallback("smui.JWTOpenIdAuthenticatedAction.cookie.name", "jwt")

  private def redirectToHomePage(): Future[Result] = {
    Future {
      Results.Redirect("http://localhost:9000/")
    }
  }

  private def getValueFromConfigWithFallback(key: String, default: String): String = {
    appConfig.getOptional[String](key) match {
      case Some(value: String) => value
      case None =>
        logger.warn(s":: No value for $key found. Setting pass to super-default.")
        default
    }
  }

  def callback() = Action { implicit request: Request[AnyContent] =>
    logger.warn("Here is the authorization code: "  + request.getQueryString("code"))


    val code: Option[String] = request getQueryString "code"
    val upper = code map { _.trim } filter { _.length != 0 }


    logger.warn("We now have a Authorization Code, and now we need to convert it to a Access Token.")

    val result = Http("http://keycloak:9080/auth/realms/smui/protocol/openid-connect/token").postForm
    .param("grant_type", "authorization_code")
    .param("client_id", "smui")
    .param("redirect_uri","http://localhost:9000/auth/openid/callback")
    .param("code", upper getOrElse "")
    .header("Content-Type", "application/x-www-form-urlencoded")
    .header("Charset", "UTF-8")
    .option(HttpOptions.readTimeout(10000)).asString

    logger.warn(s"Result is $result" )

    val responseJson = Json.parse(result.body)

    val accessToken : String = responseJson("access_token").as[String]

    val decodedAccessToken = Jwt
      .decodeRawAll(
        accessToken,
        JwtOptions(signature = false, expiration = false, notBefore = false)
      )


    logger.warn("Decoded access token: " + decodedAccessToken)

    // This should come from the decodedAccessToken, not from the responseJson ;-(
    val scope : String = responseJson("scope").as[String]


    logger.warn("Scope is " + scope)



    Results.Redirect("http://localhost:9000/health").withCookies(Cookie(JWT_COOKIE, accessToken))
  }
}
