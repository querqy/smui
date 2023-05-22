package auth

import controllers.auth.{JWTJsonAuthenticatedAction, UserRequest}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import pdi.jwt.{JwtAlgorithm, JwtJson}
import play.api.db.{Database, Databases}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsString, Json}
import play.api.mvc.{Cookie, Request, Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Application, Mode}

import java.security.{KeyPairGenerator, SecureRandom}
import java.util.Base64
import scala.concurrent.{ExecutionContext, Future}

class JWTJsonAuthenticatedActionSpec extends PlaySpec with MockitoSugar with GuiceOneAppPerTest with ScalaFutures {

  protected lazy val db: Database = Databases.inMemory()

  private val rsaKeyPair = generateRsaKeyPair()

  override def fakeApplication(): Application = {
    GuiceApplicationBuilder()
      .in(Mode.Test)
      .configure(Map(
        "db.default.url" -> db.url,
        "db.default.driver" -> "org.h2.Driver",
        "db.default.username" -> "",
        "db.default.password" -> "",
        "smui.authAction" -> "controllers.auth.JWTJsonAuthenticatedAction",
        "smui.JWTJsonAuthenticatedAction.login.url" -> "https://redirect.com",
        "smui.JWTJsonAuthenticatedAction.cookie.name" -> "test_token",
        "smui.JWTJsonAuthenticatedAction.public.key" -> new String(Base64.getEncoder.encode(rsaKeyPair.getPublic.getEncoded)),
        "smui.JWTJsonAuthenticatedAction.algorithm" -> "rsa",
        "smui.JWTJsonAuthenticatedAction.authorization.active" -> "true",
        "smui.JWTJsonAuthenticatedAction.authorization.json.path" -> "$.roles",
        "smui.JWTJsonAuthenticatedAction.authorization.roles" -> "admin, search-manager, smui rules analyst"
      ))
      .build()
  }

  "The JWTJsonAuthenticatedAction" must {

    "redirect if no jwt token is provided" in {
      val request = FakeRequest(GET, "/")
      val home: Future[Result] = route(app, request).get

      whenReady(home) { result =>
        result.header.status mustBe 303
        result.header.headers(LOCATION) must equal(getJwtConfiguration("login.url"))
      }
    }

    "redirect if an invalid jwt token is provided" in {
      val request = FakeRequest(GET, "/")
        .withCookies(buildJWTCookie(Seq("admin"), value = Some("invalid_token")))

      val home: Future[Result] = route(app, request).get

      whenReady(home) { result =>
        result.header.status mustBe 303
        result.header.headers(LOCATION) must equal(getJwtConfiguration("login.url"))
      }
    }

    "redirect if the user has not the right permissions" in {
      val request = FakeRequest(GET, "/")
        .withCookies(buildJWTCookie(Seq("not_admin")))

      val home: Future[Result] = route(app, request).get

      whenReady(home) { result =>
        result.header.status mustBe 303
        result.header.headers(LOCATION) must equal(getJwtConfiguration("login.url"))
      }
    }

    "lead user to SMUI if a valid rsa encoded token is provided" in {
      val request = FakeRequest(GET, "/")
        .withCookies(buildJWTCookie(Seq("search-manager")))

      val home: Future[Result] = route(app, request).get

      // TODO test seems flaky, failed once!!
      whenReady(home) { result =>
        result.header.status mustBe 200
      }
    }

    "let users pass to SMUI if they have the right role even if they also have other roles" in {
      val request = FakeRequest(GET, "/")
        .withCookies(buildJWTCookie(Seq("search-manager", "barkeeper")))

      val home: Future[Result] = route(app, request).get

      whenReady(home) { result =>
        result.header.status mustBe 200
      }
    }

    "let users pass to SMUI if they have role containing a whitespace character" in {
      val request = FakeRequest(GET, "/")
        .withCookies(buildJWTCookie(Seq("smui rules analyst")))

      val home: Future[Result] = route(app, request).get

      whenReady(home) { result =>
        result.header.status mustBe 200
      }
    }

    "should secure API routes" in {
      var request = FakeRequest(GET, "/api/v1/inputTags")
        .withCookies(buildJWTCookie(Seq("search-manager")))

      var home: Future[Result] = route(app, request).get

      whenReady(home) { result =>
        result.header.status mustBe 200
      }

      request = FakeRequest(GET, "/api/v1/inputTags")
      home = route(app, request).get

      whenReady(home) { result =>
        result.header.status mustBe 303
        result.header.headers(LOCATION) must equal(getJwtConfiguration("login.url"))
      }
    }

    "respond correct to api call" in {
      val request = FakeRequest(GET, "/api/v1/allRulesTxtFiles")
        .withCookies(buildJWTCookie(Seq("search-manager")))

      val home: Future[Result] = route(app, request).get

      whenReady(home) { result =>
        result.header.status mustBe 200
      }
    }

    "return a UserRequest if the subject claim is present" in {
      val request = FakeRequest(GET, "/api/v1/allRulesTxtFiles")
        .withCookies(buildJWTCookie(Seq("search-manager")))
      val authenticator = app.injector.instanceOf[JWTJsonAuthenticatedAction]
      var modifiedRequest: Request[Any] = request

      val authenticated = authenticator.invokeBlock(request, (receivedRequest: Request[Any]) => {
        modifiedRequest = receivedRequest
        Future.successful(Results.Ok)
      })

      whenReady(authenticated) { _ =>
        modifiedRequest mustBe a[UserRequest[Any]]
      }
    }

    "not touch the request if the subject claim is not present" in {
      val request = FakeRequest(GET, "/api/v1/allRulesTxtFiles")
        .withCookies(buildJWTCookie(Seq("search-manager"), optUserName = None))
      val authenticator = app.injector.instanceOf[JWTJsonAuthenticatedAction]
      var modifiedRequest: Request[Any] = request

      val authenticated = authenticator.invokeBlock(request, (receivedRequest: Request[Any]) => {
        modifiedRequest = receivedRequest
        Future.apply(Results.Ok)(ExecutionContext.global)
      })

      whenReady(authenticated) { _ =>
        modifiedRequest mustBe request
        modifiedRequest must not be a[UserRequest[Any]]
      }
    }
  }

  private def generateRsaKeyPair() = {
    val keyGen = KeyPairGenerator.getInstance("RSA")
    keyGen.initialize(2048, new SecureRandom())
    keyGen.generateKeyPair()
  }

  private def buildJWTCookie(roles: Seq[String] = Seq.empty, optUserName: Option[String] = Option("test_user"), value: Option[String] = None) = {
    var token = Json.obj(("roles", roles))
    for (userName <- optUserName) {
      token = token + ("sub", JsString(userName))
    }

    Cookie(
      name = getJwtConfiguration("cookie.name"),
      value = if (value.isEmpty) JwtJson.encode(token, rsaKeyPair.getPrivate, JwtAlgorithm.RS512) else value.get
    )
  }

  private def getJwtConfiguration(key: String): String = {
    app.configuration.get[String]("smui.JWTJsonAuthenticatedAction." + key)
  }

}
