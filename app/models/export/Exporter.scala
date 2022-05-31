package models.`export`

import anorm.SQL
import models.{DatabaseExecutionContext, SuggestedSolrField}
import models.FeatureToggleModel.FeatureToggleService
import models.input.SearchInput
import models.rules.{DeleteRule, FilterRule, SynonymRule}
import play.api.libs.json.{JsArray, JsObject, JsString, JsValue}
import play.api.Logging
import play.api.db.DBApi

import java.time.LocalDateTime
import javax.inject.Inject

@javax.inject.Singleton
class Exporter @Inject()(dbapi: DBApi, toggleService: FeatureToggleService)(implicit ec: DatabaseExecutionContext) extends Logging {

  private val db = dbapi.database("default")

  var somethings1: IndexedSeq[Something] = IndexedSeq(
    new Something(SomethingId(), "something1", LocalDateTime.now()),
    new Something(SomethingId(), "something2", LocalDateTime.now())
  )

  def getAllTablesForJs(tables : IndexedSeq[IndexedSeq[JsonExportable]]): JsValue = {
    var aggregation: Seq[JsValue] = Seq[JsValue]()
    println("hello ------ ")
    println(tables.size)
    getAllTablesForJs1(tables, aggregation)
  }

  def getAllTablesForJs1(tables : IndexedSeq[IndexedSeq[JsonExportable]], aggregation: Seq[JsValue]): JsValue = {
    if (tables.nonEmpty) {
      val headTable = tables.head // the first table
      val remainingTables = tables.drop(1) // a list of the tables AFTER the first table
      if (headTable.nonEmpty) {
        logger.debug("In Exporter.getAllTablesForJs1 : head table was non-empty")
        val name: (String, JsValue) = "tableName" -> headTable.seq(0).getTableName
        val cols: (String, JsValue) = "columns" -> headTable.seq(0).getColumns
        val rows: (String, JsValue) = "rows" -> asIndexedSeqForJs(headTable)
        val obj: (JsValue) = JsObject(IndexedSeq(name, cols, rows))
        getAllTablesForJs1(remainingTables, aggregation :+ obj)
      } else {
        logger.debug("In Exporter.getAllTablesForJs1 : return aggregation because headTable is empty")
        JsArray(aggregation.toIndexedSeq)
      }
    } else {
      logger.debug("In Exporter.getAllTablesForJs1 : return aggregation because tables is empty")
      JsArray(aggregation.toIndexedSeq)
    }
  }

  def asIndexedSeqForJs(indexedSeqSource : IndexedSeq[JsonExportable]): JsValue = {
    var target: IndexedSeq[JsValue] = IndexedSeq[JsValue]()
    for((element,index) <- indexedSeqSource.view.zipWithIndex) {
      logger.debug("In Exporter.asIndexedSeqForJs : String #" + index + " is " + element.getRow)
      target = target :+ element.getRow
    }
    JsArray(target.toIndexedSeq)
  }

  def getSomethingsFromDatabase: IndexedSeq[Something] = db.withConnection {
    implicit connection => {
      SQL(Something.selectAllStatement).as(Something.sqlParser.*).toIndexedSeq
    }
  }

  def getSearchInputsFromDatabase: IndexedSeq[SearchInputExport] = db.withConnection {
    implicit connection => {
      SQL(SearchInputExport.selectAllStatement).as(SearchInputExport.sqlParser.*).toIndexedSeq
    }
  }

  def getDeleteRulesFromDatabase: IndexedSeq[DeleteRuleExport] = db.withConnection {
    implicit connection => {
      SQL(DeleteRuleExport.selectAllStatement).as(DeleteRuleExport.sqlParser.*).toIndexedSeq
    }
  }

  def getFilterRulesFromDatabase: IndexedSeq[FilterRuleExport] = db.withConnection {
    implicit connection => {
      SQL(FilterRuleExport.selectAllStatement).as(FilterRuleExport.sqlParser.*).toIndexedSeq
    }
  }

  def getSynonymRulesFromDatabase: IndexedSeq[SynonymRuleExport] = db.withConnection {
    implicit connection => {
      SQL(SynonymRuleExport.selectAllStatement).as(SynonymRuleExport.sqlParser.*).toIndexedSeq
    }
  }

  def getUpDownRulesFromDatabase: IndexedSeq[UpDownRuleExport] = db.withConnection {
    implicit connection => {
      SQL(UpDownRuleExport.selectAllStatement).as(UpDownRuleExport.sqlParser.*).toIndexedSeq
    }
  }

  def getRedirectRulesFromDatabase: IndexedSeq[RedirectRuleExport] = db.withConnection {
    implicit connection => {
      SQL(RedirectRuleExport.selectAllStatement).as(RedirectRuleExport.sqlParser.*).toIndexedSeq
    }
  }

  def getSolrIndexFromDatabase: IndexedSeq[SolrIndexExport] = db.withConnection {
    implicit connection => {
      SQL(SolrIndexExport.selectAllStatement).as(SolrIndexExport.sqlParser.*).toIndexedSeq
    }
  }

  def getSuggestedSolrFieldsFromDatabase: IndexedSeq[SuggestedSolrFieldExport] = db.withConnection {
    implicit connection => {
      SQL(SuggestedSolrFieldExport.selectAllStatement).as(SuggestedSolrFieldExport.sqlParser.*).toIndexedSeq
    }
  }

  def getInputTagsFromDatabase: IndexedSeq[InputTagExport] = db.withConnection {
    implicit connection => {
      SQL(InputTagExport.selectAllStatement).as(InputTagExport.sqlParser.*).toIndexedSeq
    }
  }

  def getTagInputAssociationsFromDatabase: IndexedSeq[TagInputAssociationExport] = db.withConnection {
    implicit connection => {
      SQL(TagInputAssociationExport.selectAllStatement).as(TagInputAssociationExport.sqlParser.*).toIndexedSeq
    }
  }

  def getCanonicalSpellingsFromDatabase: IndexedSeq[CanonicalSpellingExport] = db.withConnection {
    implicit connection => {
      SQL(CanonicalSpellingExport.selectAllStatement).as(CanonicalSpellingExport.sqlParser.*).toIndexedSeq
    }
  }

  def getAlternativeSpellingsFromDatabase: IndexedSeq[AlternativeSpellingExport] = db.withConnection {
    implicit connection => {
      SQL(AlternativeSpellingExport.selectAllStatement).as(AlternativeSpellingExport.sqlParser.*).toIndexedSeq
    }
  }


  def getDatabaseJson: JsValue = {
    logger.debug("In getDatabaseJson")
    val tableSeq = IndexedSeq(
      getSolrIndexFromDatabase, //1
      getSearchInputsFromDatabase, //2
      getRedirectRulesFromDatabase, //3
      getSynonymRulesFromDatabase, //4
      getUpDownRulesFromDatabase, //5
      getDeleteRulesFromDatabase, //6
      getFilterRulesFromDatabase, //7
      getSuggestedSolrFieldsFromDatabase, //8
      getInputTagsFromDatabase, //9
      getTagInputAssociationsFromDatabase, //10
      getCanonicalSpellingsFromDatabase, //11
      getAlternativeSpellingsFromDatabase //12
      //getSomethingsFromDatabase //13
    )
    getAllTablesForJs(tableSeq)
  }

}
