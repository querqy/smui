package models.spellings

import java.sql.Connection
import java.time.LocalDateTime

import models.SolrIndexId
import models.input.{FullSearchInputWithRules, SearchInputId}
import play.api.libs.json.{Json, OFormat}

/**
  * Needed for JSON persistence in eventhistory (especially for DELETED events). Readonly!!
  */
// TODO maybe define an abstract interface (@see /smui/app/models/input/FullSearchInputWithRules.scala)
case class FullCanonicalSpellingWithAlternatives(id: CanonicalSpellingId,
                                                 term: String,
                                                 isActive: Boolean,
                                                 comment: String,
                                                 solrIndexId: SolrIndexId,
                                                 lastUpdate: LocalDateTime,
                                                 alternativeSpellings: List[AlternativeSpelling] = Nil
                                                ) {

}

object FullCanonicalSpellingWithAlternatives {

  implicit val jsonFormat: OFormat[FullCanonicalSpellingWithAlternatives] = Json.format[FullCanonicalSpellingWithAlternatives]

  def loadById(id: CanonicalSpellingId)(implicit connection: Connection): Option[FullCanonicalSpellingWithAlternatives] = {
    CanonicalSpelling.loadById(id).map { canonicalSpelling =>
      FullCanonicalSpellingWithAlternatives(
        canonicalSpelling.id,
        canonicalSpelling.term,
        canonicalSpelling.isActive,
        canonicalSpelling.comment,
        solrIndexId = canonicalSpelling.solrIndexId,
        lastUpdate = canonicalSpelling.lastUpdate,
        alternativeSpellings = AlternativeSpelling.loadByCanonicalId(id)
      )
    }
  }

}