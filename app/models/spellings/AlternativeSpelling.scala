package models.spellings

import java.sql.Connection
import java.time.LocalDateTime

import anorm.SqlParser.get
import anorm._
import play.api.libs.json.{Json, OFormat}
import models.{Id, IdObject, Status}

class AlternativeSpellingId(id: String) extends Id(id)

object AlternativeSpellingId extends IdObject[AlternativeSpellingId](new AlternativeSpellingId(_))

case class AlternativeSpelling(id: AlternativeSpellingId = AlternativeSpellingId(),
                               canonicalSpellingId: CanonicalSpellingId,
                               term: String,
                               isActive: Boolean) {

  import AlternativeSpelling._

  def toNamedParameters: Seq[NamedParameter] = Seq(
    ID -> id,
    CANONICAL_SPELLING_ID -> canonicalSpellingId,
    TERM -> term,
    LAST_UPDATE -> LocalDateTime.now(),
    STATUS -> Status.statusFromIsActive(isActive)
  )
}

object AlternativeSpelling {

  val TABLE_NAME = "alternative_spelling"
  val ID = "id"
  val CANONICAL_SPELLING_ID = "canonical_spelling_id"
  val TERM = "term"
  val LAST_UPDATE = "last_update"
  val STATUS = "status"

  val orderByField: String = TERM

  implicit val jsonFormat: OFormat[AlternativeSpelling] = Json.format[AlternativeSpelling]

  val sqlParser: RowParser[AlternativeSpelling] = {
      get[AlternativeSpellingId](s"$ID") ~
      get[CanonicalSpellingId](s"$CANONICAL_SPELLING_ID") ~
      get[String](s"$TERM") ~
      get[Int](s"$STATUS") map { case id ~ canonicalSpellingId ~ term ~ status =>
      AlternativeSpelling(id, canonicalSpellingId, term, Status.isActiveFromStatus(status))
    }
  }

  val insertStatement: String =
    s"insert into $TABLE_NAME ($ID, $CANONICAL_SPELLING_ID, $TERM, $STATUS, $LAST_UPDATE) " +
      s"values ({$ID}, {$CANONICAL_SPELLING_ID}, {$TERM}, {$STATUS}, {$LAST_UPDATE})"

  def loadByCanonicalId(canonicalSpellingId: CanonicalSpellingId)(implicit connection: Connection): List[AlternativeSpelling] = {
    SQL"select * from #$TABLE_NAME where #$CANONICAL_SPELLING_ID = $canonicalSpellingId order by lower(#$orderByField)".as(sqlParser.*)
  }

  def updateForCanonicalSpelling(canonicalSpellingId: CanonicalSpellingId, alternativeSpellings: List[AlternativeSpelling])(implicit connection: Connection): Unit = {
    SQL"delete from #$TABLE_NAME where #$CANONICAL_SPELLING_ID = $canonicalSpellingId".execute()

    if (alternativeSpellings.nonEmpty) {
      BatchSql(
        insertStatement,
        alternativeSpellings.head.toNamedParameters,
        alternativeSpellings.tail.map(_.toNamedParameters): _*
      ).execute()
    }
  }

  def deleteByCanonicalSpelling(canonicalSpellingId: CanonicalSpellingId)(implicit connection: Connection): Int = {
    SQL"delete from #$TABLE_NAME where #$CANONICAL_SPELLING_ID = $canonicalSpellingId".executeUpdate()
  }

  def loadByCanonicalSpellingIds(ids: Seq[CanonicalSpellingId])(implicit connection: Connection): Map[CanonicalSpellingId, Seq[AlternativeSpelling]] = {
    ids.grouped(100).toSeq.flatMap { idGroup =>
      SQL"select * from #$TABLE_NAME where #$CANONICAL_SPELLING_ID in ($idGroup)".as((sqlParser ~ get[CanonicalSpellingId](CANONICAL_SPELLING_ID)).*).map { case alternativeSpelling ~ id =>
        id -> alternativeSpelling
      }
    }.groupBy(_._1).mapValues(_.map(_._2))
  }
}
