package services

import javax.inject.Inject
import play.api.Logging
import play.api.db.DBApi
import models.{DatabaseExecutionContext, SearchManagementRepository}
import models.FeatureToggleModel.FeatureToggleService
import models.eventhistory.InputEvent
import models.input.PredefinedTag
import play.api.db.evolutions.ApplicationEvolutions
import java.io.FileInputStream

@javax.inject.Singleton
class MigrationService @Inject()(dbapi: DBApi, toggleService: FeatureToggleService, searchManagementRepository: SearchManagementRepository, applicationEvolutions: ApplicationEvolutions)(implicit ec: DatabaseExecutionContext) extends Logging {

  private val db = dbapi.database("default")

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

  private def syncPredefinedTagsWithDB() = {
    db.withTransaction { implicit connection =>


      if (toggleService.isRuleTaggingActive) {
        // We can only sync rules if we are up to date on our evolutions.
        if (evolutionsUpToDate) {
          // On startup, always sync predefined tags with the DB
          logger.info(("Database evolutions are up to date, now syncing any predefined tags"))
          val tags = PredefinedTag.fromStream(new FileInputStream(toggleService.predefinedTagsFileName.get))
          PredefinedTag.updateInDB(tags)
        }
        else {
          logger.error("Database evolutions are not up to date, so not syncing any predefined tags with database")
        }
      }
    }
  }

  private def logCountEventsWithoutProperUserInfoPreVersion314() = {
    db.withTransaction { implicit connection =>

      val countEvents = InputEvent.countEventsWithoutProperUserInfo
      if (countEvents > 0) {
        logger.warn(s"You have ${countEvents} history events without userInfo in your database. Support for empty userInfo entries have been removed as of v3.14 of SMUI (see https://github.com/querqy/smui/pull/83#issuecomment-1023284550). Please migrate existing event data.")
      }

    }
  }

  // protect all migrations within a try-catch. In case, migrations fail, try to bootstrap SMUI anyway!!
  // TODO Compile Time Dependency Injection & Play evolutions seem to be entangled unfavorably. Therefore, during development (DEV environment), it might be necessary to reload / restart the application (happened, when testing the migration towards v3.11.9).
  try {

    virtuallyCreateEventsPreVersion38()
    syncPredefinedTagsWithDB()
    logCountEventsWithoutProperUserInfoPreVersion314()

  } catch {
    case e: Throwable => {
      logger.error(s"In MigrationService :: exception during migration, e = ${e.toString}")
    }
  }

}
