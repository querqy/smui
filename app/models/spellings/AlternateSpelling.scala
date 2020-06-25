package models.spellings

import java.sql.Connection
import java.time.LocalDateTime
import anorm.SqlParser.get
import anorm._
import play.api.libs.json.{Json, OFormat}
import models.{Id, IdObject}

class AlternateSpellingId(id: String) extends Id(id)

object AlternateSpellingId extends IdObject[AlternateSpellingId](new AlternateSpellingId(_))

case class AlternateSpelling(id: AlternateSpellingId = AlternateSpellingId(),
                             canonicalSpellingId: CanonicalSpellingId,
                             term: String) {

  import AlternateSpelling._

  def toNamedParameters: Seq[NamedParameter] = Seq(
    ID -> id,
    CANONICAL_SPELLING_ID -> canonicalSpellingId,
    TERM -> term,
    LAST_UPDATE -> LocalDateTime.now()
  )
}

object AlternateSpelling {

  val TABLE_NAME = "alternate_spelling"
  val ID = "id"
  val CANONICAL_SPELLING_ID = "canonical_spelling_id"
  val TERM = "term"
  val LAST_UPDATE = "last_update"

  val orderByField: String = TERM

  implicit val jsonFormat: OFormat[AlternateSpelling] = Json.format[AlternateSpelling]

  val sqlParser: RowParser[AlternateSpelling] = {
      get[AlternateSpellingId](s"$TABLE_NAME.$ID") ~
      get[CanonicalSpellingId](s"$TABLE_NAME.$CANONICAL_SPELLING_ID") ~
      get[String](s"$TABLE_NAME.$TERM") map { case id ~ canonicalSpellingId ~ term =>
      AlternateSpelling(id, canonicalSpellingId, term)
    }
  }

  val insertStatement: String =
    s"insert into $TABLE_NAME ($ID, $CANONICAL_SPELLING_ID, $TERM, $LAST_UPDATE) values ({$ID}, {$CANONICAL_SPELLING_ID}, {$TERM}, {$LAST_UPDATE})"

  def loadByCanonicalId(canonicalSpellingId: CanonicalSpellingId)(implicit connection: Connection): List[AlternateSpelling] = {
    SQL"select * from #$TABLE_NAME where #$CANONICAL_SPELLING_ID = $canonicalSpellingId order by #$orderByField".as(sqlParser.*)
  }

  def updateForCanonicalSpelling(canonicalSpellingId: CanonicalSpellingId, alternateSpellings: List[AlternateSpelling])(implicit connection: Connection): Unit = {
    SQL"delete from #$TABLE_NAME where #$CANONICAL_SPELLING_ID = $canonicalSpellingId".execute()

    if (alternateSpellings.nonEmpty) {
      BatchSql(
        insertStatement,
        alternateSpellings.head.toNamedParameters,
        alternateSpellings.tail.map(_.toNamedParameters): _*
      ).execute()
    }
  }

  def deleteByCanonicalSpelling(canonicalSpellingId: CanonicalSpellingId)(implicit connection: Connection): Int = {
    SQL"delete from #$TABLE_NAME where #$CANONICAL_SPELLING_ID = $canonicalSpellingId".executeUpdate()
  }

  def loadByCanonicalSpellingIds(ids: Seq[CanonicalSpellingId])(implicit connection: Connection): Map[CanonicalSpellingId, Seq[AlternateSpelling]] = {
    ids.grouped(100).toSeq.flatMap { idGroup =>
      SQL"select * from #$TABLE_NAME where #$CANONICAL_SPELLING_ID in ($idGroup)".as((sqlParser ~ get[CanonicalSpellingId](CANONICAL_SPELLING_ID)).*).map { case alternateSpelling ~ id =>
        id -> alternateSpelling
      }
    }.groupBy(_._1).mapValues(_.map(_._2))
  }
}
