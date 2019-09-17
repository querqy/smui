package models

import java.io.FileInputStream
import java.util.UUID
import java.util.Date

import javax.inject.Inject
import anorm.SqlParser._
import anorm._
import models.FeatureToggleModel.FeatureToggleService
import play.api.db.DBApi
import models.rules._

@javax.inject.Singleton
class SearchManagementRepository @Inject()(dbapi: DBApi, toggleService: FeatureToggleService)(implicit ec: DatabaseExecutionContext) {

  private val db = dbapi.database("default")

  // On startup, always sync predefined tags with the DB
  syncPredefinedTagsWithDB()

  /**
    * Parse a DeleteRule from a ResultSet
    */
  private[models] val simpleSuggestedSolrField = {
    get[SuggestedSolrFieldId]("suggested_solr_field.id") ~
      get[String]("suggested_solr_field.name") map {
      case id~name => SuggestedSolrField(id, name)
    }
  }

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
      val inputs = SearchInput.loadAllForIndex(solrIndexId)
      val rules = SynonymRule.loadUndirectedBySearchInputIds(inputs.map(_.id))
      val tags = TagInputAssociation.loadTagsBySearchInputIds(inputs.map(_.id))

      inputs.map { input =>
        SearchInputWithRules(input.id, input.term,
          synonymRules = rules.getOrElse(input.id, Nil).toList,
          tags = tags.getOrElse(input.id, Seq.empty))
      }
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
  def addNewSearchInput(solrIndexId: SolrIndexId, searchInputTerm: String): SearchInputId = db.withConnection { implicit connection =>
    SearchInput.insert(solrIndexId, searchInputTerm).id
  }

  def getDetailedSearchInput(searchInputId: SearchInputId): Option[SearchInputWithRules] = db.withConnection { implicit connection =>
    SearchInputWithRules.loadById(searchInputId)
  }

  def updateSearchInput(searchInput: SearchInputWithRules): Unit = db.withTransaction { implicit connection =>
    SearchInputWithRules.update(searchInput)
  }

  /**
    * tbd
    *
    * @param searchInputId tbd
    * @return tbd
    */
  def deleteSearchInput(searchInputId: String): Int = db.withTransaction { implicit connection =>
    SearchInputWithRules.delete(SearchInputId(searchInputId))
  }

  def listAllSuggestedSolrFields(solrIndexId: String): List[SuggestedSolrField] = db.withConnection { implicit connection =>
    SQL(
      "select * from suggested_solr_field " +
      "where solr_index_id = {solr_index_id} " +
      "order by name asc"
    )
    .on(
      'solr_index_id -> solrIndexId
    )
    .as(simpleSuggestedSolrField.*)
  }

  def addNewSuggestedSolrField(solrIndexId: String, suggestedSolrFieldName: String): Option[String] = db.withConnection { implicit connection =>
    val newId = UUID.randomUUID().toString
    SQL("insert into suggested_solr_field(id, name, solr_index_id, last_update) values ({id}, {name}, {solr_index_id}, {last_update})")
      .on(
        'id -> newId,
        'name -> suggestedSolrFieldName,
        'solr_index_id -> solrIndexId,
        'last_update -> new Date()
      )
      .execute()
    Some(newId)
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

}
