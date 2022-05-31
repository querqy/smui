package models.rules

import anorm.SqlParser.get
import anorm.{NamedParameter, RowParser, ~}
import models.`export`.JsonExportable
import models.input.SearchInputId
import models.{Id, IdObject, Status}
import play.api.libs.json.{JsArray, JsNumber, JsString, JsValue, Json, OFormat}

import java.time.LocalDateTime

case class RedirectRuleExport(id: RedirectRuleId = RedirectRuleId(),
                        target: String,
                        searchInputId: SearchInputId,
                        lastUpdate: LocalDateTime,
                        status: Int) extends JsonExportable {

  def getTableName: JsString = JsString("redirect_rule")

  def getColumns: JsValue = {
    JsArray(
      IndexedSeq (
        JsString("id"),
        JsString("target"),
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
        JsString(target),
        JsString(searchInputId.toString),
        JsString(lastUpdate.toString),
        JsNumber(status)
      )
    )
  }
}

object RedirectRuleExport extends CommonRuleFields {

  val TABLE_NAME = "redirect_rule"
  val TARGET = "target"

  implicit val jsonFormat: OFormat[RedirectRule] = Json.format[RedirectRule]

  //override def fieldNames: Seq[String] = super.fieldNames :+ TARGET

  //override def orderByField: String = TARGET

  val sqlParser: RowParser[RedirectRuleExport] = {
    get[RedirectRuleId](s"$TABLE_NAME.$ID") ~
      get[String](s"$TABLE_NAME.$TARGET") ~
      get[SearchInputId](s"$TABLE_NAME.$SEARCH_INPUT_ID") ~
      get[LocalDateTime](s"$TABLE_NAME.$LAST_UPDATE") ~
      get[Int](s"$TABLE_NAME.$STATUS") map { case id ~ target ~ searchInputId ~ lastUpdate ~ status =>
      RedirectRuleExport(id, target, searchInputId, lastUpdate, status)
    }
  }

}