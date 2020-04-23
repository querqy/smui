package models

import models.rules._
import org.scalatest.{BeforeAndAfterAll, Suite}
import play.api.{Application, Mode}
import play.api.db.{Database, Databases}
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder

/**
  * Base class for tests that start an application with an in-memory database.
  */
trait ApplicationTestBase extends BeforeAndAfterAll { self: Suite =>

  protected lazy val db: Database = Databases.inMemory()

  // Use logging settings from logback-test.xml for test application
  System.setProperty("logger.resource", "logback-test.xml")

  protected lazy val application: Application = new GuiceApplicationBuilder().
    in(Mode.Test).
    configure("db.default.url" -> db.url, "db.default.driver" -> "org.h2.Driver",
      "db.default.username" -> "", "db.default.password" -> "", "toggle.rule-deployment.log-rule-id" -> true).
    build()

  protected lazy val injector: Injector = application.injector

  protected lazy val repo: SearchManagementRepository = injector.instanceOf[SearchManagementRepository]

  protected val (core1Id, core2Id) = (SolrIndexId(), SolrIndexId())

  protected def createTestCores(): Unit = {
    repo.addNewSolrIndex(SolrIndex(core1Id, "core1", "First core"))
    repo.addNewSolrIndex(SolrIndex(core2Id, "core2", "Second core"))
  }

  protected def createTestRule(): Seq[SearchInputId] = {
    val synonymRules = List (SynonymRule(SynonymRuleId(), 0, "mercury", isActive = true))
    val upDownRules = List(
      UpDownRule(UpDownRuleId(), UpDownRule.TYPE_UP, 10, "notebook", isActive = true),
      UpDownRule(UpDownRuleId(), UpDownRule.TYPE_UP, 10, "lenovo", isActive = false),
      UpDownRule(UpDownRuleId(), UpDownRule.TYPE_DOWN, 10, "battery", isActive = true)
    )
    val deleteRules = List(DeleteRule(DeleteRuleId(), "freddy", isActive = true))
    val filterRules = List(FilterRule(FilterRuleId(), "zz top", isActive = true))

    val id = repo.addNewSearchInput(core1Id, "aerosmith", Seq.empty)
    val searchInput = SearchInputWithRules(id, "aerosmith", synonymRules, upDownRules, filterRules)
    repo.updateSearchInput(searchInput)

    val shippingId = repo.addNewSearchInput(core1Id, "shipping", Seq.empty)
    val redirectRule = RedirectRule(RedirectRuleId(), "http://xyz.com/shipping", isActive = true)
    val searchInputForRedirect = SearchInputWithRules(shippingId, "shipping", redirectRules = List(redirectRule))
    repo.updateSearchInput(searchInputForRedirect)

    Seq(id, shippingId)
  }

  override protected def afterAll(): Unit = {
    application.stop()
    db.shutdown()
  }

}
