package models.export

import anorm.SqlParser.get
import anorm.{RowParser, ~}
import models.input.SearchInputId
import models.rules.{CommonRuleFields, UpDownRuleId}
import play.api.libs.json._
import java.time.LocalDateTime

case class UpDownRuleExport(id: UpDownRuleId = UpDownRuleId(),
                      upDownType: Int,
                      boostMalusValue: Int,
                      term: String,
                      status: Int,
                      searchInputId: SearchInputId,
                      lastUpdate: LocalDateTime) extends JsonExportable {

  def getTableName: JsString = JsString("up_down_rule")

  def getColumns: JsValue = {
    JsArray(
      IndexedSeq (
        JsString("id"),
        JsString("up_down_type"),
        JsString("boost_malus_value"),
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
        JsNumber(upDownType),
        JsNumber(boostMalusValue),
        JsString(term),
        JsString(searchInputId.toString),
        JsString(lastUpdate.toString),
        JsNumber(status)
      )
    )
  }
}

object UpDownRuleExport extends CommonRuleFields {

  val TABLE_NAME = "up_down_rule"

  val UP_DOWN_TYPE = "up_down_type"
  val BOOST_MALUS_VALUE = "boost_malus_value"

  val TYPE_UP = 0
  val TYPE_DOWN = 1

  implicit val jsonFormat: OFormat[UpDownRuleExport] = Json.format[UpDownRuleExport]

  val sqlParser: RowParser[UpDownRuleExport] = {
    (get[UpDownRuleId](s"$TABLE_NAME.$ID") ~
      get[Int](s"$TABLE_NAME.$UP_DOWN_TYPE") ~
      get[Int](s"$TABLE_NAME.$BOOST_MALUS_VALUE") ~
      get[String](s"$TABLE_NAME.$TERM") ~
      get[Int](s"$TABLE_NAME.$STATUS") ~
      get[SearchInputId](s"$TABLE_NAME.$SEARCH_INPUT_ID") ~
      get[LocalDateTime](s"$TABLE_NAME.$LAST_UPDATE")) map {
        case id ~
          upDownType ~
          boostMalusValue ~
          term ~
          status ~
          searchInputId ~
          lastUpdate =>
          UpDownRuleExport(id,
            upDownType,
            boostMalusValue,
            term,
            status,
            searchInputId,
            lastUpdate)
      }
  }

  val selectAllStatement : String = {
    s"select $TABLE_NAME.$ID, " +
    s"$TABLE_NAME.$UP_DOWN_TYPE, " +
    s"$TABLE_NAME.$BOOST_MALUS_VALUE, " +
    s"$TABLE_NAME.$TERM, " +
    s"$TABLE_NAME.$STATUS, " +
    s"$TABLE_NAME.$SEARCH_INPUT_ID, " +
    s"$TABLE_NAME.$LAST_UPDATE from $TABLE_NAME"
  }

  def selectStatement(id: String) : String = {
    this.selectAllStatement + " where search_input_id in (select id from search_input where solr_index_id = '" + id + "')"
  }

}