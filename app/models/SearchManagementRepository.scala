package models

import java.io.FileInputStream
import java.time.LocalDateTime
import java.util.{Date, UUID}

import javax.inject.Inject
import anorm.SqlParser.get
import anorm._
import play.api.db.DBApi
import models.FeatureToggleModel.FeatureToggleService
import models.input.{InputTag, InputTagId, PredefinedTag, SearchInput, SearchInputId, SearchInputWithRules, TagInputAssociation}
import models.spellings.{CanonicalSpelling, CanonicalSpellingId, CanonicalSpellingWithAlternatives}
import models.eventhistory.{ActivityLog, ActivityLogEntry, InputEvent}
import models.reports.RulesReport

@javax.inject.Singleton
class SearchManagementRepository @Inject()(dbapi: DBApi, toggleService: FeatureToggleService)(implicit ec: DatabaseExecutionContext) {

  private val db = dbapi.database("default")

  // On startup, always sync predefined tags with the DB
  syncPredefinedTagsWithDB()

  private def syncPredefinedTagsWithDB(): Unit = {
    db.withTransaction { implicit connection =>
      if (toggleService.isRuleTaggingActive) {
        for (fileName <- toggleService.predefinedTagsFileName) {
          val tags = PredefinedTag.fromStream(new FileInputStream(fileName))
          PredefinedTag.updateInDB(tags)
        }
      }
    }
  }

  /**
    * List all Solr Indeces the SearchInput's can be configured for.
    */

  def listAllSolrIndexes: List[SolrIndex] = db.withConnection { implicit connection =>
    SolrIndex.listAll
  }

  def getSolrIndexName(solrIndexId: SolrIndexId): String = db.withConnection { implicit connection =>
    SolrIndex.loadNameById(solrIndexId)
  }

  def addNewSolrIndex(newSolrIndex: SolrIndex): SolrIndexId = db.withConnection { implicit connection =>
    SolrIndex.insert(newSolrIndex)
  }

  def listAllInputTags(): Seq[InputTag] = db.withConnection { implicit connection =>
    InputTag.loadAll()
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
    */

  def addNewCanonicalSpelling(solrIndexId: SolrIndexId, term: String): CanonicalSpelling =
    db.withConnection { implicit connection =>
      val spelling = CanonicalSpelling.insert(solrIndexId, term)

      // add CREATED event for spelling
      if (toggleService.getToggleActivateEventHistory) {
        InputEvent.createForSpelling(
          CanonicalSpellingWithAlternatives.loadById(spelling.id).get,
          None, // TODO userInfo not being logged so far
          false
        )
      }

      spelling
    }

  def getDetailedSpelling(canonicalSpellingId: String): Option[CanonicalSpellingWithAlternatives] =
    db.withConnection { implicit connection =>
      CanonicalSpellingWithAlternatives.loadById(CanonicalSpellingId(canonicalSpellingId))
    }

  def updateSpelling(spelling: CanonicalSpellingWithAlternatives): Unit =
    db.withTransaction { implicit connection =>
      CanonicalSpellingWithAlternatives.update(spelling)

      // add UPDATED event for spelling and associated alternatives
      if (toggleService.getToggleActivateEventHistory) {
        InputEvent.updateForSpelling(
          CanonicalSpellingWithAlternatives.loadById(spelling.id).get,
          None // TODO userInfo not being logged so far
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

  def deleteSpelling(canonicalSpellingId: String): Int =
    db.withTransaction { implicit connection =>
      val id = CanonicalSpellingId(canonicalSpellingId)
      val count = CanonicalSpellingWithAlternatives.delete(id)

      // add DELETED event for spelling and associated alternatives
      if (toggleService.getToggleActivateEventHistory) {
        InputEvent.deleteForSpelling(
          id,
          None // TODO userInfo not being logged so far
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
  def addNewSearchInput(solrIndexId: SolrIndexId, searchInputTerm: String, tags: Seq[InputTagId]): SearchInputId = db.withConnection { implicit connection =>

    // add search input
    val id = SearchInput.insert(solrIndexId, searchInputTerm).id
    if (tags.nonEmpty) {
      TagInputAssociation.updateTagsForSearchInput(id, tags)
    }

    // add CREATED event for search input (maybe containing tags)
    if (toggleService.getToggleActivateEventHistory) {
      InputEvent.createForSearchInput(
        SearchInputWithRules.loadById(id).get,
        None, // TODO userInfo not being logged so far
        false
      )
    }

    id
  }

  def getDetailedSearchInput(searchInputId: SearchInputId): Option[SearchInputWithRules] = db.withConnection { implicit connection =>
    SearchInputWithRules.loadById(searchInputId)
  }

  def updateSearchInput(searchInput: SearchInputWithRules): Unit = db.withTransaction { implicit connection =>
    SearchInputWithRules.update(searchInput)

    // add UPDATED event for search input and rules
    if (toggleService.getToggleActivateEventHistory) {
      InputEvent.updateForSearchInput(
        SearchInputWithRules.loadById(searchInput.id).get,
        None // TODO userInfo not being logged so far
      )
    }
  }

  def deleteSearchInput(searchInputId: String): Int = db.withTransaction { implicit connection =>
    val id = SearchInputWithRules.delete(SearchInputId(searchInputId))

    // add DELETED event for search input and rules
    if (toggleService.getToggleActivateEventHistory) {
      InputEvent.deleteForSearchInput(
        SearchInputId(searchInputId),
        None // TODO userInfo not being logged so far
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

  case class DeploymentLogDetail(id: String, lastUpdate: LocalDateTime, result: Int)

  val sqlParserDeploymentLogDetail: RowParser[DeploymentLogDetail] = {
    get[String](s"deployment_log.id") ~
      get[LocalDateTime](s"deployment_log.last_update") ~
      get[Int](s"deployment_log.result") map { case id ~ lastUpdate ~ result =>
      DeploymentLogDetail(id, lastUpdate, result)
    }
  }

  def lastDeploymentLogDetail(solrIndexId: String, targetPlatform: String): Option[DeploymentLogDetail] = db.withConnection {
    implicit connection => {
      SQL"select * from deployment_log where solr_index_id = $solrIndexId and target_platform = $targetPlatform order by last_update desc".as(sqlParserDeploymentLogDetail.*).headOption
    }
  }

  /**
    * Get the activity log (based on event history).
    */

  def getInputRuleActivityLog(inputId: String): ActivityLog = db.withConnection {
    implicit connection => {

      if (toggleService.getToggleActivateEventHistory) {

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

}
