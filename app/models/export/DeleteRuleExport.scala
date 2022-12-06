package models.`export`

import anorm.SqlParser.get
import anorm.{RowParser, ~}
import models.`export`.JsonExportable
import models.input.SearchInputId
import models.rules.{CommonRuleFields, DeleteRuleId}
import models.{Status, `export`}
import play.api.libs.json._

import java.time.LocalDateTime

case class DeleteRuleExport(id: DeleteRuleId = DeleteRuleId(),
                      term: String,
                      isActive: Boolean,
                      status: Int,
                      searchInputId: SearchInputId,
                      lastUpdate: LocalDateTime) extends JsonExportable {

  def getTableName: JsString = JsString("delete_rule")

  def getColumns: JsValue = {
    JsArray(
      IndexedSeq (
        JsString("id"),
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
        JsString(term),
        JsString(searchInputId.toString),
        JsString(lastUpdate.toString),
        JsNumber(status)
      )
    )
  }

}

object DeleteRuleExport extends CommonRuleFields {

  val TABLE_NAME = "delete_rule"

  implicit val jsonFormat: OFormat[DeleteRuleExport] = Json.format[DeleteRuleExport]

  val sqlParser: RowParser[DeleteRuleExport] = {
    get[DeleteRuleId](s"$TABLE_NAME.$ID") ~
      get[String](s"$TABLE_NAME.$TERM") ~
      get[Int](s"$TABLE_NAME.$STATUS") ~
      get[SearchInputId](s"$TABLE_NAME.$SEARCH_INPUT_ID") ~
      get[LocalDateTime](s"$TABLE_NAME.$LAST_UPDATE") map { case id ~ term ~ status ~ searchInputId ~ lastUpdate =>
      `export`.DeleteRuleExport(id, term, Status.isActiveFromStatus(status), status, searchInputId, lastUpdate)
    }
  }

  val selectAllStatement = s"select id, term, search_input_id, last_update, status from delete_rule"

  def selectStatement(id: String) : String = {
    this.selectAllStatement + " where search_input_id in (select id from search_input where solr_index_id = '" + id + "')"
  }

}