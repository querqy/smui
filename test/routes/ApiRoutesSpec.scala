package routes

import controllers.HealthController
import models.spellings.{AlternateSpelling, CanonicalSpelling, CanonicalSpellingWithAlternatives}
import models.{ApplicationTestBase, ListItem, ListItemType, SearchRulesAndSpellingsForList}
import org.scalatest.{FlatSpec, Matchers}
import play.api.http.ContentTypes
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, route, _}

import scala.concurrent.Future

class ApiRoutesSpec extends FlatSpec with Matchers with ApplicationTestBase {

  override protected def beforeAll(): Unit = {
    super.beforeAll()

    createTestCores()
    createTestRule()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    createTestSpellings()
  }

  override def afterEach(): Unit = {
    super.beforeEach()
    deleteAllSpellingsFromDB(core1Id)
  }

  "The health route" should "return a valid response" in {
    val request = FakeRequest(GET, "/health")
    val result: Future[Result] = route(application, request).get

    status(result) shouldBe 200
    contentType(result) shouldBe Some(ContentTypes.JSON)
    contentAsString(result) should not be empty
  }

  "The search inputs and spellings" should "be returned as list items" in {
    import ListItemType._

    val request = FakeRequest(GET, s"/api/v1/${core1Id.id}/rules-and-spellings")
    val result: Future[Result] = route(application, request).get

    status(result) shouldBe 200
    val listItems = contentAsJson(result).as[Seq[ListItem]]

    listItems.map(_.term) shouldBe Seq("aerosmith", "freezer", "inactive", "machine", "pants", "shipping")
    listItems.map(_.itemType) shouldBe Seq(RuleManagement, Spelling, RuleManagement, Spelling, Spelling, RuleManagement)

    listItems.find(_.term == "inactive").get.isActive shouldBe false
    listItems.find(_.term == "inactive").get.comment shouldBe "inactive"
    listItems.find(_.term == "aerosmith").get.synonyms shouldBe Seq("mercury")
    listItems.find(_.term == "shipping").get.tags.map(_.value)shouldBe Seq("testValue")
    listItems.find(_.term == "freezer").get.additionalTermsForSearch shouldBe Seq("frezer", "freazer", "frazer")
  }

  "Canonical spellings" should "be added" in {
    val request = FakeRequest(PUT, s"/api/v1/${core1Id.id}/spelling")
      .withHeaders("Content-type" -> "application/json")
      .withBody("""{ "term":"palm tree" }""")
    val result: Future[Result] = route(application, request).get

    status(result) shouldBe 200
    (contentAsJson(result) \ "message").as[String] shouldBe "Adding new canonical spelling 'palm tree' successful."

    db.withConnection { implicit connection =>
      val canonicalSpellings = CanonicalSpelling.loadAllForIndex(core1Id)
      canonicalSpellings.map(_.term) shouldBe Seq("freezer", "machine", "palm tree", "pants")
    }
  }

  it should "not be added if the json is not valid" in {
    val request = FakeRequest(PUT, s"/api/v1/${core1Id.id}/spelling")
      .withHeaders("Content-type" -> "application/json")
      .withBody(""" "invalid":"json" }""")
    val result: Future[Result] = route(application, request).get

    status(result) shouldBe 400
  }

  it should "not be added if the json does not contain the term" in {
    val request = FakeRequest(PUT, s"/api/v1/${core1Id.id}/spelling")
      .withHeaders("Content-type" -> "application/json")
      .withBody("""{ "not_term":"word" }""")
    val result: Future[Result] = route(application, request).get

    status(result) shouldBe 400
  }

  it should "return details of an item" in {
    val request = FakeRequest(GET, s"/api/v1/spelling/${freezer.id}")
    val result: Future[Result] = route(application, request).get

    status(result) shouldBe 200
    val item = contentAsJson(result).as[CanonicalSpellingWithAlternatives]
    item.id shouldBe freezer.id
    item.term shouldBe freezer.term
    item.alternateSpellings.map(_.term) shouldBe Seq("frazer", "freazer", "frezer")
  }

  it should "be updated" in {
    val spellings = CanonicalSpellingWithAlternatives(
      freezer.id, freezer.term,
      List(
        AlternateSpelling(canonicalSpellingId = freezer.id, term = "frezer"),
        AlternateSpelling(canonicalSpellingId = freezer.id, term = "freazer"),
        AlternateSpelling(canonicalSpellingId = freezer.id, term = "frazer"),
        AlternateSpelling(canonicalSpellingId = freezer.id, term = "freeezer")
      )
    )

    val request = FakeRequest(POST, s"/api/v1/${core1Id.id}/spelling/${freezer.id}")
      .withHeaders("Content-type" -> "application/json")
      .withBody(Json.toJson(spellings))
    val result: Future[Result] = route(application, request).get

    status(result) shouldBe 200
    (contentAsJson(result) \ "message").as[String] shouldBe "Updating canonical spelling successful."

    db.withConnection { implicit connection =>
      val canonicalSpellings = CanonicalSpelling.loadAllForIndex(core1Id)
      canonicalSpellings.map(_.term) shouldBe Seq("freezer", "machine", "pants")

      val alternativeSpellings = AlternateSpelling.loadByCanonicalId(freezer.id)
      alternativeSpellings.map(_.term) shouldBe Seq("frazer", "freazer", "freeezer", "frezer")
    }
  }

  it should "not be updated if an alternate spelling is duplicated" in {
    val spellings = CanonicalSpellingWithAlternatives(
      freezer.id, freezer.term,
      List(
        AlternateSpelling(canonicalSpellingId = freezer.id, term = "frezer"),
        AlternateSpelling(canonicalSpellingId = freezer.id, term = "freazer"),
        AlternateSpelling(canonicalSpellingId = freezer.id, term = "frazer"),
        AlternateSpelling(canonicalSpellingId = freezer.id, term = "frazer")
      )
    )

    val request = FakeRequest(POST, s"/api/v1/${core1Id.id}/spelling/${freezer.id}")
      .withHeaders("Content-type" -> "application/json")
      .withBody(Json.toJson(spellings))
    val result: Future[Result] = route(application, request).get

    status(result) shouldBe 400
    (contentAsJson(result) \ "message").as[String] should include("Duplicate alternate spelling for 'freezer'")

    db.withConnection { implicit connection =>
      val alternativeSpellings = AlternateSpelling.loadByCanonicalId(freezer.id)
      alternativeSpellings.map(_.term) shouldBe Seq("frazer", "freazer", "frezer")
    }
  }

  it should "not be updated if an alternate spelling is equal to the canonical term" in {
    val spellings = CanonicalSpellingWithAlternatives(
      freezer.id, freezer.term,
      List(
        AlternateSpelling(canonicalSpellingId = freezer.id, term = "frezer"),
        AlternateSpelling(canonicalSpellingId = freezer.id, term = "freazer"),
        AlternateSpelling(canonicalSpellingId = freezer.id, term = "frazer"),
        AlternateSpelling(canonicalSpellingId = freezer.id, term = freezer.term)
      )
    )

    val request = FakeRequest(POST, s"/api/v1/${core1Id.id}/spelling/${freezer.id}")
      .withHeaders("Content-type" -> "application/json")
      .withBody(Json.toJson(spellings))
    val result: Future[Result] = route(application, request).get

    status(result) shouldBe 400
    (contentAsJson(result) \ "message").as[String] should include("Alternate spelling is same as the canonical spelling 'freezer'")

    db.withConnection { implicit connection =>
      val alternativeSpellings = AlternateSpelling.loadByCanonicalId(freezer.id)
      alternativeSpellings.map(_.term) shouldBe Seq("frazer", "freazer", "frezer")
    }
  }

  it should "not be updated if an alternate spelling is equal to another canonical term" in {
    val spellings = CanonicalSpellingWithAlternatives(
      freezer.id, freezer.term,
      List(
        AlternateSpelling(canonicalSpellingId = freezer.id, term = "frezer"),
        AlternateSpelling(canonicalSpellingId = freezer.id, term = "freazer"),
        AlternateSpelling(canonicalSpellingId = freezer.id, term = "frazer"),
        AlternateSpelling(canonicalSpellingId = freezer.id, term = machine.term)
      )
    )

    val request = FakeRequest(POST, s"/api/v1/${core1Id.id}/spelling/${freezer.id}")
      .withHeaders("Content-type" -> "application/json")
      .withBody(Json.toJson(spellings))
    val result: Future[Result] = route(application, request).get

    status(result) shouldBe 400
    (contentAsJson(result) \ "message").as[String] should include("Alternate spelling is same as another canonical spelling")

    db.withConnection { implicit connection =>
      val alternativeSpellings = AlternateSpelling.loadByCanonicalId(freezer.id)
      alternativeSpellings.map(_.term) shouldBe Seq("frazer", "freazer", "frezer")
    }
  }

  it should "be deleted" in {
    val request = FakeRequest(DELETE, s"/api/v1/spelling/${freezer.id}")
      .withHeaders("Content-type" -> "application/json")
    val result: Future[Result] = route(application, request).get

    status(result) shouldBe 200
    (contentAsJson(result) \ "message").as[String] shouldBe "Deleting canonical spelling with alternatives successful."

    db.withConnection { implicit connection =>
      val canonicalSpellings = CanonicalSpelling.loadAllForIndex(core1Id)
      canonicalSpellings.map(_.term) shouldBe Seq("machine", "pants")
    }
  }
}