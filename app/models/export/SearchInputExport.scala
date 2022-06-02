package models.`export`

import anorm.SqlParser.get
import anorm._
import models._
import models.`export`.JsonExportable
import models.input.{SearchInput, SearchInputId}
import play.api.libs.json._

import java.time.LocalDateTime

case class SearchInputExport(id: SearchInputId,
                       solrIndexId: SolrIndexId,
                       term: String,
                       lastUpdate: LocalDateTime,
                       status: Int,
                       comment: String) extends JsonExportable {

  def getTableName: JsString = JsString("search_input")

  def getRow: JsValue = {
    JsArray(
      IndexedSeq (
        JsString(id.toString),
        JsString(term),
        JsString(solrIndexId.toString),
        JsString(lastUpdate.toString),
        JsNumber(status),
        JsString(comment)
      )
    )
  }

  def getColumns: JsValue = {
    JsArray(
      IndexedSeq (
        JsString("id"),
        JsString("term"),
        JsString("solr_index_id"),
        JsString("last_update"),
        JsString("status"),
        JsString("comment")
      )
    )
  }

}

object SearchInputExport {

  implicit val jsonFormat: OFormat[SearchInput] = Json.format[SearchInput]

  val TABLE_NAME = "search_input"
  val ID = "id"
  val TERM = "term"
  val SOLR_INDEX_ID = "solr_index_id"
  val LAST_UPDATE = "last_update"
  val STATUS = "status"
  val COMMENT = "comment"

  val sqlParser: RowParser[SearchInputExport] = {
    get[SearchInputId](s"$TABLE_NAME.$ID") ~
      get[String](s"$TABLE_NAME.$TERM") ~
      get[SolrIndexId](s"$TABLE_NAME.$SOLR_INDEX_ID") ~
      get[LocalDateTime](s"$TABLE_NAME.$LAST_UPDATE") ~
      get[Int](s"$TABLE_NAME.$STATUS") ~
      get[String](s"$TABLE_NAME.$COMMENT") map { case id ~ term ~ indexId ~ lastUpdate ~ status ~ comment =>
        models.`export`.SearchInputExport(id, indexId, term, lastUpdate, status, comment)
    }
  }

  val selectAllStatement = s"select id, term, solr_index_id, last_update, status, comment from search_input"

  def selectStatement(id: String) : String = {
    s"select id, term, solr_index_id, last_update, status, comment from search_input where solr_index_id = '" + id + "'"
  }

}
