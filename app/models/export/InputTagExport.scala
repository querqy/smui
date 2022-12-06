package models.export

import anorm.SqlParser.get
import anorm.{RowParser, ~}
import models.SolrIndexId
import models.input.InputTagId
import models.rules.CommonRuleFields
import play.api.libs.json._

import java.time.LocalDateTime

case class  InputTagExport(id: InputTagId,
                           solrIndexId: SolrIndexId,
                           property: String,
                           tagValue: String,
                           exported: Int,
                           predefined: Int,
                           lastUpdate: LocalDateTime) extends JsonExportable {

  def getTableName: JsString = JsString("input_tag")

  def getColumns: JsValue = {
    JsArray(
      IndexedSeq (
        JsString("id"),
        JsString("solr_index_id"),
        JsString("property"),
        JsString("tag_value"),
        JsString("exported"),
        JsString("predefined"),
        JsString("last_update")
      )
    )
  }

  def getRow: JsValue = {
    JsArray(
      IndexedSeq (
        JsString(id.toString),
        JsString(solrIndexId.toString),
        JsString(property),
        JsString(tagValue),
        JsNumber(exported),
        JsNumber(predefined),
        JsString(lastUpdate.toString)
      )
    )
  }
}

object InputTagExport {

  val TABLE_NAME = "input_tag"
  val ID = "id"
  val SOLR_INDEX_ID = "solr_index_id"
  val PROPERTY = "property"
  val TAG_VALUE = "tag_value"
  val EXPORTED = "exported"
  val PREDEFINED = "predefined"
  val LAST_UPDATE = "last_update"

  val sqlParser: RowParser[InputTagExport] = {
    get[InputTagId](s"$TABLE_NAME.$ID") ~
      get[SolrIndexId](s"$TABLE_NAME.$SOLR_INDEX_ID") ~
      get[String](s"$TABLE_NAME.$PROPERTY") ~
      get[String](s"$TABLE_NAME.$TAG_VALUE") ~
      get[Int](s"$TABLE_NAME.$EXPORTED") ~
      get[Int](s"$TABLE_NAME.$PREDEFINED") ~
      get[LocalDateTime](s"$TABLE_NAME.$LAST_UPDATE") map {
      case id ~ solrIndexId ~ property ~ tagValue ~ exported ~ predefined ~ lastUpdate =>
        InputTagExport(id, solrIndexId, property, tagValue, exported, predefined, lastUpdate)
      }
  }

  val selectAllStatement: String = {
    s"select $TABLE_NAME.$ID, " +
    s"$TABLE_NAME.$SOLR_INDEX_ID, " +
    s"$TABLE_NAME.$PROPERTY, " +
    s"$TABLE_NAME.$TAG_VALUE, " +
    s"$TABLE_NAME.$EXPORTED, " +
    s"$TABLE_NAME.$PREDEFINED, " +
    s"$TABLE_NAME.$LAST_UPDATE from $TABLE_NAME"
  }

  def selectStatement(id: String) : String = {
    this.selectAllStatement + s" where $TABLE_NAME.$SOLR_INDEX_ID = '" + id + "'"
  }

}