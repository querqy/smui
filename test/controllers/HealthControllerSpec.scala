package controllers

import org.scalatestplus.play._
import play.api.http.ContentTypes
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future


class HealthControllerSpec extends PlaySpec {

  "The HealthController" must {
    "provide an health status json" in {
    val controller = new HealthController
      val result: Future[Result] = controller.health.apply(FakeRequest())
      val bodyText: String = contentAsString(result)
      contentType(result) mustBe Some(ContentTypes.JSON)
      (contentAsJson(result) \ "name").asOpt[String] mustBe Some("search-management-ui")
    }
  }
}