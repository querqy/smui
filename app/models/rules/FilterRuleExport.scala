package models.rules

import anorm.SqlParser.get
import anorm.{RowParser, ~}
import models.`export`.JsonExportable
import models.input.SearchInputId
import models.rules.DeleteRule.{LAST_UPDATE, SEARCH_INPUT_ID, TABLE_NAME}
import models.{Id, IdObject, Status}
import play.api.libs.json.{JsArray, JsNumber, JsString, JsValue, Json, OFormat}

import java.time.LocalDateTime

//class FilterRuleId(id: String) extends Id(id)
//object FilterRuleId extends IdObject[FilterRuleId](new FilterRuleId(_))


case class FilterRuleExport(id: FilterRuleId,
                      term: String,
                      isActive: Boolean,
                      status: Int,
                      searchInputId: SearchInputId,
                      lastUpdate: LocalDateTime) extends JsonExportable {

  def getTableName: JsString = JsString("filter_rule")

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

object FilterRuleExport extends CommonRuleFields {

  val TABLE_NAME = "filter_rule"

  implicit val jsonFormat: OFormat[FilterRule] = Json.format[FilterRule]

  val sqlParser: RowParser[FilterRuleExport] = {
    get[FilterRuleId](s"$TABLE_NAME.$ID") ~
      get[String](s"$TABLE_NAME.$TERM") ~
      get[Int](s"$TABLE_NAME.$STATUS") ~
      get[SearchInputId](s"$TABLE_NAME.$SEARCH_INPUT_ID") ~
      get[LocalDateTime](s"$TABLE_NAME.$LAST_UPDATE") map { case id ~ term ~ status ~ searchInputId ~ lastUpdate =>
      FilterRuleExport(id, term, Status.isActiveFromStatus(status), status, searchInputId, lastUpdate)
    }
  }

}