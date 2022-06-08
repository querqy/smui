package models.`export`

import anorm.SQL
import models.DatabaseExecutionContext
import models.FeatureToggleModel.FeatureToggleService
import play.api.libs.json.{JsArray, JsObject, JsValue}
import play.api.Logging
import play.api.db.DBApi
import javax.inject.Inject

@javax.inject.Singleton
class Exporter @Inject()(dbApi: DBApi,
                         toggleService: FeatureToggleService)
                        (implicit ec: DatabaseExecutionContext) extends Logging {

  private val db = dbApi.database("default")

  def getAllTablesForJs(tables : IndexedSeq[IndexedSeq[JsonExportable]]): JsValue = {
    var aggregation: Seq[JsValue] = Seq[JsValue]()
    getAllTablesForJs1(tables, aggregation)
  }

  def getAllTablesForJs1(tables : IndexedSeq[IndexedSeq[JsonExportable]], aggregation: Seq[JsValue]): JsValue = {
    if (tables.nonEmpty) {
      val headTable = tables.head // the first table
      val remainingTables = tables.drop(1) // a list of the tables AFTER the first table
      if (headTable.nonEmpty) {
        logger.debug("In Exporter.getAllTablesForJs1 : head table was non-empty")
        val name: (String, JsValue) = "tableName" -> headTable.seq(0).getTableName
        logger.debug("table name: "+ name)
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

  def getSearchInputsFromDatabase(id: String): IndexedSeq[SearchInputExport] = db.withConnection {
    implicit connection => {
      SQL(SearchInputExport.selectStatement(id)).as(SearchInputExport.sqlParser.*).toIndexedSeq
    }
  }

  def getDeleteRulesFromDatabase(id: String): IndexedSeq[DeleteRuleExport] = db.withConnection {
    implicit connection => {
      SQL(DeleteRuleExport.selectStatement(id)).as(DeleteRuleExport.sqlParser.*).toIndexedSeq
    }
  }

  def getFilterRulesFromDatabase(id: String): IndexedSeq[FilterRuleExport] = db.withConnection {
    implicit connection => {
      SQL(FilterRuleExport.selectStatement(id)).as(FilterRuleExport.sqlParser.*).toIndexedSeq
    }
  }

  def getSynonymRulesFromDatabase(id: String): IndexedSeq[SynonymRuleExport] = db.withConnection {
    implicit connection => {
      SQL(SynonymRuleExport.selectStatement(id)).as(SynonymRuleExport.sqlParser.*).toIndexedSeq
    }
  }

  def getUpDownRulesFromDatabase(id: String): IndexedSeq[UpDownRuleExport] = db.withConnection {
    implicit connection => {
      SQL(UpDownRuleExport.selectStatement(id)).as(UpDownRuleExport.sqlParser.*).toIndexedSeq
    }
  }

  def getRedirectRulesFromDatabase(id: String): IndexedSeq[RedirectRuleExport] = db.withConnection {
    implicit connection => {
      SQL(RedirectRuleExport.selectStatement(id)).as(RedirectRuleExport.sqlParser.*).toIndexedSeq
    }
  }

  def getSolrIndexFromDatabase(id: String): IndexedSeq[SolrIndexExport] = db.withConnection {
    implicit connection => {
      SQL(SolrIndexExport.selectStatement(id)).as(SolrIndexExport.sqlParser.*).toIndexedSeq
    }
  }

  def getSuggestedSolrFieldsFromDatabase(id: String): IndexedSeq[SuggestedSolrFieldExport] = db.withConnection {
    implicit connection => {
      SQL(SuggestedSolrFieldExport.selectStatement(id)).as(SuggestedSolrFieldExport.sqlParser.*).toIndexedSeq
    }
  }

  def getInputTagsFromDatabase(id: String): IndexedSeq[InputTagExport] = db.withConnection {
    implicit connection => {
      SQL(InputTagExport.selectStatement(id)).as(InputTagExport.sqlParser.*).toIndexedSeq
    }
  }

  def getTagInputAssociationsFromDatabase(id: String): IndexedSeq[TagInputAssociationExport] = db.withConnection {
    implicit connection => {
      SQL(TagInputAssociationExport.selectStatement(id)).as(TagInputAssociationExport.sqlParser.*).toIndexedSeq
    }
  }

  def getCanonicalSpellingsFromDatabase(id: String): IndexedSeq[CanonicalSpellingExport] = db.withConnection {
    implicit connection => {
      SQL(CanonicalSpellingExport.selectStatement(id)).as(CanonicalSpellingExport.sqlParser.*).toIndexedSeq
    }
  }

  def getAlternativeSpellingsFromDatabase(id: String): IndexedSeq[AlternativeSpellingExport] = db.withConnection {
    implicit connection => {
      SQL(AlternativeSpellingExport.selectStatement(id)).as(AlternativeSpellingExport.sqlParser.*).toIndexedSeq
    }
  }

  def getDatabaseJsonWithId(id: String): JsValue = {
    logger.debug("In Exporter.getDatabaseJsonWithId")
    val tableSeq = IndexedSeq(
      getSolrIndexFromDatabase(id),
      getSearchInputsFromDatabase(id),
      getRedirectRulesFromDatabase(id),
      getSynonymRulesFromDatabase(id),
      getUpDownRulesFromDatabase(id),
      getDeleteRulesFromDatabase(id),
      getFilterRulesFromDatabase(id),
      getSuggestedSolrFieldsFromDatabase(id),
      getInputTagsFromDatabase(id),
      getTagInputAssociationsFromDatabase(id),
      getCanonicalSpellingsFromDatabase(id),
      getAlternativeSpellingsFromDatabase(id)
    )
    getAllTablesForJs(tableSeq)
  }

}
