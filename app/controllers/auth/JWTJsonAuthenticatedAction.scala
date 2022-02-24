package controllers.auth

import com.jayway.jsonpath.JsonPath
import net.minidev.json.JSONArray
import pdi.jwt.{JwtAlgorithm, JwtClaim, JwtJson}
import play.api.mvc._
import play.api.{Configuration, Logging}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class JWTJsonAuthenticatedAction(parser: BodyParsers.Default, appConfig: Configuration)(implicit ec: ExecutionContext)
  extends ActionBuilderImpl(parser) with Logging {

  logger.debug("In JWTJsonAuthenticatedAction")

  private val JWT_LOGIN_URL = getValueFromConfigWithFallback("smui.JWTJsonAuthenticatedAction.login.url", "")
  private val JWT_COOKIE = getValueFromConfigWithFallback("smui.JWTJsonAuthenticatedAction.cookie.name", "jwt")
  private val JWT_PUBLIC_KEY = getValueFromConfigWithFallback("smui.JWTJsonAuthenticatedAction.public.key", "")
  private val JWT_ALGORITHM = getValueFromConfigWithFallback("smui.JWTJsonAuthenticatedAction.algorithm", "rsa")

  private val JWT_AUTHORIZATION_ACTIVE = getValueFromConfigWithFallback("smui.JWTJsonAuthenticatedAction.authorization.active", "false").toBoolean
  private val JWT_ROLES_JSON_PATH = getValueFromConfigWithFallback("smui.JWTJsonAuthenticatedAction.authorization.json.path", "$.roles")
  private val JWT_AUTHORIZED_ROLES = getValueFromConfigWithFallback("smui.JWTJsonAuthenticatedAction.authorization.roles", "admin")

  private val authorizedRoles = JWT_AUTHORIZED_ROLES.split(",").map(_.trim())

  private def getValueFromConfigWithFallback(key: String, default: String): String = {
    appConfig.getOptional[String](key) match {
      case Some(value: String) => value
      case None =>
        logger.error(s":: No value for $key found. Setting pass to super-default.")
        default
    }
  }

  private def decodeJwtToken(jwt: String): Try[JwtClaim] = {
    JWT_ALGORITHM match {
      case "hmac" => JwtJson.decode(jwt, JWT_PUBLIC_KEY, JwtAlgorithm.allHmac())
      case "asymmetric" => JwtJson.decode(jwt, JWT_PUBLIC_KEY, JwtAlgorithm.allAsymmetric())
      case "rsa" => JwtJson.decode(jwt, JWT_PUBLIC_KEY, JwtAlgorithm.allRSA())
      case "ecdsa" => JwtJson.decode(jwt, JWT_PUBLIC_KEY, JwtAlgorithm.allECDSA())
      case _ => JwtJson.decode(jwt, JWT_PUBLIC_KEY, JwtAlgorithm.allRSA())
    }
  }

  private def getJwtCookie[A](request: Request[A]): Option[Cookie] = {
    request.cookies.get(JWT_COOKIE)
  }

  private def isAuthenticated(jwt: String): Option[JwtClaim] = {
    decodeJwtToken(jwt) match {
      case Success(token) => Some(token)
      case Failure(_) => None
    }
  }

  private def isAuthorized(token: String): Boolean = {
    if (JWT_AUTHORIZATION_ACTIVE) {
      val rolesReadFromToken = Try(JsonPath.read[JSONArray](token, JWT_ROLES_JSON_PATH).toArray.toSeq)

      rolesReadFromToken match {
        case Success(rolesInToken) => rolesInToken.exists(authorizedRoles.contains)
        case _ => false
      }
    } else true
  }

  private def redirectToLoginPage(): Future[Result] = {
    Future {
      Results.Redirect(JWT_LOGIN_URL)
    }
  }

  override def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] = {

    logger.debug(s":: invokeBlock :: request.path = ${request.path}")

    getJwtCookie(request) match {
      case Some(cookie) =>
        isAuthenticated(cookie.value) match {
          case Some(token) if isAuthorized(token.content) => block(request)
          case _ => redirectToLoginPage()
        }
      case None => redirectToLoginPage()
    }
  }
}
