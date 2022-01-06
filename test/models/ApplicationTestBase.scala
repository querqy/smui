package models

import java.time.LocalDateTime

import models.input.{SearchInputId, SearchInputWithRules, InputTag, InputTagId}
import models.rules._
import models.spellings.{AlternativeSpelling, AlternativeSpellingId, CanonicalSpelling, CanonicalSpellingId, CanonicalSpellingWithAlternatives}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import play.api.db.{Database, Databases}
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Mode}

/**
 * Base class for tests that start an application with an in-memory database.
 */
trait ApplicationTestBase extends BeforeAndAfterAll with BeforeAndAfterEach {
  self: Suite =>

  protected lazy val db: Database = Databases.inMemory()

  // Use logging settings from logback-test.xml for test application
  System.setProperty("logger.resource", "logback-test.xml")

  // override this val to add spec specific application config
  protected lazy val additionalAppConfig: Seq[(String, Any)] = Nil

  protected lazy val activateSpelling = true

  // TestBase wide application config
  protected lazy val baseAppConfig: Seq[(String, Any)] = Seq(
      "db.default.url" -> db.url,
      "db.default.driver" -> "org.h2.Driver",
      "db.default.username" -> "",
      "db.default.password" -> "",
      "toggle.rule-deployment.log-rule-id" -> true,
      "toggle.activate-spelling" -> activateSpelling
    ) ++
    additionalAppConfig

  protected lazy val application: Application = new GuiceApplicationBuilder().
    in(Mode.Test).
    configure(baseAppConfig: _*).
    build()

  protected lazy val injector: Injector = application.injector

  protected lazy val repo: SearchManagementRepository = injector.instanceOf[SearchManagementRepository]

  protected val (core1Id, core2Id) = (SolrIndexId(), SolrIndexId())

  // set to e.g. 2000, to receive a definite order of events (especially for EventHistorySpec)
  protected val MILLIS_BETWEEN_CHANGE_EVENTS = 0

  protected def createTestCores(): Unit = {
    repo.addNewSolrIndex(SolrIndex(core1Id, "core1", "First core"))
    repo.addNewSolrIndex(SolrIndex(core2Id, "core2", "Second core"))
  }

  protected def createTestRule(): Seq[SearchInputId] = {
    val synonymRules = List(
      SynonymRule(SynonymRuleId(), 0, "mercury", isActive = true),
      SynonymRule(SynonymRuleId(), 1, "directed", isActive = true),
      SynonymRule(SynonymRuleId(), 0, "inactive", isActive = false)
    )
    val upDownRules = List(
      UpDownRule(UpDownRuleId(), UpDownRule.TYPE_UP, 10, "notebook", isActive = true),
      UpDownRule(UpDownRuleId(), UpDownRule.TYPE_UP, 10, "lenovo", isActive = false),
      UpDownRule(UpDownRuleId(), UpDownRule.TYPE_DOWN, 10, "battery", isActive = true)
    )
    val deleteRules = List(DeleteRule(DeleteRuleId(), "freddy", isActive = true))
    val filterRules = List(FilterRule(FilterRuleId(), "zz top", isActive = true))

    val aerosmithId = repo.addNewSearchInput(core1Id, "aerosmith", Seq.empty, None)
    Thread.sleep(MILLIS_BETWEEN_CHANGE_EVENTS)
    val searchInput = SearchInputWithRules(
      aerosmithId,
      "aerosmith",
      synonymRules,
      upDownRules,
      filterRules,
      isActive = true,
      comment = ""
    )
    repo.updateSearchInput(searchInput, None)
    Thread.sleep(MILLIS_BETWEEN_CHANGE_EVENTS)

    val tag = createTestTag("testProperty", "testValue", core1Id)

    val shippingId = repo.addNewSearchInput(core1Id, "shipping", Seq(tag.id), None)
    Thread.sleep(MILLIS_BETWEEN_CHANGE_EVENTS)
    val redirectRule = RedirectRule(RedirectRuleId(), "http://xyz.com/shipping", isActive = true)
    val searchInputForRedirect = SearchInputWithRules(
      shippingId,
      "shipping",
      redirectRules = List(redirectRule),
      isActive = true,
      comment = "",
      tags = Seq(tag)
    )
    repo.updateSearchInput(searchInputForRedirect, None)
    Thread.sleep(MILLIS_BETWEEN_CHANGE_EVENTS)

    val inactiveId = repo.addNewSearchInput(core1Id, "inactive", Seq.empty, None)
    Thread.sleep(MILLIS_BETWEEN_CHANGE_EVENTS)
    val inactiveSearchInput = SearchInputWithRules(
      inactiveId,
      "inactive",
      redirectRules = List.empty,
      isActive = false,
      comment = "inactive"
    )
    repo.updateSearchInput(inactiveSearchInput, None)
    Thread.sleep(MILLIS_BETWEEN_CHANGE_EVENTS)

    Seq(aerosmithId, shippingId)
  }

  protected def createTestTag(name: String, value: String, coreId: SolrIndexId): InputTag = {
    val tag = InputTag(InputTagId(), Some(core1Id), Some(name), value, exported = true, predefined = false, LocalDateTime.now())
    db.withConnection { implicit connection =>
      InputTag.insert(Seq(tag): _*)
    }
    return tag
  }

  var freezer: CanonicalSpelling = _
  var machine: CanonicalSpelling = _
  var pants: CanonicalSpelling = _

  protected def createTestSpellings(): Seq[CanonicalSpellingId] = {
    freezer = repo.addNewCanonicalSpelling(core1Id, "freezer", None)
    Thread.sleep(MILLIS_BETWEEN_CHANGE_EVENTS)
    machine = repo.addNewCanonicalSpelling(core1Id, "machine", None)
    Thread.sleep(MILLIS_BETWEEN_CHANGE_EVENTS)
    pants = repo.addNewCanonicalSpelling(core1Id, "pants", None)
    Thread.sleep(MILLIS_BETWEEN_CHANGE_EVENTS)

    repo.updateSpelling(CanonicalSpellingWithAlternatives(
      freezer.id,
      freezer.term,
      freezer.isActive,
      freezer.comment,
      alternativeSpellings = List(
        AlternativeSpelling(AlternativeSpellingId(), freezer.id, "frezer", true),
        AlternativeSpelling(AlternativeSpellingId(), freezer.id, "freazer", true),
        AlternativeSpelling(AlternativeSpellingId(), freezer.id, "frazer", true)
      )
    ), None)
    Thread.sleep(MILLIS_BETWEEN_CHANGE_EVENTS)

    repo.updateSpelling(CanonicalSpellingWithAlternatives(
      machine.id,
      machine.term,
      machine.isActive,
      machine.comment,
      alternativeSpellings = List(
        AlternativeSpelling(AlternativeSpellingId(), machine.id, "machin", false),
        AlternativeSpelling(AlternativeSpellingId(), machine.id, "mechine", true)
      )
    ), None)
    Thread.sleep(MILLIS_BETWEEN_CHANGE_EVENTS)

    repo.updateSpelling(CanonicalSpellingWithAlternatives(
      pants.id,
      pants.term,
      isActive = false,
      "This is a comment",
      List(
        AlternativeSpelling(AlternativeSpellingId(), pants.id, "pands", true),
        AlternativeSpelling(AlternativeSpellingId(), pants.id, "pents", true)
      )
    ), None)
    Thread.sleep(MILLIS_BETWEEN_CHANGE_EVENTS)

    Seq(freezer.id, machine.id, pants.id)
  }

  def deleteAllSpellingsFromDB(solrIndexId: SolrIndexId): Unit = {
    db.withConnection { implicit connection =>
      CanonicalSpelling.loadAllForIndex(solrIndexId).foreach { canonicalSpelling =>
        CanonicalSpellingWithAlternatives.delete(canonicalSpelling.id)
        Thread.sleep(MILLIS_BETWEEN_CHANGE_EVENTS)
      }
    }
  }

  def deleteAllSearchInputsFromDb(solrIndexId: SolrIndexId): Unit = {
    db.withConnection { implicit connection =>
      SearchInputWithRules.loadWithUndirectedSynonymsAndTagsForSolrIndexId(solrIndexId).foreach { searchInput =>
        SearchInputWithRules.delete(searchInput.id)
        Thread.sleep(MILLIS_BETWEEN_CHANGE_EVENTS)
      }
    }
  }

  override protected def afterAll(): Unit = {
    application.stop()
    db.shutdown()
  }

}
