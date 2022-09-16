package models

import anorm._
import models.FeatureToggleModel.FeatureToggleService
import models.eventhistory.{ActivityLog, ActivityLogEntry, InputEvent}
import models.export.Exporter
import models.input._
import models.reports.{ActivityReport, DeploymentLog, RulesReport}
import models.spellings.{CanonicalSpelling, CanonicalSpellingId, CanonicalSpellingWithAlternatives}
import models.validatedimport.{ValidatedImportData, ValidatedImportImporter}
import play.api.Logging
import play.api.db.DBApi
import play.api.libs.json.JsValue

import java.time.LocalDateTime
import java.util.{Date, UUID}
import javax.inject.Inject

// TODO Make `userInfo` mandatory (for all input/spelling and deploymentLog CRUD operations), when removing unauthorized access.
@javax.inject.Singleton
class SearchManagementRepository @Inject()(dbapi: DBApi, toggleService: FeatureToggleService)(implicit ec: DatabaseExecutionContext) extends Logging {


  private val db = dbapi.database("default")

  /**
    * List all Solr Indeces the SearchInput's can be configured for.
    */

  def listAllSolrIndexes: List[SolrIndex] = db.withConnection { implicit connection =>
    SolrIndex.listAll
  }

  def getSolrIndexName(solrIndexId: SolrIndexId): String = db.withConnection { implicit connection =>
    SolrIndex.loadNameById(solrIndexId)
  }

  def getSolrIndex(solrIndexId: SolrIndexId): SolrIndex = db.withConnection { implicit connection =>
    SolrIndex.loadById(solrIndexId)
  }

  def addNewSolrIndex(newSolrIndex: SolrIndex): SolrIndexId = db.withConnection { implicit connection =>
    SolrIndex.insert(newSolrIndex)
  }

  /**
    * We check for any InputTags, CanonicalSpellings, and SearchInputs.  We don't
    * check for the existence of any SuggestedSolrFields.
    */
  def deleteSolrIndex(solrIndexId: String): Int = db.withTransaction { implicit connection =>

    val solrIndexIdId = SolrIndexId(solrIndexId)
    val inputTags = InputTag.loadAll.filter(_.solrIndexId== Option(solrIndexIdId))
    if (inputTags.size > 0) {
      throw new Exception("Can't delete Solr Index that has " + inputTags.size + "tags existing");
    }

    val canonicalSpellings = CanonicalSpelling.loadAllForIndex(solrIndexIdId)
    if (canonicalSpellings.size > 0) {
      throw new Exception("Can't delete Solr Index that has " + canonicalSpellings.size
        + " canonical spellings existing");
    }


    val searchInputs = SearchInput.loadAllForIndex(solrIndexIdId)
    for (searchInput <- searchInputs) {
      SearchInput.delete(searchInput.id)
    }

    // TODO consider reconfirmation and deletion of history entries (if some exist) (
    //  see https://github.com/querqy/smui/issues/97)

    val id = SolrIndex.delete(solrIndexId)

    id
  }

  def listAllInputTags(): Seq[InputTag] = db.withConnection { implicit connection =>
    InputTag.loadAll()
  }

  def addNewInputTag(inputTag: InputTag) = db.withConnection { implicit connection =>
    InputTag.insert(inputTag)
  }

  /**
    * Lists all Search Inputs including directed Synonyms belonging to them (for a list overview).
    */

  def listAllSearchInputsInclDirectedSynonyms(solrIndexId: SolrIndexId): List[SearchInputWithRules] = {
    db.withConnection { implicit connection =>
      SearchInputWithRules.loadWithUndirectedSynonymsAndTagsForSolrIndexId(solrIndexId)
    }
  }

  def loadAllInputIdsForSolrIndex(solrIndexId: SolrIndexId): Seq[SearchInputId] = {
    db.withConnection { implicit connection =>
      SearchInput.loadAllIdsForIndex(solrIndexId)
    }
  }

  /**
   * Canonical spellings and alternative spellings
   *
   */

  def addNewCanonicalSpelling(solrIndexId: SolrIndexId, term: String, userInfo: Option[String]): CanonicalSpelling =
    db.withConnection { implicit connection =>
      val spelling = CanonicalSpelling.insert(solrIndexId, term)

      // add CREATED event for spelling
      if (toggleService.getToggleActivateEventHistory) {
        InputEvent.createForSpelling(
          spelling.id,
          userInfo,
          false
        )
      }

      spelling
    }

  def getDetailedSpelling(canonicalSpellingId: String): Option[CanonicalSpellingWithAlternatives] =
    db.withConnection { implicit connection =>
      CanonicalSpellingWithAlternatives.loadById(CanonicalSpellingId(canonicalSpellingId))
    }

  def updateSpelling(spelling: CanonicalSpellingWithAlternatives, userInfo: Option[String]): Unit =
    db.withTransaction { implicit connection =>
      CanonicalSpellingWithAlternatives.update(spelling)

      // add UPDATED event for spelling and associated alternatives
      if (toggleService.getToggleActivateEventHistory) {
        InputEvent.updateForSpelling(
          spelling.id,
          userInfo
        )
      }
    }

  def listAllSpellings(solrIndexId: SolrIndexId): List[CanonicalSpelling] =
    db.withConnection { implicit connection =>
      CanonicalSpelling.loadAllForIndex(solrIndexId)
    }

  def listAllSpellingsWithAlternatives(solrIndexId: SolrIndexId): List[CanonicalSpellingWithAlternatives] =
    db.withConnection { implicit connection =>
      CanonicalSpellingWithAlternatives.loadAllForIndex(solrIndexId)
    }

  def deleteSpelling(canonicalSpellingId: String, userInfo: Option[String]): Int =
    db.withTransaction { implicit connection =>
      val id = CanonicalSpellingId(canonicalSpellingId)
      val count = CanonicalSpellingWithAlternatives.delete(id)

      // add DELETED event for spelling and associated alternatives
      if (toggleService.getToggleActivateEventHistory) {
        InputEvent.deleteForSpelling(
          id,
          userInfo
        )
      }

      count
    }

  /**
    * Search input and rules.
    */

  /**
    * Adds new Search Input (term) to the database table. This method only focuses the term, and does not care about any synonyms.
    */
  def addNewSearchInput(solrIndexId: SolrIndexId, searchInputTerm: String, tags: Seq[InputTagId], userInfo: Option[String]): SearchInputId = db.withConnection { implicit connection =>

    // add search input
    val id = SearchInput.insert(solrIndexId, searchInputTerm).id
    if (tags.nonEmpty) {
      TagInputAssociation.updateTagsForSearchInput(id, tags)
    }

    // add CREATED event for search input (maybe containing tags)
    if (toggleService.getToggleActivateEventHistory) {
      InputEvent.createForSearchInput(
        id,
        userInfo,
        false
      )
    }

    id
  }

  def getDetailedSearchInput(searchInputId: SearchInputId): Option[SearchInputWithRules] = db.withConnection { implicit connection =>
    SearchInputWithRules.loadById(searchInputId)
  }

  def updateSearchInput(searchInput: SearchInputWithRules, userInfo: Option[String]): Unit = db.withTransaction { implicit connection =>
    SearchInputWithRules.update(searchInput)

    // add UPDATED event for search input and rules
    if (toggleService.getToggleActivateEventHistory) {
      InputEvent.updateForSearchInput(
        searchInput.id,
        userInfo
      )
    }
  }

  def deleteSearchInput(searchInputId: String, userInfo: Option[String]): Int = db.withTransaction { implicit connection =>
    val id = SearchInputWithRules.delete(SearchInputId(searchInputId))

    // add DELETED event for search input and rules
    if (toggleService.getToggleActivateEventHistory) {
      InputEvent.deleteForSearchInput(
        SearchInputId(searchInputId),
        userInfo
      )
    }

    id
  }

  /**
    * SMUI helper (like suggested Solr fields, deployment log)
    */

  def listAllSuggestedSolrFields(solrIndexId: String): List[SuggestedSolrField] = db.withConnection { implicit connection =>
    SuggestedSolrField.listAll(SolrIndexId(solrIndexId))
  }

  def addNewSuggestedSolrField(solrIndexId: SolrIndexId, suggestedSolrFieldName: String): SuggestedSolrField = db.withConnection { implicit connection =>
    SuggestedSolrField.insert(solrIndexId, suggestedSolrFieldName)
  }
  def deleteSuggestedSolrField(suggestedSolrFieldId: SuggestedSolrFieldId): Int = db.withTransaction { implicit connection =>
    val id = SuggestedSolrField.delete(suggestedSolrFieldId);

    id
  }

  def addNewDeploymentLogOk(solrIndexId: String, targetPlatform: String): Boolean = db.withConnection { implicit connection =>
    SQL("insert into deployment_log(id, solr_index_id, target_platform, last_update, result) values ({id}, {solr_index_id}, {target_platform}, {last_update}, {result})")
      .on(
        'id -> UUID.randomUUID().toString,
        'solr_index_id -> solrIndexId,
        'target_platform -> targetPlatform,
        'last_update -> new Date(),
        'result -> 0
      )
      .execute()
  }

  def lastDeploymentLogDetail(solrIndexId: String, targetPlatform: String): Option[DeploymentLog] = db.withConnection {
    implicit connection => {
      DeploymentLog.loadForSolrIndexIdAndPlatform(solrIndexId, targetPlatform)
    }
  }

  /**
    * Get the activity log (based on event history).
    */

  def getInputRuleActivityLog(inputId: String): ActivityLog = db.withConnection {
    implicit connection => {

      if (toggleService.getToggleActivateEventHistory) {

        // TODO maybe add defaultUsername in ActivityLog already?
        val defaultUsername = if (toggleService.getToggleDefaultDisplayUsername.isEmpty) None else Some(toggleService.getToggleDefaultDisplayUsername)

        ActivityLog(
          items = ActivityLog.loadForId(inputId).items
            .map(logEntry =>
              ActivityLogEntry(
                formattedDateTime = logEntry.formattedDateTime,
                userInfo = (if (logEntry.userInfo.isEmpty) defaultUsername else logEntry.userInfo),
                diffSummary = logEntry.diffSummary
              )
            )
        )
      } else {

        ActivityLog(items = Nil)
      }
    }
  }

  /**
    * Reports
    */

  def getRulesReport(solrIndexId: SolrIndexId): RulesReport = db.withConnection {
    implicit connection => {
      RulesReport.loadForSolrIndexId(solrIndexId)
    }
  }

  def getActivityReport(solrIndexId: SolrIndexId, dateFrom: LocalDateTime, dateTo: LocalDateTime): ActivityReport = db.withConnection {
    implicit connection => {

      // TODO maybe add defaultUsername in ActivityLog already?
      val defaultUsername = if (toggleService.getToggleDefaultDisplayUsername.isEmpty) None else Some(toggleService.getToggleDefaultDisplayUsername)

      // TODO ensure dateFrom/To span whole days (00:00 to 23:59)
      ActivityReport(
        items = ActivityReport.reportForSolrIndexIdInPeriod(solrIndexId, dateFrom, dateTo).items
          .map(item => item.copy(
            user = (if (item.user.isEmpty) defaultUsername else item.user),
          ))
      )
    }
  }

  def getDatabaseJsonWithId(id: String): JsValue = db.withConnection {
    implicit connection => {
      logger.debug("In SearchManagementRepository:getDatabaseJsonWithId():1")
      val exporter : Exporter = new Exporter(dbapi, toggleService)
      exporter.getDatabaseJsonWithId(id)
    }
  }

  def doImport(validatedImport: ValidatedImportData): String = db.withTransaction { implicit connection =>
    var aString : String = "At SearchManagementRepository:doImport():1"
    logger.debug(aString)

    val importer = new ValidatedImportImporter(validatedImport, dbapi, toggleService)
    importer.performImport()

    aString = "At SearchManagementRepository:doImport():2"
    logger.debug(aString)
    aString
  }

}
