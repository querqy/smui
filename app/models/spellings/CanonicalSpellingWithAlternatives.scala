package models.spellings

import java.sql.Connection

import models.SolrIndexId
import play.api.libs.json.{Json, OFormat}

case class CanonicalSpellingWithAlternatives(id: CanonicalSpellingId,
                                             term: String,
                                             alternativeSpellings: List[AlternativeSpelling] = Nil) {

  def exportToReplaceFile: Boolean = {
    this.term.nonEmpty && alternativeSpellings.nonEmpty
  }
}

object CanonicalSpellingWithAlternatives {

  implicit val jsonFormat: OFormat[CanonicalSpellingWithAlternatives] = Json.format[CanonicalSpellingWithAlternatives]

  def loadAllForIndex(solrIndexId: SolrIndexId)(implicit connection: Connection): List[CanonicalSpellingWithAlternatives] = {
    val canonicalSpellings = CanonicalSpelling.loadAllForIndex(solrIndexId)
    val alternativeSpellings = AlternativeSpelling.loadByCanonicalSpellingIds(canonicalSpellings.map(_.id))

    canonicalSpellings.map { canonicalSpelling =>
      CanonicalSpellingWithAlternatives(
        canonicalSpelling.id, canonicalSpelling.term,
        alternativeSpellings.getOrElse(canonicalSpelling.id, Seq.empty).toList
      )
    }
  }

  def loadById(id: CanonicalSpellingId)(implicit connection: Connection): Option[CanonicalSpellingWithAlternatives] = {
    CanonicalSpelling.loadById(id).map { canonicalSpelling =>
      CanonicalSpellingWithAlternatives(
        canonicalSpelling.id,
        canonicalSpelling.term,
        AlternativeSpelling.loadByCanonicalId(id)
      )
    }
  }

  def update(spellingWithAlternatives: CanonicalSpellingWithAlternatives)(implicit connection: Connection): Unit = {
    CanonicalSpelling.update(spellingWithAlternatives.id, spellingWithAlternatives.term)
    AlternativeSpelling.updateForCanonicalSpelling(spellingWithAlternatives.id, spellingWithAlternatives.alternativeSpellings)
  }

  def delete(id: CanonicalSpellingId)(implicit connection: Connection): Int = {
    val deleted = CanonicalSpelling.delete(id)
    if (deleted > 1) {
      AlternativeSpelling.deleteByCanonicalSpelling(id)
    }
    deleted
  }

}
