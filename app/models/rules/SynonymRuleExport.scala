package models.rules

import java.sql.Connection
import anorm.SqlParser.get
import anorm._
import models.`export`.{JsonExportable, Something, SomethingId}
import models.input.SearchInputId
import models.rules.FilterRule.{LAST_UPDATE, SEARCH_INPUT_ID, TABLE_NAME}
import models.{Id, IdObject, Status}
import play.api.libs.json.{JsArray, JsNumber, JsString, JsValue, Json, OFormat}

import java.time.LocalDateTime

case class SynonymRuleExport(id: SynonymRuleId,
                       synonymType: Int,
                       term: String,
                       status: Int,
                       searchInputId: SearchInputId,
                       lastUpdate: LocalDateTime) extends JsonExportable {

  def getTableName: JsString = JsString("synonym_rule")

  def getColumns: JsValue = {
    JsArray(
      IndexedSeq (
        JsString("id"),
        JsString("synonymType"),
        JsString("term"),
        JsString("search_input_id"),
        JsString("last_update"),
        JsString("status")
      )
    )
  }

  def getRow: JsValue = {
    JsArray(
      IndexedSeq (
        JsString(id.toString),
        JsNumber(synonymType),
        JsString(term),
        JsString(searchInputId.toString),
        JsString(lastUpdate.toString),
        JsNumber(status)
      )
    )
  }
}

object SynonymRuleExport extends CommonRuleFields {

  val TABLE_NAME = "synonym_rule"
  val TYPE = "synonym_type"

  val TYPE_UNDIRECTED = 0
  val TYPE_DIRECTED = 1

  implicit val jsonFormat: OFormat[SynonymRule] = Json.format[SynonymRule]

  val sqlParser: RowParser[SynonymRuleExport] = {
    get[SynonymRuleId](s"$TABLE_NAME.$ID") ~
      get[Int](s"$TABLE_NAME.$TYPE") ~
      get[String](s"$TABLE_NAME.$TERM") ~
      get[Int](s"$TABLE_NAME.$STATUS") ~
      get[SearchInputId](s"$TABLE_NAME.$SEARCH_INPUT_ID") ~
      get[LocalDateTime](s"$TABLE_NAME.$LAST_UPDATE") map { case id ~ synonymType ~ term ~ status ~ searchInputId ~ lastUpdate =>
      SynonymRuleExport(id, synonymType, term, status, searchInputId, lastUpdate)
    }
  }

}