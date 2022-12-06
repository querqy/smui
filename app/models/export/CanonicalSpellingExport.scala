package models.`export`

import anorm.SqlParser.get
import anorm.{RowParser, ~}
import models.`export`.JsonExportable
import models.input.SearchInputId
import models.rules.{CommonRuleFields, DeleteRuleId}
import models.spellings.CanonicalSpellingId
import models.{SolrIndexId, Status, `export`}
import play.api.libs.json._

import java.time.LocalDateTime

case class CanonicalSpellingExport(id: CanonicalSpellingId = CanonicalSpellingId(),
                                   solrIndexId: SolrIndexId,
                                   term: String,
                                   status: Int,
                                   comment: String,
                                   lastUpdate: LocalDateTime) extends JsonExportable {

  def getTableName: JsString = JsString("canonical_spelling")

  def getColumns: JsValue = {
    JsArray(
      IndexedSeq (
        JsString("id"),
        JsString("solr_index_id"),
        JsString("term"),
        JsString("status"),
        JsString("comment"),
        JsString("last_update")
      )
    )
  }

  def getRow: JsValue = {
    JsArray(
      IndexedSeq (
        JsString(id.toString),
        JsString(solrIndexId.toString),
        JsString(term),
        JsNumber(status),
        JsString(comment),
        JsString(lastUpdate.toString)
      )
    )
  }

}

object CanonicalSpellingExport {

  val TABLE_NAME = "canonical_spelling"
  val ID = "id"
  val SOLR_INDEX_ID = "solr_index_id"
  val TERM = "term"
  val STATUS = "status"
  val COMMENT = "comment"
  val LAST_UPDATE = "last_update"

  val sqlParser: RowParser[CanonicalSpellingExport] = {
    get[CanonicalSpellingId](s"$TABLE_NAME.$ID") ~
      get[SolrIndexId](s"$TABLE_NAME.$SOLR_INDEX_ID") ~
      get[String](s"$TABLE_NAME.$TERM") ~
      get[Int](s"$TABLE_NAME.$STATUS") ~
      get[String](s"$TABLE_NAME.$COMMENT") ~
      get[LocalDateTime](s"$TABLE_NAME.$LAST_UPDATE") map {
      case id ~ solrIndexId ~ term ~ status ~ comment ~ lastUpdate =>
      CanonicalSpellingExport(id, solrIndexId, term, status, comment, lastUpdate)
    }
  }

  val selectAllStatement : String = {
    s"select $TABLE_NAME.$ID, " +
      s"$TABLE_NAME.$SOLR_INDEX_ID, " +
      s"$TABLE_NAME.$TERM, " +
      s"$TABLE_NAME.$STATUS, " +
      s"$TABLE_NAME.$COMMENT, " +
      s"$TABLE_NAME.$LAST_UPDATE from $TABLE_NAME"
  }

  def selectStatement(id: String) : String = {
    this.selectAllStatement + " where solr_index_id = '" + id + "'"
  }
}