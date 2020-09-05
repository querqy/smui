package models.input

import java.sql.Connection
import java.time.LocalDateTime

import play.api.libs.json.{Json, OFormat}
import models.SolrIndexId
import models.rules._

/**
  * Needed for JSON persistence in eventhistory (especially for DELETED events). Readonly!!
  */
// TODO maybe define an abstract ASearchInput interface for shared attributes
case class FullSearchInputWithRules(id: SearchInputId,
                                    term: String,
                                    synonymRules: List[SynonymRule] = Nil,
                                    upDownRules: List[UpDownRule] = Nil,
                                    filterRules: List[FilterRule] = Nil,
                                    deleteRules: List[DeleteRule] = Nil,
                                    redirectRules: List[RedirectRule] = Nil,
                                    tags: Seq[InputTag] = Seq.empty,
                                    isActive: Boolean,
                                    comment: String,
                                    solrIndexId: SolrIndexId,
                                    lastUpdate: LocalDateTime) {

}

object FullSearchInputWithRules {

  implicit val jsonFormat: OFormat[FullSearchInputWithRules] = Json.format[FullSearchInputWithRules]

  def loadById(id: SearchInputId)(implicit connection: Connection): Option[FullSearchInputWithRules] = {
    SearchInput.loadById(id).map { input =>
        FullSearchInputWithRules(
          input.id,
          input.term,
          synonymRules = SynonymRule.loadByInputId(id),
          upDownRules = UpDownRule.loadByInputId(id),
          filterRules = FilterRule.loadByInputId(id),
          deleteRules = DeleteRule.loadByInputId(id),
          redirectRules = RedirectRule.loadByInputId(id),
          tags = TagInputAssociation.loadTagsBySearchInputId(id),
          isActive = input.isActive,
          comment = input.comment,
          solrIndexId = input.solrIndexId,
          lastUpdate = input.lastUpdate
        )
    }
  }

}

