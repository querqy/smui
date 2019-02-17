package models

import models.SearchManagementModel._
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
      "db.default.username" -> "", "db.default.password" -> "").
    build()

  protected lazy val injector: Injector = application.injector

  protected lazy val repo: SearchManagementRepository = injector.instanceOf[SearchManagementRepository]

  protected val core1Id = "1"

  protected def createTestCores(): Unit = {
    repo.addNewSolrIndex(SolrIndex(Some(core1Id), "core1", "First core"))
    repo.addNewSolrIndex(SolrIndex(Some("2"), "core2", "Second core"))
  }

  protected def createTestRule(): Unit = {
    val synonymRules = List (SynonymRule(None, 0, "mercury", isActive = true))
    val upDownRules = List(
      UpDownRule(None, 0, 10, "notebook", isActive = true),
      UpDownRule(None, 0, 10, "lenovo", isActive = false),
      UpDownRule(None, 1, 10, "battery", isActive = true)
    )
    val deleteRules = List(DeleteRule(None, "freddy", isActive = true))
    val filterRules = List(FilterRule(None, "zz top", isActive = true))

    val id = repo.addNewSearchInput(core1Id, "aerosmith")
    val searchInput = SearchInput(id, "aerosmith", synonymRules, upDownRules, filterRules)
    repo.updateSearchInput(searchInput)
  }

  override protected def afterAll(): Unit = {
    application.stop()
    db.shutdown()
  }

}
