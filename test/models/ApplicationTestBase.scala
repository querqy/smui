package models

import java.time.LocalDateTime

import models.rules._
import models.spellings.{AlternateSpelling, AlternateSpellingId, CanonicalSpelling, CanonicalSpellingWithAlternatives}
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

  protected lazy val application: Application = new GuiceApplicationBuilder().
    in(Mode.Test).
    configure("db.default.url" -> db.url, "db.default.driver" -> "org.h2.Driver",
      "db.default.username" -> "", "db.default.password" -> "",
      "toggle.rule-deployment.log-rule-id" -> true,
      "toggle.activate-spelling" -> true).
    build()

  protected lazy val injector: Injector = application.injector

  protected lazy val repo: SearchManagementRepository = injector.instanceOf[SearchManagementRepository]

  protected val (core1Id, core2Id) = (SolrIndexId(), SolrIndexId())

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

    val id = repo.addNewSearchInput(core1Id, "aerosmith", Seq.empty)
    val searchInput = SearchInputWithRules(id, "aerosmith", synonymRules, upDownRules, filterRules, isActive = true, comment = "")
    repo.updateSearchInput(searchInput)

    val tag = InputTag(InputTagId(), Some(core1Id), Some("testProperty"), "testValue", exported = true, predefined = false, LocalDateTime.now())
    db.withConnection { implicit connection =>
      InputTag.insert(Seq(tag): _*)
    }

    val shippingId = repo.addNewSearchInput(core1Id, "shipping", Seq(tag.id))
    val redirectRule = RedirectRule(RedirectRuleId(), "http://xyz.com/shipping", isActive = true)
    val searchInputForRedirect = SearchInputWithRules(shippingId, "shipping", redirectRules = List(redirectRule), isActive = true, comment = "", tags = Seq(tag))
    repo.updateSearchInput(searchInputForRedirect)

    val inactiveId = repo.addNewSearchInput(core1Id, "inactive", Seq.empty)
    val inactiveSearchInput = SearchInputWithRules(inactiveId, "inactive", redirectRules = List.empty, isActive = false, comment = "inactive")
    repo.updateSearchInput(inactiveSearchInput)

    Seq(id, shippingId)
  }

  var freezer: CanonicalSpelling = _
  var machine: CanonicalSpelling = _
  var pants: CanonicalSpelling = _

  protected def createTestSpellings(): Unit = {
    freezer = repo.addNewCanonicalSpelling(core1Id, "freezer")
    machine = repo.addNewCanonicalSpelling(core1Id, "machine")
    pants = repo.addNewCanonicalSpelling(core1Id, "pants")

    repo.updateSpelling(CanonicalSpellingWithAlternatives(
      freezer.id, freezer.term,
      List(
        AlternateSpelling(AlternateSpellingId(), freezer.id, "frezer"),
        AlternateSpelling(AlternateSpellingId(), freezer.id, "freazer"),
        AlternateSpelling(AlternateSpellingId(), freezer.id, "frazer")
      )
    ))

    repo.updateSpelling(CanonicalSpellingWithAlternatives(
      machine.id, machine.term,
      List(
        AlternateSpelling(AlternateSpellingId(), machine.id, "machin"),
        AlternateSpelling(AlternateSpellingId(), machine.id, "mechine")
      )
    ))

    repo.updateSpelling(CanonicalSpellingWithAlternatives(
      pants.id, pants.term,
      List(
        AlternateSpelling(AlternateSpellingId(), pants.id, "pands"),
        AlternateSpelling(AlternateSpellingId(), pants.id, "pents")
      )
    ))
  }

  def deleteAllSpellingsFromDB(solrIndexId: SolrIndexId): Unit = {
    db.withConnection { implicit connection =>
      CanonicalSpelling.loadAllForIndex(solrIndexId).foreach { canonicalSpelling =>
        CanonicalSpellingWithAlternatives.delete(canonicalSpelling.id)
      }
    }
  }

  override protected def afterAll(): Unit = {
    application.stop()
    db.shutdown()
  }

}
