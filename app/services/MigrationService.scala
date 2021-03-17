package services

import javax.inject.Inject
import play.api.Logging
import play.api.db.DBApi
import models.DatabaseExecutionContext
import models.FeatureToggleModel.FeatureToggleService
import models.eventhistory.InputEvent
import play.api.db.evolutions.ApplicationEvolutions
import play.api.Configuration

@javax.inject.Singleton
class MigrationService @Inject()(appConfig: Configuration, dbapi: DBApi, toggleService: FeatureToggleService, applicationEvolutions: ApplicationEvolutions)(implicit ec: DatabaseExecutionContext) extends Logging {

  val DATABASE_CONFIG_TO_USE = appConfig.get[String]("smui.database.config")
  private val db = dbapi.database(DATABASE_CONFIG_TO_USE)

  // inform, if play evolutions are up-to-date BEFORE any custom migrations happen
  // (see https://discuss.lightbend.com/t/startup-timing-issues-w-database-initialization-and-evolutions/2458)
  val evolutionsUpToDate = applicationEvolutions.upToDate
  logger.info(s"In MigrationService :: ensure applicationEvolutions are up-to-date: ${evolutionsUpToDate}")

  /**
    * Handle the migration of pre v3.8 SMUI instances by persisting virtual create events for SearchInput and CanonicalSpelling on SMUI startup.
    */
  private def virtuallyCreateEventsPreVersion38() = {
    db.withTransaction { implicit connection =>

      if (toggleService.getToggleActivateEventHistory) {

        logger.info("In virtuallyCreateEventsPreVersion38 (event history switched ON)")

        // specify the custom migration to create event data for SMUI versions < 3.8
        SmuiMigrationLock.executeOnce("pre_v3.8", () => {

          // logic for the custom migration
          // fyi: no further version check needed. gaps between input entities and the event history can be filled anyway!!

          // determine missing events
          val missingSearchInputIds = InputEvent.searchInputIdsWithoutEvent()
          val missingSpellingIds = InputEvent.spellingIdsWithoutEvent()
          logger.info("virtuallyCreateEventsPreVersion38 :: count missingSearchInputIds = " + missingSearchInputIds.length)
          logger.info("virtuallyCreateEventsPreVersion38 :: count missingSpellingIds = " + missingSpellingIds.length)

          // create virtual CREATED events (for inputs and spellings)
          missingSearchInputIds.map(id => {
            InputEvent.createForSearchInput(
              id,
              None,
              true
            )
          })
          missingSpellingIds.map(id => {
            InputEvent.createForSpelling(
              id,
              None,
              true
            )
          })

        })
      }
    }
  }

  // protect all migrations within a try-catch. In case, migrations fail, try to bootstrap SMUI anyway!!
  // TODO Compile Time Dependency Injection & Play evolutions seem to be entangled unfavorably. Therefore, during development (DEV environment), it might be necessary to reload / restart the application (happened, when testing the migration towards v3.11.9).
  try {

    virtuallyCreateEventsPreVersion38()

  } catch {
    case e: Throwable => {
      logger.error(s"In MigrationService :: exception during migration, e = ${e.toString}")
    }
  }

}
