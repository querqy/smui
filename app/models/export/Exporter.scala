package models.`export`

import anorm.SQL
import models.DatabaseExecutionContext
import models.FeatureToggleModel.FeatureToggleService
import models.input.{SearchInput, SearchInputExport}
import models.rules.{DeleteRule, DeleteRuleExport, FilterRule, FilterRuleExport, SynonymRule, SynonymRuleExport, UpDownRuleExport}
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
      val headTable = tables.head
      val remainingTables = tables.drop(1)
      println("remaining")
      println(remainingTables.size)

      if (headTable.nonEmpty) {
        println("tablename")
        println(headTable.seq(0).getTableName)
        val name: (String, JsValue) = "tableName" -> headTable.seq(0).getTableName
        val cols: (String, JsValue) = "columns" -> headTable.seq(0).getColumns
        val rows: (String, JsValue) = "rows" -> asIndexedSeqForJs(headTable)
        val obj: (JsValue) = JsObject(IndexedSeq(name, cols, rows))
        println("calling recursion")
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

//  def getSomethingsFromDatabase: IndexedSeq[Something] = db.withConnection {
//    implicit connection => {
//      logger.debug("In SearchManagementRepository:getSomethingsFromDatabase():1")
//      val x : List[Something] = SQL(s"select id, value0, last_update from something")
//        .as(SomethingRow.sqlParser.*)
//      logger.debug("In SearchManagementRepository:getSomethingsFromDatabase():2")
//      x.toIndexedSeq
//    }
//  }

  def getSearchInputsFromDatabase: IndexedSeq[SearchInputExport] = db.withConnection {
    implicit connection => {
      val x : List[SearchInputExport] = SQL(s"select id, term, solr_index_id, last_update, status, comment from search_input")
        .as(SearchInputExport.sqlParser.*)
      x.toIndexedSeq
    }
  }

  def getDeleteRulesFromDatabase: IndexedSeq[DeleteRuleExport] = db.withConnection {
    implicit connection => {
      val x : List[DeleteRuleExport] = SQL(s"select id, term, search_input_id, last_update, status from delete_rule")
        .as(DeleteRuleExport.sqlParser.*)
      x.toIndexedSeq
    }
  }

  def getFilterRulesFromDatabase: IndexedSeq[FilterRuleExport] = db.withConnection {
    implicit connection => {
      logger.debug("In getFilterRulesFromDatabase")
      val x : List[FilterRuleExport] = SQL(s"select id, term, search_input_id, last_update, status from filter_rule")
        .as(FilterRuleExport.sqlParser.*)
      x.toIndexedSeq
    }
  }

  def getSynonymRulesFromDatabase: IndexedSeq[SynonymRuleExport] = db.withConnection {
    implicit connection => {
      val x : List[SynonymRuleExport] = SQL(s"select id, synonym_type, term, search_input_id, last_update, status from synonym_rule")
        .as(SynonymRuleExport.sqlParser.*)
      x.toIndexedSeq
    }
  }

  def getUpDownRulesFromDatabase: IndexedSeq[UpDownRuleExport] = db.withConnection {
    implicit connection => {
      val x : List[UpDownRuleExport] = SQL(s"select id, up_down_type, boost_malus_value, term, search_input_id, last_update, status from up_down_rule")
        .as(UpDownRuleExport.sqlParser.*)
      x.toIndexedSeq
    }
  }

  def getDatabaseJson: JsValue = {
    logger.debug("In getDatabaseJson")
    val tableSeq = IndexedSeq(
      //getSomethingsFromDatabase,
      getSearchInputsFromDatabase,
      getDeleteRulesFromDatabase,
      getFilterRulesFromDatabase,
      getSynonymRulesFromDatabase,
      getUpDownRulesFromDatabase
    )
    getAllTablesForJs(tableSeq)
  }

}
