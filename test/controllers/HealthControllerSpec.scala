package controllers

import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.ContentTypes
import play.api.mvc.{ControllerComponents, DefaultControllerComponents, Result}
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers._

import scala.concurrent.Future


class HealthControllerSpec extends PlaySpec with MockitoSugar {

  "The HealthController" must {
    "provide an health status json" in {
      val stubControllerComponents = Helpers.stubControllerComponents()
      val controller = new HealthController(stubControllerComponents)
      val result: Future[Result] = controller.health.apply(FakeRequest())
      val bodyText: String = contentAsString(result)
      contentType(result) mustBe Some(ContentTypes.JSON)
      (contentAsJson(result) \ "name").asOpt[String] mustBe Some("search-management-ui")
    }
  }
}