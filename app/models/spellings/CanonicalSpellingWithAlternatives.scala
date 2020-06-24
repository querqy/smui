package models.spellings

import java.sql.Connection

import play.api.libs.json.{Json, OFormat}

case class CanonicalSpellingWithAlternatives(id: CanonicalSpellingId,
                                             term: String,
                                             alternateSpellings: List[AlternateSpelling] = Nil)

object CanonicalSpellingWithAlternatives {

  implicit val jsonFormat: OFormat[CanonicalSpellingWithAlternatives] = Json.format[CanonicalSpellingWithAlternatives]

  def loadById(id: CanonicalSpellingId)(implicit connection: Connection): Option[CanonicalSpellingWithAlternatives] = {
    CanonicalSpelling.loadById(id).map { canonicalSpelling =>
      CanonicalSpellingWithAlternatives(
        canonicalSpelling.id,
        canonicalSpelling.term,
        AlternateSpelling.loadByCanonicalId(id)
      )
    }
  }

  def update(spellingWithAlternatives: CanonicalSpellingWithAlternatives)(implicit connection: Connection): Unit = {
    CanonicalSpelling.update(spellingWithAlternatives.id, spellingWithAlternatives.term)
    AlternateSpelling.updateForCanonicalSpelling(spellingWithAlternatives.id, spellingWithAlternatives.alternateSpellings)
  }

  def delete(id: CanonicalSpellingId)(implicit connection: Connection): Int = {
    val deleted = CanonicalSpelling.delete(id)
    if (deleted > 1) {
      AlternateSpelling.deleteByCanonicalSpelling(id)
    }
    deleted
  }

}
