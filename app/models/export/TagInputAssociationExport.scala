package models.`export`

import anorm.SqlParser.get
import anorm.{RowParser, SQL, ~}
import models.input.InputTagId
import models.rules.CommonRuleFields
import play.api.libs.json._

import java.time.LocalDateTime

case class  TagInputAssociationExport(id: InputTagId,
                                      searchInputId: String,
                                      lastUpdate: LocalDateTime) extends JsonExportable {

  def getTableName: JsString = JsString("tag_2_input")

  def getColumns: JsValue = {
    JsArray(
      IndexedSeq (
        JsString("id"),
        JsString("searchInputId"),
        JsString("last_update")
      )
    )
  }

  def getRow: JsValue = {
    JsArray(
      IndexedSeq (
        JsString(id.toString),
        JsString(searchInputId),
        JsString(lastUpdate.toString)
      )
    )
  }
}

object TagInputAssociationExport  {

  val TABLE_NAME = "tag_2_input"
  val ID = "tag_id"
  val SEARCH_INPUT_ID = "input_id"
  val LAST_UPDATE = "last_update"

  val sqlParser: RowParser[TagInputAssociationExport] = {
    (get[InputTagId](s"$TABLE_NAME.$ID") ~
      get[String](s"$TABLE_NAME.$SEARCH_INPUT_ID") ~
      get[LocalDateTime](s"$TABLE_NAME.$LAST_UPDATE")) map {
        case
          id ~
          searchInputId ~
          lastUpdate
        =>
        TagInputAssociationExport(
          id,
          searchInputId,
          lastUpdate)
    }
  }

  val selectAllStatement : String = {
    s"select $TABLE_NAME.$ID, " +
    s"$TABLE_NAME.$SEARCH_INPUT_ID, " +
    s"$TABLE_NAME.$LAST_UPDATE from $TABLE_NAME"
  }

  def selectStatement(id: String) : String = {
    this.selectAllStatement + s" where $TABLE_NAME.$SEARCH_INPUT_ID in (select id from search_input where solr_index_id = '" + id + "')"
  }

}