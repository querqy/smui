package controllers.auth

import com.jayway.jsonpath.JsonPath
import net.minidev.json.JSONArray
import pdi.jwt.{Jwt, JwtOptions, JwtAlgorithm, JwtClaim, JwtJson}
import play.api.mvc._
import pdi.jwt._
import play.api.{Configuration, Logging}
import scalaj.http.{Http, HttpOptions}
import play.api.libs.json.Json
import java.util.Base64
import scala.util.Success

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class JWTOpenIdAuthenticatedAction(parser: BodyParsers.Default, appConfig: Configuration)(implicit ec: ExecutionContext)
  extends ActionBuilderImpl(parser) with Logging {

  logger.warn("In JWTOpenIdAuthenticatedAction")

  private val JWT_LOGIN_URL = getValueFromConfigWithFallback("smui.JWTOpenIdAuthenticatedAction.login.url", "")
  private val JWT_COOKIE = getValueFromConfigWithFallback("smui.JWTOpenIdAuthenticatedAction.cookie.name", "jwt")
  private val JWT_AUTHORIZED_ROLES = getValueFromConfigWithFallback("smui.JWTOpenIdAuthenticatedAction.authorization.roles", "admin")

  private lazy val authorizedRoles = JWT_AUTHORIZED_ROLES.replaceAll("\\s", "").split(",").toSeq

  private def getValueFromConfigWithFallback(key: String, default: String): String = {
    appConfig.getOptional[String](key) match {
      case Some(value: String) => value
      case None =>
        logger.warn(s":: No value for $key found. Setting pass to super-default.")
        default
    }
  }



  def decodeRawAll(jwt: String): Try[(String, String, String)] = {
    Jwt
    .decodeRawAll(
      jwt,
      JwtOptions(signature = false, expiration = false, notBefore = false)
    )
  }

  //private def isAuthenticated(jwt: String): Option[JwtClaim] = {
  private def isAuthenticated(jwt: String): Option[Boolean] = {
    logger.warn(s"\n Trying to deal with $jwt")


    logger.warn("We should validate the token")

    val result = for {
    (header, claim, signature) <-
        decodeRawAll(jwt)
    } yield claim


    logger.warn("claim: " + claim)



    Some(true)
  }

  private def isAuthorized(token: String): Boolean = {
    val rolesInToken = token.split(" ").toSeq
    logger.warn("Here are the rolesinToken:" + rolesInToken)
    rolesInToken.forall(authorizedRoles.contains)
  }

  private def redirectToLoginPage(): Future[Result] = {
    Future {
      Results.Redirect(JWT_LOGIN_URL)
    }
  }

  private def getJwtCookie[A](request: Request[A]): Option[Cookie] = {
    request.cookies.get(JWT_COOKIE)
  }



  override def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {

    logger.warn(s":: invokeBlock :: request.path = ${request.path}")

    if (request.path == "/auth/openid/callback"){
      // https://www.appsdeveloperblog.com/keycloak-authorization-code-grant-example/
      logger.warn("We got a callback!!!!");

    }
    getJwtCookie(request) match {
      case Some(cookie) =>
        isAuthenticated(cookie.value) match {
          case Some(token) if isAuthorized("smui:searchandizder") => block(request)
          case _ => redirectToLoginPage()
        }
      case None => redirectToLoginPage()
    }

    //if (isAuthorized("smui:searchandizer")) {
    //  block(request)
    //} else {
    //  redirectToLoginPage
    //}
  }
}
