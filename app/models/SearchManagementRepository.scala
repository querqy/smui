package models

import javax.inject.Inject

import anorm.SqlParser._
import anorm._
import play.api.db.DBApi

import scala.concurrent.Future
import models.SearchManagementModel._

import scala.collection.mutable.ListBuffer

@javax.inject.Singleton
class SearchManagementRepository @Inject()(dbapi: DBApi)(implicit ec: DatabaseExecutionContext) {

  private val db = dbapi.database("default")

  /**
    * Parse a SearchInput from a ResultSet
    */
  private[models] val simpleSolrIndex = {
    get[Option[Long]]("solr_index.id") ~
      get[String]("solr_index.name") ~
      get[String]("solr_index.description") map {
      case id~name~description => SolrIndex(id, name, description)
    }
  }

  /**
    * Parse a SearchInput from a ResultSet
    */
  private[models] val simpleSearchInput = {
    get[Option[Long]]("search_input.id") ~
      get[String]("search_input.term") map {
      case id~term => SearchInput(id, term, List[SynonymRule](), List[UpDownRule](), List[FilterRule](), List[DeleteRule]())
    }
  }

  /**
    * Parse a SynonymRule from a ResultSet
    */
  private[models] val simpleSynonymRule = {
    get[Option[Long]]("synonym_rule.id") ~
      get[Int]("synonym_rule.synonym_type") ~
      get[String]("synonym_rule.term") ~
      get[Int]("synonym_rule.status") map {
      case id~synonymType~term~status => SynonymRule(id, synonymType, term, (status & 0x01) == 0x01)
    }
  }

  /**
    * Parse a UpDownRule from a ResultSet
    */
  private[models] val simpleUpDownRule = {
    get[Option[Long]]("up_down_rule.id") ~
      get[Int]("up_down_rule.up_down_type") ~
      get[Int]("up_down_rule.boost_malus_value") ~
      get[String]("up_down_rule.term") ~
      get[Int]("up_down_rule.status") map {
      case id~upDownType~boostMalusValue~term~status => UpDownRule(id, upDownType, boostMalusValue, term, (status & 0x01) == 0x01)
    }
  }

  /**
    * Parse a FilterRule from a ResultSet
    */
  private[models] val simpleFilterRule = {
    get[Option[Long]]("filter_rule.id") ~
      get[String]("filter_rule.term") ~
      get[Int]("filter_rule.status") map {
      case id~term~status => FilterRule(id, term, (status & 0x01) == 0x01)
    }
  }

  /**
    * Parse a DeleteRule from a ResultSet
    */
  private[models] val simpleDeleteRule = {
    get[Option[Long]]("delete_rule.id") ~
      get[String]("delete_rule.term") ~
      get[Int]("delete_rule.status") map {
      case id~term~status => DeleteRule(id, term, (status & 0x01) == 0x01)
    }
  }

  /**
    * Parse a DeleteRule from a ResultSet
    */
  private[models] val simpleSuggestedSolrField = {
    get[Option[Long]]("suggested_solr_field.id") ~
      get[String]("suggested_solr_field.name") map {
      case id~name => SuggestedSolrField(id, name)
    }
  }

  def getSyonymRulesForSearchInputWithId(searchInputId: Long, maybeRestrictSynonymType: Option[Int]): List[SynonymRule] = db.withConnection { implicit connection => {
      // TODO solve more elegant while adding a whole anorm-SQL-AND-clause conditionally in match/case (not only SQL-string components)
      val STATIC_SQL_PREFIX = "select * from synonym_rule where synonym_rule.search_input_id = {search_input_id}"
      val STATIC_SQL_SUFFIX = "order by synonym_rule.term"
      return maybeRestrictSynonymType match {
        case Some(restrictSynonymType) =>
          SQL(
            STATIC_SQL_PREFIX +
              " and synonym_rule.synonym_type = {synonym_type} " +
              STATIC_SQL_SUFFIX)
            .on(
              'search_input_id -> searchInputId,
              'synonym_type -> restrictSynonymType)
            .as(simpleSynonymRule *)
        case None =>
          SQL(STATIC_SQL_PREFIX + " " + STATIC_SQL_SUFFIX)
            .on('search_input_id -> searchInputId)
            .as(simpleSynonymRule *)
      }
    }
  }

  def getUpDownRulesForSearchInputWithId(searchInputId: Long): List[UpDownRule] = db.withConnection { implicit connection =>
    SQL("select * from up_down_rule where up_down_rule.search_input_id = {search_input_id} order by up_down_rule.term")
    .on('search_input_id -> searchInputId)
    .as(simpleUpDownRule *)
  }

  def getFilterRulesForSearchInputWithId(searchInputId: Long): List[FilterRule] = db.withConnection { implicit connection =>
    SQL("select * from filter_rule where filter_rule.search_input_id = {search_input_id} order by filter_rule.term")
      .on('search_input_id -> searchInputId)
      .as(simpleFilterRule *)
  }

  def getDeleteRulesForSearchInputWithId(searchInputId: Long): List[DeleteRule] = db.withConnection { implicit connection =>
    SQL("select * from delete_rule where delete_rule.search_input_id = {search_input_id} order by delete_rule.term")
      .on('search_input_id -> searchInputId)
      .as(simpleDeleteRule *)
  }

  /**
    * List all Solr Indeces the SearchInput's can be configured for
    *
    * @return tbd
    */
  def listAllSolrIndeces: List[SolrIndex] = db.withConnection { implicit connection =>
    SQL("select * from solr_index order by name asc")
    .as(simpleSolrIndex *)
  }

  def getSolrIndexName(solrIndexId: Long): String = db.withConnection { implicit connection =>
    val allMatchingIndeces = SQL(
        "select * from solr_index " +
        "where id = {solr_index_id}"
      )
      .on(
        'solr_index_id -> solrIndexId
      )
      .as(simpleSolrIndex *)
    // TODO Handle illegal cases, if none or 1+ solr indeces selected
    return allMatchingIndeces.head.name
  }

  def addNewSolrIndex(newSolrIndex: SolrIndex): Option[Long] = db.withConnection { implicit connection =>
    SQL("insert into solr_index(name, description) values ({index_name}, {index_description})")
      .on(
        'index_name -> newSolrIndex.name,
        'index_description -> newSolrIndex.description
      )
      .executeInsert()
  }

  /**
    * Lists all Search Inputs including directed Synonyms belonging to them (for a list overview)
    *
    * @return tbd
    */
  def listAllSearchInputsInclDirectedSynonyms(solrIndexId: Long): List[SearchInput] = db.withConnection { implicit connection =>
    var resultListSearchInput: List[SearchInput] =
      SQL(
        "select * from search_input " +
        "where solr_index_id = {solr_index_id} " +
        "order by term asc")
        .on(
          'solr_index_id -> solrIndexId
        )
        .as(simpleSearchInput *)

    // TODO see SearchManagementModel.SearchInput, solve more elegant
    for (searchInput <- resultListSearchInput) {
      searchInput.synonymRules = getSyonymRulesForSearchInputWithId(searchInput.id.get, Some(0))
    }

    resultListSearchInput
  }

  /**
    * Adds new Search Input (term) to the database table. This method only focuses the term, and does not care about any synonyms.
    *
    * @param solrIndexId
    * @param searchInputTerm
    * @return
    */
  def addNewSearchInput(solrIndexId: Long, searchInputTerm: String): Option[Long] = db.withConnection { implicit connection =>
    SQL("insert into search_input(term, solr_index_id) values ({synonym_term}, {solr_index_id})")
      .on(
        'synonym_term -> searchInputTerm,
        'solr_index_id -> solrIndexId
      )
      .executeInsert()
  }

  /**
    * tbd
    *
    * @param searchInputId tbd
    * @return tbd
    */
  def getDetailedSearchInput(searchInputId: Long) = db.withConnection { implicit connection =>
    var resultListSearchInput: List[SearchInput] =
      SQL("select * from search_input where search_input.id = {search_input_id}")
          .on('search_input_id -> searchInputId)
        .as(simpleSearchInput *)

    // TODO see SearchManagementModel.SearchInput, solve more elegant
    resultListSearchInput(0).synonymRules = getSyonymRulesForSearchInputWithId(searchInputId, None)
    resultListSearchInput(0).upDownRules = getUpDownRulesForSearchInputWithId(searchInputId)
    resultListSearchInput(0).filterRules = getFilterRulesForSearchInputWithId(searchInputId)
    resultListSearchInput(0).deleteRules = getDeleteRulesForSearchInputWithId(searchInputId)

    // TODO not retrieve a list, but one anorm-search_input-entry only. Check that exactly one exists.
    resultListSearchInput(0)
  }

  def diffAndUpdateSynonymRulesOfSearchInput(searchInput: SearchInput) = db.withConnection { implicit connection =>
    // diff synonymRules
    var unconsideredSynonymRuleIds = ListBuffer.empty[Long]
    // ... update matching
    for (existingSynonymRule <- getSyonymRulesForSearchInputWithId(searchInput.id.get, None)) {
      var bFound = false
      for (updateSynonymRule <- searchInput.synonymRules) {
        updateSynonymRule.id match {
          case Some(updateSynonymRuleId) => {
            if (existingSynonymRule.id.get.equals(updateSynonymRuleId)) {
              SQL(
                "update synonym_rule " +
                  "set " +
                    "synonym_rule.synonym_type = {synonym_rule_type}, " +
                    "synonym_rule.term = {synonym_rule_term}, " +
                    "synonym_rule.status = {synonym_status} " +
                  "where synonym_rule.id = {synonym_rule_id}"
              )
                .on(
                  'synonym_rule_type -> updateSynonymRule.synonymType,
                  'synonym_rule_term -> updateSynonymRule.term,
                  'synonym_status -> (if(updateSynonymRule.isActive) 0x01 else 0x00),
                  'synonym_rule_id -> updateSynonymRuleId
                )
                .executeUpdate()
              bFound = true
            }
          }
          case None => {}
        }
      }
      if (!bFound) {
        unconsideredSynonymRuleIds += existingSynonymRule.id.get
      }
    }
    // ... delete unconsidered
    for (deleteSynonymRuleId <- unconsideredSynonymRuleIds) {
      SQL("delete from synonym_rule where synonym_rule.id = {synonym_rule_id}").on('synonym_rule_id -> deleteSynonymRuleId).execute()
    }
    // ... insert newly added
    for (newSynonymRule <- searchInput.synonymRules.filter(r => r.id.isEmpty)) {
      SQL(
        "insert into synonym_rule(synonym_type, term, status, search_input_id) " +
          "values ({synonym_type}, {synonym_term}, {synonym_status}, {search_input_id})")
        .on(
          'synonym_type -> newSynonymRule.synonymType,
          'synonym_term -> newSynonymRule.term,
          'synonym_status -> (if(newSynonymRule.isActive) 0x01 else 0x00),
          'search_input_id -> searchInput.id.get
        )
        .executeInsert()
    }
  }

  def diffAndUpdateUpDownRulesOfSearchInput(searchInput: SearchInput) = db.withConnection { implicit connection =>
    var unconsideredUpDownRuleIds = ListBuffer.empty[Long]
    // ... update matching
    for (existingUpDownRule <- getUpDownRulesForSearchInputWithId(searchInput.id.get)) {
      var bFound = false
      for (updateUpDownRule <- searchInput.upDownRules) {
        updateUpDownRule.id match {
          case Some(updateUpDownRuleId) => {
            if (existingUpDownRule.id.get.equals(updateUpDownRuleId)) {
              SQL(
                "update up_down_rule " +
                  "set " +
                    "up_down_rule.up_down_type = {up_down_type}, " +
                    "up_down_rule.boost_malus_value = {boost_malus_value}, " +
                    "up_down_rule.term = {up_down_rule_term}, " +
                    "up_down_rule.status = {up_down_rule_status} " +
                  "where up_down_rule.id = {up_down_rule_id}"
              )
                .on(
                  'up_down_type -> updateUpDownRule.upDownType,
                  'boost_malus_value -> updateUpDownRule.boostMalusValue,
                  'up_down_rule_term -> updateUpDownRule.term,
                  'up_down_rule_status -> (if(updateUpDownRule.isActive) 0x01 else 0x00),
                  'up_down_rule_id -> updateUpDownRuleId
                )
                .executeUpdate()
              bFound = true
            }
          }
          case None => {}
        }
      }
      if (!bFound) {
        unconsideredUpDownRuleIds += existingUpDownRule.id.get
      }
    }
    // ... delete unconsidered
    for (deleteUpDownRuleId <- unconsideredUpDownRuleIds) {
      SQL("delete from up_down_rule where up_down_rule.id = {up_down_rule_id}").on('up_down_rule_id -> deleteUpDownRuleId).execute()
    }
    // ... insert newly added
    for (newUpDownRule <- searchInput.upDownRules.filter(r => r.id.isEmpty)) {
      SQL(
        "insert into up_down_rule(up_down_type, boost_malus_value, term, status, search_input_id) " +
          "values ({up_down_type}, {boost_malus_value}, {up_down_rule_term}, {up_down_rule_status}, {search_input_id})")
        .on(
          'up_down_type -> newUpDownRule.upDownType,
          'boost_malus_value -> newUpDownRule.boostMalusValue,
          'up_down_rule_term -> newUpDownRule.term,
          'up_down_rule_status -> (if(newUpDownRule.isActive) 0x01 else 0x00),
          'search_input_id -> searchInput.id.get
        )
        .executeInsert()
    }
  }

  def diffAndUpdateFilterRulesOfSearchInput(searchInput: SearchInput) = db.withConnection { implicit connection =>
    var unconsideredFilterRuleIds = ListBuffer.empty[Long]
    // ... update matching
    for (existingFilterRule <- getFilterRulesForSearchInputWithId(searchInput.id.get)) {
      var bFound = false
      for (updateFilterRule <- searchInput.filterRules) {
        updateFilterRule.id match {
          case Some(updateFilterRuleId) => {
            if (existingFilterRule.id.get.equals(updateFilterRuleId)) {
              SQL(
                "update filter_rule " +
                  "set " +
                    "filter_rule.term = {filter_rule_term}, " +
                    "filter_rule.status = {filter_rule_status} " +
                  "where filter_rule.id = {filter_rule_id}"
              )
                .on(
                  'filter_rule_term -> updateFilterRule.term,
                  'filter_rule_status -> (if(updateFilterRule.isActive) 0x01 else 0x00),
                  'filter_rule_id -> updateFilterRuleId
                )
                .executeUpdate()
              bFound = true
            }
          }
          case None => {}
        }
      }
      if (!bFound) {
        unconsideredFilterRuleIds += existingFilterRule.id.get
      }
    }
    // ... delete unconsidered
    for (deleteFilterRuleId <- unconsideredFilterRuleIds) {
      SQL("delete from filter_rule where filter_rule.id = {filter_rule_id}").on('filter_rule_id -> deleteFilterRuleId).execute()
    }
    // ... insert newly added
    for (newFilterRule <- searchInput.filterRules.filter(r => r.id.isEmpty)) {
      SQL(
        "insert into filter_rule(term, status, search_input_id) " +
          "values ({filter_rule_term}, {filter_rule_status}, {search_input_id})")
        .on(
          'filter_rule_term -> newFilterRule.term,
          'filter_rule_status -> (if(newFilterRule.isActive) 0x01 else 0x00),
          'search_input_id -> searchInput.id.get
        )
        .executeInsert()
    }
  }

  def diffAndUpdateDeleteRulesOfSearchInput(searchInput: SearchInput) = db.withConnection { implicit connection =>
    var unconsideredDeleteRuleIds = ListBuffer.empty[Long]
    // ... update matching
    for (existingDeleteRule <- getDeleteRulesForSearchInputWithId(searchInput.id.get)) {
      var bFound = false
      for (updateDeleteRule <- searchInput.deleteRules) {
        updateDeleteRule.id match {
          case Some(updateDeleteRuleId) => {
            if (existingDeleteRule.id.get.equals(updateDeleteRuleId)) {
              SQL(
                "update delete_rule " +
                  "set " +
                    "delete_rule.term = {delete_rule_term}, " +
                    "delete_rule.status = {delete_rule_status} " +
                  "where delete_rule.id = {delete_rule_id}"
              )
                .on(
                  'delete_rule_term -> updateDeleteRule.term,
                  'delete_rule_status -> (if(updateDeleteRule.isActive) 0x01 else 0x00),
                  'delete_rule_id -> updateDeleteRuleId
                )
                .executeUpdate()
              bFound = true
            }
          }
          case None => {}
        }
      }
      if (!bFound) {
        unconsideredDeleteRuleIds += existingDeleteRule.id.get
      }
    }
    // ... delete unconsidered
    for (deleteDeleteRuleId <- unconsideredDeleteRuleIds) {
      SQL("delete from delete_rule where delete_rule.id = {delete_rule_id}").on('delete_rule_id -> deleteDeleteRuleId).execute()
    }
    // ... insert newly added
    for (newDeleteRule <- searchInput.deleteRules.filter(r => r.id.isEmpty)) {
      SQL(
        "insert into delete_rule(term, status, search_input_id) " +
          "values ({delete_rule_term}, {delete_rule_status}, {search_input_id})")
        .on(
          'delete_rule_term -> newDeleteRule.term,
          'delete_rule_status -> (if(newDeleteRule.isActive) 0x01 else 0x00),
          'search_input_id -> searchInput.id.get
        )
        .executeInsert()
    }
  }

  /**
    * tbd
    *
    * @param searchInput tbd
    */
  def updateSearchInput(searchInput: SearchInput) = db.withConnection { implicit connection =>
    // TODO verify, that necessary Ids are passed to searchInput

    // update SearchInput itself
    SQL(
      "update search_input " +
      "set search_input.term = {search_input_term} " +
      "where search_input.id = {search_input_id}")
    .on(
      'search_input_id -> searchInput.id,
      'search_input_term -> searchInput.term
    )
    .executeUpdate()

    // TODO think about more abstract solution for SQL-update, -delete and -insert (functional-only) to avoid repetitive boilerplate code
    // TODO in this solution, verify that affected rows after execute... is plausible
    diffAndUpdateSynonymRulesOfSearchInput(searchInput)
    diffAndUpdateUpDownRulesOfSearchInput(searchInput)
    diffAndUpdateFilterRulesOfSearchInput(searchInput)
    diffAndUpdateDeleteRulesOfSearchInput(searchInput)
  }

  /**
    * tbd
    *
    * @param searchInputId tbd
    * @return tbd
    */
  def deleteSearchInput(searchInputId: Long) = db.withConnection { implicit connection =>
    // TODO maybe realise as BatchSql
    // TODO verify amount of deleted DB entries
    SQL("delete from delete_rule where delete_rule.search_input_id = {search_input_id}").on('search_input_id -> searchInputId).execute()
    SQL("delete from filter_rule where filter_rule.search_input_id = {search_input_id}").on('search_input_id -> searchInputId).execute()
    SQL("delete from up_down_rule where up_down_rule.search_input_id = {search_input_id}").on('search_input_id -> searchInputId).execute()
    SQL("delete from synonym_rule where synonym_rule.search_input_id = {search_input_id}").on('search_input_id -> searchInputId).execute()
    SQL("delete from search_input where search_input.id = {search_input_id}").on('search_input_id -> searchInputId).execute()
  }

  def listAllSuggestedSolrFields(solrIndexId: Long) = db.withConnection { implicit connection =>
    SQL(
      "select * from suggested_solr_field " +
      "where solr_index_id = {solr_index_id} " +
      "order by name asc"
    )
    .on(
      'solr_index_id -> solrIndexId
    )
    .as(simpleSuggestedSolrField *)
  }

  def addNewDeploymentLogOk(solrIndexId: Long, targetPlatform: String) = db.withConnection { implicit connection =>
    SQL("insert into deployment_log(solr_index_id, target_platform) values ({solr_index_id}, {target_platform})")
      .on(
        'solr_index_id -> solrIndexId,
        'target_platform -> targetPlatform
      )
      .executeInsert()
  }

}
