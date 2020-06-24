package models.spellings

import java.sql.Connection
import java.time.LocalDateTime

import anorm.SqlParser.get
import anorm._
import models.{Id, IdObject, SolrIndexId}
import play.api.libs.json.{Json, OFormat}

class CanonicalSpellingId(id: String) extends Id(id)

object CanonicalSpellingId extends IdObject[CanonicalSpellingId](new CanonicalSpellingId(_))

case class CanonicalSpelling(id: CanonicalSpellingId,
                             solrIndexId: SolrIndexId,
                             term: String,
                             lastUpdate: LocalDateTime) {

  import CanonicalSpelling._

  def toNamedParameters: Seq[NamedParameter] = Seq(
    ID -> id,
    SOLR_INDEX_ID -> solrIndexId,
    TERM -> term,
    LAST_UPDATE -> lastUpdate
  )
}

object CanonicalSpelling {

  val TABLE_NAME = "canonical_spelling"
  val ID = "id"
  val SOLR_INDEX_ID = "solr_index_id"
  val TERM = "term"
  val LAST_UPDATE = "last_update"

  val orderByField: String = TERM

  implicit val jsonFormat: OFormat[CanonicalSpelling] = Json.format[CanonicalSpelling]

  val sqlParser: RowParser[CanonicalSpelling] = {
      get[CanonicalSpellingId](s"$TABLE_NAME.$ID") ~
      get[SolrIndexId](s"$TABLE_NAME.$SOLR_INDEX_ID") ~
      get[String](s"$TABLE_NAME.$TERM") ~
      get[LocalDateTime](s"$TABLE_NAME.$LAST_UPDATE") map { case id ~ indexId ~ term ~ lastUpdate =>
      CanonicalSpelling(id, indexId, term, lastUpdate)
    }
  }

  def insert(solrIndexId: SolrIndexId, term: String)(implicit connection: Connection): CanonicalSpelling = {
    val canonicalSpelling = CanonicalSpelling(CanonicalSpellingId(), solrIndexId, term, LocalDateTime.now())
    SQL(s"insert into $TABLE_NAME ($ID, $SOLR_INDEX_ID, $TERM, $LAST_UPDATE) values ({$ID}, {$SOLR_INDEX_ID}, {$TERM}, {$LAST_UPDATE})")
      .on(canonicalSpelling.toNamedParameters: _*).execute()
    canonicalSpelling
  }

  def loadById(id: CanonicalSpellingId)(implicit connection: Connection): Option[CanonicalSpelling] = {
    SQL"select * from #$TABLE_NAME where #$ID = $id".as(sqlParser.*).headOption
  }

  def loadAllForIndex(solrIndexId: SolrIndexId)(implicit connection: Connection): List[CanonicalSpelling] = {
    SQL"select * from #$TABLE_NAME where #$SOLR_INDEX_ID = $solrIndexId order by #$orderByField asc".as(sqlParser.*)
  }

  def update(id: CanonicalSpellingId, term: String)(implicit connection: Connection): Unit = {
    SQL"update #$TABLE_NAME set #$TERM = $term, #$LAST_UPDATE = ${LocalDateTime.now()} where #$ID = $id".executeUpdate()
  }

  def delete(id: CanonicalSpellingId)(implicit connection: Connection): Int = {
    SQL"delete from #$TABLE_NAME where #$ID = $id".executeUpdate()
  }
}