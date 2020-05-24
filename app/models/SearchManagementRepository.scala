package models

import java.io.FileInputStream
import java.time.LocalDateTime
import java.util.UUID
import java.util.Date

import anorm.SqlParser.get
import javax.inject.Inject
import anorm._
import models.FeatureToggleModel.FeatureToggleService
import models.SearchInput.ID
import play.api.db.DBApi

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
    * List all Solr Indeces the SearchInput's can be configured for
    *
    *
    * @return tbd
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
    * Lists all Search Inputs including directed Synonyms belonging to them (for a list overview)
    *
    * @return tbd
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
    * Adds new Search Input (term) to the database table. This method only focuses the term, and does not care about any synonyms.
    */
  def addNewSearchInput(solrIndexId: SolrIndexId, searchInputTerm: String, tags: Seq[InputTagId]): SearchInputId = db.withConnection { implicit connection =>
    val id = SearchInput.insert(solrIndexId, searchInputTerm).id
    if (tags.nonEmpty) {
      TagInputAssociation.updateTagsForSearchInput(id, tags)
    }
    id
  }

  def getDetailedSearchInput(searchInputId: SearchInputId): Option[SearchInputWithRules] = db.withConnection { implicit connection =>
    SearchInputWithRules.loadById(searchInputId)
  }

  def updateSearchInput(searchInput: SearchInputWithRules): Unit = db.withTransaction { implicit connection =>
    SearchInputWithRules.update(searchInput)
  }

  def deleteSearchInput(searchInputId: String): Int = db.withTransaction { implicit connection =>
    SearchInputWithRules.delete(SearchInputId(searchInputId))
  }

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

}
