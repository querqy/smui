package controllers.auth

import com.jayway.jsonpath.JsonPath
import net.minidev.json.JSONArray
import pdi.jwt.{JwtAlgorithm, JwtClaim, JwtJson}
import play.api.mvc._
import play.api.{Configuration, Logging}
import scalaj.http.{Http, HttpOptions}
import play.api.libs.json.Json
import java.util.Base64

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class JWTJsonAuthenticatedAction(parser: BodyParsers.Default, appConfig: Configuration)(implicit ec: ExecutionContext)
  extends ActionBuilderImpl(parser) with Logging {

  logger.warn("In JWTJsonAuthenticatedAction")

  private val JWT_LOGIN_URL = getValueFromConfigWithFallback("smui.JWTJsonAuthenticatedAction.login.url", "")
  private val JWT_COOKIE = getValueFromConfigWithFallback("smui.JWTJsonAuthenticatedAction.cookie.name", "jwt")
  private val JWT_PUBLIC_KEY = getValueFromConfigWithFallback("smui.JWTJsonAuthenticatedAction.public.key", "")
  private val JWT_ALGORITHM = getValueFromConfigWithFallback("smui.JWTJsonAuthenticatedAction.algorithm", "rsa")

  private val JWT_AUTHORIZATION_ACTIVE = getValueFromConfigWithFallback("smui.JWTJsonAuthenticatedAction.authorization.active", "false").toBoolean
  private val JWT_ROLES_JSON_PATH = getValueFromConfigWithFallback("smui.JWTJsonAuthenticatedAction.authorization.json.path", "$.roles")
  private val JWT_AUTHORIZED_ROLES = getValueFromConfigWithFallback("smui.JWTJsonAuthenticatedAction.authorization.roles", "admin")

  private lazy val authorizedRoles = JWT_AUTHORIZED_ROLES.replaceAll("\\s", "").split(",").toSeq

  private def getValueFromConfigWithFallback(key: String, default: String): String = {
    appConfig.getOptional[String](key) match {
      case Some(value: String) => value
      case None =>
        logger.warn(s":: No value for $key found. Setting pass to super-default.")
        default
    }
  }

  private def decodeJwtToken(jwt: String): Try[JwtClaim] = {
    logger.warn(s":: attempting to decode $jwt .")
    logger.warn("aglo is " + JWT_ALGORITHM)
    //JWT_PUBLIC_KEY = "lx9B442HNg100vqbknfStZr1EPfygoQLktBOMYkmDVWoxXPWgkSszKEX83liNt84femCJT1HdU2zri9mZlERaiwFFFqSrdx7H5i06zSOPAKTfPEz3pTgMRICCPbQtd8DnzcZdbymmNrJsmpryfkVaQaqTZpXcN5t0sW-9VGkFHwo2J9kzaSQzONOTUvZ-I5oXi5jYMdYmY4nMzvkJqVu2GwyUiap5BCsrVfausjPLIs1JICJB8tIhangcWfBEO8nsDsIynF30CZOf9D7xOb0JlDyHiBI9d6qc0iT-Efk39A6ckSVzC1yIQN3fHrb3RTNfPN-duhFOxGNDKQhZC3gDw"
    logger.warn(s"Public Key is $JWT_PUBLIC_KEY")
    logger.warn("boom:" + JwtJson.decode(jwt, JWT_PUBLIC_KEY, JwtAlgorithm.allRSA()))

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
    logger.warn(s"\n Trying to deal with $jwt")
    decodeJwtToken(jwt) match {
      case Success(token) => Some(token)
      case Failure(_) => None
    }
  }

  private def isAuthorized(token: String): Boolean = {
    logger.warn("HEY THERE")
    if (JWT_AUTHORIZATION_ACTIVE) {
      val rolesInToken = Try(JsonPath.read[JSONArray](token, JWT_ROLES_JSON_PATH).toArray.toSeq)

      rolesInToken match {
        case Success(roles) => roles.forall(authorizedRoles.contains)
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

    logger.warn(s":: invokeBlock :: request.path = ${request.path}")

    logger.warn("Hi there")
    if (request.path == "/auth/openid/callback"){
      // https://www.appsdeveloperblog.com/keycloak-authorization-code-grant-example/
      logger.warn("We gotacallback!!!!");





      val key = Base64.getDecoder.decode("lx9B442HNg100vqbknfStZr1EPfygoQLktBOMYkmDVWoxXPWgkSszKEX83liNt84femCJT1HdU2zri9mZlERaiwFFFqSrdx7H5i06zSOPAKTfPEz3pTgMRICCPbQtd8DnzcZdbymmNrJsmpryfkVaQaqTZpXcN5t0sW-9VGkFHwo2J9kzaSQzONOTUvZ-I5oXi5jYMdYmY4nMzvkJqVu2GwyUiap5BCsrVfausjPLIs1JICJB8tIhangcWfBEO8nsDsIynF30CZOf9D7xOb0JlDyHiBI9d6qc0iT-Efk39A6ckSVzC1yIQN3fHrb3RTNfPN-duhFOxGNDKQhZC3gDw")

      val claim = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJKVEYyRzlMbzNUNllabGtVcTU1ZmhTQVlYUjVqVlRUSkFFd2Q4Smc3VDVrIn0.eyJleHAiOjE2MTQ2MzEzMDMsImlhdCI6MTYxNDYzMTAwMywiYXV0aF90aW1lIjoxNjE0NjIzNzE0LCJqdGkiOiJjZTg5YjlmYi00ZjQzLTQ0YmYtYTkwZC00ZjRhZTNlODYzOTAiLCJpc3MiOiJodHRwOi8va2V5Y2xvYWs6OTA4MC9hdXRoL3JlYWxtcy9zbXVpIiwiYXVkIjoiYWNjb3VudCIsInN1YiI6ImI5ZDJkMjgwLTNhZjEtNGMxYS05NTZiLTU4ODhiMWFiYTc1NCIsInR5cCI6IkJlYXJlciIsImF6cCI6InNtdWkiLCJzZXNzaW9uX3N0YXRlIjoiNmFmZWExZDYtMDQxMy00OWE5LWJmZjEtZWI5ZDM4M2MwMDQ2IiwiYWNyIjoiMCIsImFsbG93ZWQtb3JpZ2lucyI6WyIqIiwiaHR0cDovL2xvY2FsaG9zdDo5MDAwIl0sInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJvZmZsaW5lX2FjY2VzcyIsInVtYV9hdXRob3JpemF0aW9uIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJwcm9maWxlIGVtYWlsIiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJuYW1lIjoiaGVucnkgY2xheSIsInByZWZlcnJlZF91c2VybmFtZSI6ImhlbnJ5IiwiZ2l2ZW5fbmFtZSI6ImhlbnJ5IiwiZmFtaWx5X25hbWUiOiJjbGF5IiwiZW1haWwiOiJoZW5yeUBjbGF5LmNvbSJ9.iFufJ-Sj01wTa-DTJA1VHuaVmIANjvUyCYSVkMBau0o77CSARkKSI1_VdA8OgwDi5_oMG2sW7XFaVF-lwMbdM0-d9g6jkZVFUp04Kx6vyBnKb7io1794BjRM4NVM2bi11hF94qX9eaIszcXLe2nCYWVZ6VWOg-VL_P5hal3wKKQFmYWw39900UAkuvOkwUh3CihhRKI3b1Zw0ZH6JihC3AEKwTF5lkLIw00GYhSF_HLKyw_yLPEfockS-iNPYosQEH-rEaALkNO79HM52BrPpnrpIMWoPRHagD81PK6jkYjUXKdsges-TT4MxrRmgDrvqJxDDqrAc726m63W2eJhgQ"

      logger.warn("Big TRY 2:" + JwtJson.decode(claim, key, Seq(JwtAlgorithm.RS256)))















      logger.warn(s"body is $request.body")

      logger.warn("Hereis code: "  + request.getQueryString("code"))
      logger.warn("Here is session_state: "  + request.getQueryString("session_state"))

      val code: Option[String] = request getQueryString "code"
      val upper = code map { _.trim } filter { _.length != 0 }

      logger.warn(upper getOrElse "")

      logger.warn(s"code extracted: $upper" )

      logger.warn("We now have a code, and now we need to convert it to a token that is the actual JWT.")

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

      logger.warn("Here we gooo" + responseJson("access_token"))

      //val token: Option[String] = result getQueryString "access_token"
      //val uppertwo = token map { _.trim } filter { _.length != 0 }

      //logger.warn(s"Token is $uppertwo")




      logger.warn("Experiment:" + isAuthenticated("eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJKVEYyRzlMbzNUNllabGtVcTU1ZmhTQVlYUjVqVlRUSkFFd2Q4Smc3VDVrIn0.eyJleHAiOjE2MTQ2Mjg3ODUsImlhdCI6MTYxNDYyODQ4NSwiYXV0aF90aW1lIjoxNjE0NjIzNzE0LCJqdGkiOiI3Yjg0YjJkNC0yMzM5LTRlMTgtOWNiZS1kYmY5ZjZjZjFlZTkiLCJpc3MiOiJodHRwOi8va2V5Y2xvYWs6OTA4MC9hdXRoL3JlYWxtcy9zbXVpIiwiYXVkIjoiYWNjb3VudCIsInN1YiI6ImI5ZDJkMjgwLTNhZjEtNGMxYS05NTZiLTU4ODhiMWFiYTc1NCIsInR5cCI6IkJlYXJlciIsImF6cCI6InNtdWkiLCJzZXNzaW9uX3N0YXRlIjoiNmFmZWExZDYtMDQxMy00OWE5LWJmZjEtZWI5ZDM4M2MwMDQ2IiwiYWNyIjoiMCIsImFsbG93ZWQtb3JpZ2lucyI6WyIqIiwiaHR0cDovL2xvY2FsaG9zdDo5MDAwIl0sInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJvZmZsaW5lX2FjY2VzcyIsInVtYV9hdXRob3JpemF0aW9uIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJwcm9maWxlIGVtYWlsIiwiZW1haWxfdmVyaWZpZWQiOmZhbHNlLCJuYW1lIjoiaGVucnkgY2xheSIsInByZWZlcnJlZF91c2VybmFtZSI6ImhlbnJ5IiwiZ2l2ZW5fbmFtZSI6ImhlbnJ5IiwiZmFtaWx5X25hbWUiOiJjbGF5IiwiZW1haWwiOiJoZW5yeUBjbGF5LmNvbSJ9.BP7cD98itJbZw74pf32YFd3Jlks7CYbrDsN2T9AMmjqg_-G5klZhqNYOovmrJWlTV5YRTKCvaEovd8kMPbwifQUGCaja098-vwb6fmh49wbbKU7iThjE5GGi4iIZECF5mJfpSkIJU5L8ueHegRCapAngzEmXXJND3NDXTwy1Xu2UUeNo04JmAMwIoLjHfuydaVzs8x5VCdqqiybQwPCX5j6_4eRfTsSJGj67CL_inXilLci9TnLCFu9rxSl2o4uhqUXAD6Phl2rnBTGIXVZJU4uPNGGnL6dtTTbdhW8F_OWuiyFwPTiUhMA-_e7tLOjog_ICEFiozq6U8gbbZUXrYQ"))

      logger.warn("isAuthenticated" + isAuthenticated(responseJson("access_token").toString()))



    }


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
