package models.eventhistory

import javax.inject.Inject
import java.sql.Connection

import play.api.db.DBApi
import play.api.Logging
import models.DatabaseExecutionContext
import models.FeatureToggleModel.FeatureToggleService
import models.input.SearchInputWithRules
import models.spellings.CanonicalSpellingWithAlternatives

@javax.inject.Singleton
class MigrationService @Inject()(dbapi: DBApi, toggleService: FeatureToggleService)(implicit ec: DatabaseExecutionContext) extends Logging {

  private val db = dbapi.database("default")

  /**
    * Handle the migration of pre v3.8 SMUI instances by persisting virtual create events for SearchInput and CanonicalSpelling on SMUI startup.
    */
  private def virtuallyCreateEventsPreVersion38() = {
    db.withTransaction { implicit connection =>

      if (toggleService.getToggleActivateEventHistory) {

        // fyi: no version check needed, as gaps between the entities (SearchInput and CanonicalSpelling) and the event history can be filled anyway

        logger.info("In virtuallyCreateEventsPreVersion38")

        // determine missing events

        val missingSearchInputIds = InputEvent.searchInputIdsWithoutEvent()
        val missingSpellingIds = InputEvent.spellingIdsWithoutEvent()

        logger.info(":: count missingSearchInputIds = " + missingSearchInputIds.length)
        logger.info(":: count missingSpellingIds = " + missingSpellingIds.length)

        // create virtual CREATED events

        missingSearchInputIds.map(id => {
          InputEvent.createForSearchInput(
            SearchInputWithRules.loadById(id).get,
            None,
            true
          )
        })

        missingSpellingIds.map(id => {
          InputEvent.createForSpelling(
            CanonicalSpellingWithAlternatives.loadById(id).get,
            None,
            true
          )
        })
      }
    }
  }

  virtuallyCreateEventsPreVersion38()

}
