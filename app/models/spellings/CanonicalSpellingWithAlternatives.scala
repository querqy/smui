package models.spellings

import java.sql.Connection

import models.SolrIndexId
import play.api.libs.json.{Json, OFormat}

case class CanonicalSpellingWithAlternatives(id: CanonicalSpellingId,
                                             term: String,
                                             alternateSpellings: List[AlternateSpelling] = Nil) {

  def exportToReplaceFile: Boolean = {
    this.term.nonEmpty && alternateSpellings.nonEmpty
  }
}

object CanonicalSpellingWithAlternatives {

  implicit val jsonFormat: OFormat[CanonicalSpellingWithAlternatives] = Json.format[CanonicalSpellingWithAlternatives]

  def loadAllForIndex(solrIndexId: SolrIndexId)(implicit connection: Connection): List[CanonicalSpellingWithAlternatives] = {
    val canonicalSpellings = CanonicalSpelling.loadAllForIndex(solrIndexId)
    val alternateSpellings = AlternateSpelling.loadByCanonicalSpellingIds(canonicalSpellings.map(_.id))

    canonicalSpellings.map { canonicalSpelling =>
      CanonicalSpellingWithAlternatives(
        canonicalSpelling.id, canonicalSpelling.term,
        alternateSpellings.getOrElse(canonicalSpelling.id, Seq.empty).toList
      )
    }
  }

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
