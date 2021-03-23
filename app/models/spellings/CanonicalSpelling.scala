package models.spellings

import java.sql.Connection
import java.time.LocalDateTime

import anorm.SqlParser.get
import anorm._
import models.{Id, IdObject, SolrIndexId, Status}
import play.api.libs.json.{Json, OFormat}

class CanonicalSpellingId(id: String) extends Id(id)

object CanonicalSpellingId extends IdObject[CanonicalSpellingId](new CanonicalSpellingId(_))

case class CanonicalSpelling(id: CanonicalSpellingId,
                             solrIndexId: SolrIndexId,
                             term: String,
                             isActive: Boolean,
                             comment: String,
                             lastUpdate: LocalDateTime) {

  import CanonicalSpelling._

  def toNamedParameters: Seq[NamedParameter] = Seq(
    ID -> id,
    SOLR_INDEX_ID -> solrIndexId,
    TERM -> term,
    STATUS -> Status.statusFromIsActive(isActive),
    COMMENT -> comment,
    LAST_UPDATE -> lastUpdate
  )
}

object CanonicalSpelling {

  val TABLE_NAME = "canonical_spelling"
  val ID = "id"
  val SOLR_INDEX_ID = "solr_index_id"
  val TERM = "term"
  val STATUS = "status"
  val COMMENT = "comment"
  val LAST_UPDATE = "last_update"

  val orderByField: String = TERM

  implicit val jsonFormat: OFormat[CanonicalSpelling] = Json.format[CanonicalSpelling]

  val sqlParser: RowParser[CanonicalSpelling] = {
      get[CanonicalSpellingId](s"$ID") ~
      get[SolrIndexId](s"$SOLR_INDEX_ID") ~
      get[String](s"$TERM") ~
      get[Int](s"$STATUS") ~
      get[String](s"$COMMENT") ~
      get[LocalDateTime](s"$LAST_UPDATE") map { case id ~ indexId ~ term ~ status ~ comment ~ lastUpdate =>
      CanonicalSpelling(id, indexId, term, Status.isActiveFromStatus(status), comment, lastUpdate)
    }
  }

  def insert(solrIndexId: SolrIndexId, term: String)(implicit connection: Connection): CanonicalSpelling = {
    val canonicalSpelling = CanonicalSpelling(CanonicalSpellingId(), solrIndexId, term, true, "", LocalDateTime.now())
    SQL(s"insert into $TABLE_NAME ($ID, $SOLR_INDEX_ID, $TERM, $STATUS, $COMMENT, $LAST_UPDATE) " +
      s"values ({$ID}, {$SOLR_INDEX_ID}, {$TERM}, {$STATUS}, {$COMMENT},{$LAST_UPDATE})")
      .on(canonicalSpelling.toNamedParameters: _*).execute()
    canonicalSpelling
  }

  def loadById(id: CanonicalSpellingId)(implicit connection: Connection): Option[CanonicalSpelling] = {
    SQL"select * from #$TABLE_NAME where #$ID = $id".as(sqlParser.*).headOption
  }

  def loadAllForIndex(solrIndexId: SolrIndexId)(implicit connection: Connection): List[CanonicalSpelling] = {
    SQL"select * from #$TABLE_NAME where #$SOLR_INDEX_ID = $solrIndexId order by #$orderByField asc".as(sqlParser.*)
  }

  def update(id: CanonicalSpellingId, term: String, isActive: Boolean, comment: String)(implicit connection: Connection): Unit = {
    SQL"update #$TABLE_NAME set #$TERM = $term, #$STATUS = ${Status.statusFromIsActive(isActive)}, #$COMMENT = $comment, #$LAST_UPDATE = ${LocalDateTime.now()} where #$ID = $id".executeUpdate()
  }

  def delete(id: CanonicalSpellingId)(implicit connection: Connection): Int = {
    SQL"delete from #$TABLE_NAME where #$ID = $id".executeUpdate()
  }
}