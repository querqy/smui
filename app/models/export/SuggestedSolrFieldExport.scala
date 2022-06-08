package models.`export`

import anorm.SqlParser.get
import anorm._
import models._
import models.`export`.JsonExportable
import models.input.{SearchInput, SearchInputId}
import play.api.libs.json._

import java.time.LocalDateTime

case class SuggestedSolrFieldExport(id: SuggestedSolrFieldId,
                                    name: String,
                                    solrIndexId: SolrIndexId,
                                    lastUpdate: LocalDateTime) extends JsonExportable {

  def getTableName: JsString = JsString("suggested_solr_field")

  def getRow: JsValue = {
    JsArray(
      IndexedSeq (
        JsString(id.toString),
        JsString(name),
        JsString(solrIndexId.toString),
        JsString(lastUpdate.toString)
      )
    )
  }

  def getColumns: JsValue = {
    JsArray(
      IndexedSeq (
        JsString("id"),
        JsString("name"),
        JsString("solr_index_id"),
        JsString("last_update")
      )
    )
  }

}

object SuggestedSolrFieldExport {

  val TABLE_NAME = "suggested_solr_field"
  val ID = "id"
  val NAME = "name"
  val SOLR_INDEX_ID = "solr_index_id"
  val LAST_UPDATE = "last_update"

  val sqlParser: RowParser[SuggestedSolrFieldExport] = {
    get[SuggestedSolrFieldId](s"$TABLE_NAME.$ID") ~
      get[String](s"$TABLE_NAME.$NAME") ~
      get[SolrIndexId](s"$TABLE_NAME.$SOLR_INDEX_ID") ~
      get[LocalDateTime](s"$TABLE_NAME.$LAST_UPDATE") map { case id ~ name ~ solrIndexId ~ lastUpdate =>
      SuggestedSolrFieldExport(id, name, solrIndexId, lastUpdate)
    }
  }

  val selectAllStatement = s"select $TABLE_NAME.$ID, $TABLE_NAME.$NAME, $TABLE_NAME.$SOLR_INDEX_ID, $TABLE_NAME.$LAST_UPDATE from $TABLE_NAME"

  def selectStatement(id: String) : String = {
    this.selectAllStatement + " where solr_index_id = '" + id + "'"
  }

}
