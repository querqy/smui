package models.input

import java.sql.Connection
import java.time.LocalDateTime

import play.api.libs.json.{Json, OFormat}

import anorm.SqlParser.get
import anorm._

import models._

class SearchInputId(id: String) extends Id(id)
object SearchInputId extends IdObject[SearchInputId](new SearchInputId(_))

case class SearchInput(id: SearchInputId = SearchInputId(),
                       solrIndexId: SolrIndexId,
                       term: String,
                       lastUpdate: LocalDateTime,
                       isActive: Boolean,
                       comment: String) {

  import SearchInput._

  def status: Int = Status.statusFromIsActive(isActive)

  def toNamedParameters: Seq[NamedParameter] = Seq(
    ID -> id,
    SOLR_INDEX_ID -> solrIndexId,
    TERM -> term,
    LAST_UPDATE -> lastUpdate,
    STATUS -> status,
    COMMENT -> comment
  )

}

object SearchInput {

  implicit val jsonFormat: OFormat[SearchInput] = Json.format[SearchInput]

  val TABLE_NAME = "search_input"
  val ID = "id"
  val TERM = "term"
  val SOLR_INDEX_ID = "solr_index_id"
  val LAST_UPDATE = "last_update"
  val STATUS = "status"
  val COMMENT = "comment"

  val sqlParser: RowParser[SearchInput] = {
    get[SearchInputId](s"$ID") ~
      get[String](s"$TERM") ~
      get[SolrIndexId](s"$SOLR_INDEX_ID") ~
      get[LocalDateTime](s"$LAST_UPDATE") ~
      get[Int](s"$STATUS") ~
      get[String](s"$COMMENT") map { case id ~ term ~ indexId ~ lastUpdate ~ status ~ comment =>
        SearchInput(id, indexId, term, lastUpdate, Status.isActiveFromStatus(status), comment)
    }
  }

  def insert(solrIndexId: SolrIndexId, term: String)(implicit connection: Connection): SearchInput = {
    val input = SearchInput(SearchInputId(), solrIndexId, term, LocalDateTime.now(), true, "")
    SQL(s"insert into $TABLE_NAME ($ID, $TERM, $SOLR_INDEX_ID, $LAST_UPDATE, $STATUS, $COMMENT) values ({$ID}, {$TERM}, {$SOLR_INDEX_ID}, {$LAST_UPDATE}, {$STATUS}, {$COMMENT})")
      .on(input.toNamedParameters: _*).execute()
    input
  }

  def loadAllForIndex(solrIndexId: SolrIndexId)(implicit connection: Connection): List[SearchInput] = {
    SQL"select * from #$TABLE_NAME where #$SOLR_INDEX_ID = $solrIndexId order by #$TERM asc".as(sqlParser.*)
  }

  def loadAllIdsForIndex(solrIndexId: SolrIndexId)(implicit connection: Connection): List[SearchInputId] = {
    SQL"select #$ID from #$TABLE_NAME where #$SOLR_INDEX_ID = $solrIndexId order by #$TERM asc".as(get[SearchInputId](ID).*)
  }

  def loadById(id: SearchInputId)(implicit connection: Connection): Option[SearchInput] = {
    SQL"select * from #$TABLE_NAME where #$ID = $id".as(sqlParser.*).headOption
  }

  def update(id: SearchInputId, term: String, isActive: Boolean, comment: String)(implicit connection: Connection): Unit = {
    SQL"update #$TABLE_NAME set #$TERM = $term, #$LAST_UPDATE = ${LocalDateTime.now()}, #$STATUS = ${Status.statusFromIsActive(isActive)}, #$COMMENT = $comment where #$ID = $id".executeUpdate()
  }

  /**
    * Deletes the searchInput itself, not any rules belonging to it.
    * For also deleting rules, use SearchInputWithRules.delete
    */
  def delete(id: SearchInputId)(implicit connection: Connection): Int = {
    SQL"delete from #$TABLE_NAME where #$ID = $id".executeUpdate()
  }

}
