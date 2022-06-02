package models.`export`

import anorm.SqlParser.get
import anorm.{RowParser, SQL, ~}
import models.{SolrIndexId, `export`}
import models.rules.CommonRuleFields
import play.api.libs.json._

import java.time.LocalDateTime

case class  SolrIndexExport(id: SolrIndexId,
                           name: String,
                           description: String,
                           lastUpdate: LocalDateTime) extends JsonExportable {

  def getTableName: JsString = JsString("solr_index")

  def getColumns: JsValue = {
    JsArray(
      IndexedSeq (
        JsString("id"),
        JsString("name"),
        JsString("description"),
        JsString("last_update")
      )
    )
  }

  def getRow: JsValue = {
    JsArray(
      IndexedSeq (
        JsString(id.toString),
        JsString(name),
        JsString(description),
        JsString(lastUpdate.toString)
      )
    )
  }
}

object SolrIndexExport extends CommonRuleFields {

  val TABLE_NAME = "solr_index"
  val NAME = "name"
  val DESCRIPTION = "description"

  val sqlParser: RowParser[SolrIndexExport] = {
    get[SolrIndexId](s"$TABLE_NAME.$ID") ~
      get[String](s"$TABLE_NAME.$ID") ~
      get[String](s"$TABLE_NAME.$DESCRIPTION") ~
      get[LocalDateTime](s"$TABLE_NAME.$LAST_UPDATE") map { case id ~ name ~ description ~ lastUpdate =>
      SolrIndexExport(id, name, description, lastUpdate)
    }
  }

  val selectAllStatement = s"select $TABLE_NAME.$ID, $TABLE_NAME.$ID, $TABLE_NAME.$DESCRIPTION, $TABLE_NAME.$LAST_UPDATE from $TABLE_NAME"

  def selectStatement(id: String) = {
    s"select $TABLE_NAME.$ID, $TABLE_NAME.$ID, $TABLE_NAME.$DESCRIPTION, $TABLE_NAME.$LAST_UPDATE from $TABLE_NAME where $TABLE_NAME.$ID = '" + id + "'"
  }

}