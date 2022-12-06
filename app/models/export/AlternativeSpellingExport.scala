package models.`export`

import anorm.SqlParser.get
import anorm.{RowParser, ~}
import models.`export`.JsonExportable
import models.input.SearchInputId
import models.rules.{CommonRuleFields, DeleteRuleId}
import models.spellings.{AlternativeSpellingId, CanonicalSpellingId}
import models.{SolrIndexId, Status, `export`}
import play.api.libs.json._

import java.time.{LocalDateTime, ZoneOffset}

case class AlternativeSpellingExport(id: AlternativeSpellingId = AlternativeSpellingId(),
                                     canonicalSpellingId: CanonicalSpellingId,
                                     term: String,
                                     status: Int,
                                     lastUpdate: LocalDateTime) extends JsonExportable {

  def getTableName: JsString = JsString("alternative_spelling")

  def getColumns: JsValue = {
    JsArray(
      IndexedSeq (
        JsString("id"),
        JsString("canonical_spelling_id"),
        JsString("term"),
        JsString("status"),
        JsString("last_update")
      )
    )
  }

  def getRow: JsValue = {
    JsArray(
      IndexedSeq (
        JsString(id.toString),
        JsString(canonicalSpellingId.toString),
        JsString(term),
        JsNumber(status),
        JsString(lastUpdate.toString)
      )
    )
  }

}

object AlternativeSpellingExport {

  val TABLE_NAME = "alternative_spelling"
  val ID = "id"
  val CANONICAL_SPELLING_ID = "canonical_spelling_id"
  val TERM = "term"
  val STATUS = "status"
  val LAST_UPDATE = "last_update"

  val sqlParser: RowParser[AlternativeSpellingExport] = {
    get[AlternativeSpellingId](s"$TABLE_NAME.$ID") ~
      get[CanonicalSpellingId](s"$TABLE_NAME.$CANONICAL_SPELLING_ID") ~
      get[String](s"$TABLE_NAME.$TERM") ~
      get[Int](s"$TABLE_NAME.$STATUS") ~
      get[LocalDateTime](s"$TABLE_NAME.$LAST_UPDATE") map {
      case id ~ canonicalSpellingId ~ term ~ status ~ lastUpdate =>
        AlternativeSpellingExport(id, canonicalSpellingId, term, status, lastUpdate)
    }
  }

  val selectAllStatement : String = {
    s"select $TABLE_NAME.$ID, " +
      s"$TABLE_NAME.$CANONICAL_SPELLING_ID, " +
      s"$TABLE_NAME.$TERM, " +
      s"$TABLE_NAME.$STATUS, " +
      s"$TABLE_NAME.$LAST_UPDATE from $TABLE_NAME"
  }

  def selectStatement(id: String) : String = {
    this.selectAllStatement + s" where canonical_spelling_id in (select id from canonical_spelling where solr_index_id = '" + id + "')"
  }
}