package controllers.auth

import com.auth0.jwk.UrlJwkProvider
import com.jayway.jsonpath.JsonPath
import net.minidev.json.JSONArray
import pdi.jwt._
import play.api.mvc._
import play.api.{Configuration, Logging}

import java.net.URL
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

class JWTOpenIdAuthenticatedAction(parser: BodyParsers.Default, appConfig: Configuration)(implicit ec: ExecutionContext)
  extends ActionBuilderImpl(parser) with Logging {

  logger.warn("In JWTOpenIdAuthenticatedAction")

  private val JWT_LOGIN_URL = getValueFromConfigWithFallback("smui.JWTOpenIdAuthenticatedAction.login.url", "")
  private val JWKS_URL = new URL(getValueFromConfigWithFallback("smui.JWTOpenIdAuthenticatedAction.jwks.url", ""))
  private val JWT_COOKIE = getValueFromConfigWithFallback("smui.JWTOpenIdAuthenticatedAction.cookie.name", "jwt")
  private val JWT_AUTHORIZED_ROLES = getValueFromConfigWithFallback("smui.JWTOpenIdAuthenticatedAction.authorization.roles", "admin")

  private val JWT_ROLES_JSON_PATH = getValueFromConfigWithFallback("smui.JWTOpenIdAuthenticatedAction.roles.json.path", "resource_access.smui.roles")

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

  private def isAuthenticated(jwt: String): Option[JwtClaim] = {
    logger.info(s"Authenticating using $jwt")

    // get the pub key of the signing key to verify signature
    val maybeJwk = for {
      // decode without verifying as we only need the header
      //(header, _, _) <- JwtJson.decodeRawAll(jwt, JwtOptions(signature = false)).toOption

      // decode without any verification as the token is most likely already expired
      (header, _, _) <- JwtJson.decodeRawAll(jwt, JwtOptions(false, false, false)).toOption

      keyId <- JwtJson.parseHeader(header).keyId
      jwk <- Try(new UrlJwkProvider(JWKS_URL).get(keyId)).toOption
    } yield jwk

    for {
      jwk <- maybeJwk
      // claims <- JwtJson.decode(jwt, jwk.getPublicKey, Seq(JwtAlgorithm.RS256)).toOption

      // decode without any verification as the token is most likely already expired
      claims <- JwtJson.decode(jwt, jwk.getPublicKey, Seq(JwtAlgorithm.RS256), JwtOptions(false, false, false)).toOption
    } yield claims
  }

  private def isAuthorized(claim: JwtClaim): Boolean = {
    logger.warn("ERIC HERE, claim content is: " + claim.content)
    logger.warn("ERIC HERE, JWT_ROLES_JSON_PATH is " + JWT_ROLES_JSON_PATH)
    //val rolesInToken = Try(JsonPath.read[JSONArray](claim.content, JWT_ROLES_JSON_PATH).toArray.toSeq)
    // I could get a {"scope"="smui:searchandizer"} in my claim, but not a {"resource_access":{"smui":{"roles":["smui-user"]}}}
    // So changing this code just to get it to work.
    val rolesInToken = Try(JsonPath.read[String](claim.content, JWT_ROLES_JSON_PATH).split(" ").toArray.toSeq)
    logger.warn("ERIC HERE " + rolesInToken)
    rolesInToken match {
      case Success(roles) => roles.forall(authorizedRoles.contains)
      case _ => false
    }
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

    getJwtCookie(request) match {
      case Some(cookie) =>
        isAuthenticated(cookie.value) match {
          case Some(token) if isAuthorized(token) => block(request)
          case _ => redirectToLoginPage()
        }
      case None => redirectToLoginPage()
    }
  }
}
