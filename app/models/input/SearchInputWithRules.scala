package models.input

import java.sql.Connection

import models.rules._
import models.SolrIndexId
import play.api.libs.json.{Json, OFormat}

case class SearchInputWithRules(id: SearchInputId,
                                term: String,
                                synonymRules: List[SynonymRule] = Nil,
                                upDownRules: List[UpDownRule] = Nil,
                                filterRules: List[FilterRule] = Nil,
                                deleteRules: List[DeleteRule] = Nil,
                                redirectRules: List[RedirectRule] = Nil,
                                tags: Seq[InputTag] = Seq.empty,
                                isActive: Boolean,
                                comment: String) {

  lazy val trimmedTerm: String = term.trim()

  def allRules: List[Rule] = {
    synonymRules ++ upDownRules ++ filterRules ++ deleteRules ++ redirectRules
  }

  def hasAnyActiveRules: Boolean = {
    allRules.exists(r => r.isActive)
  }

}

object SearchInputWithRules {

  implicit val jsonFormat: OFormat[SearchInputWithRules] = Json.format[SearchInputWithRules]

  def loadById(id: SearchInputId)(implicit connection: Connection): Option[SearchInputWithRules] = {
    SearchInput.loadById(id).map { input =>
      SearchInputWithRules(input.id, input.term,
        synonymRules = SynonymRule.loadByInputId(id),
        upDownRules = UpDownRule.loadByInputId(id),
        filterRules = FilterRule.loadByInputId(id),
        deleteRules = DeleteRule.loadByInputId(id),
        redirectRules = RedirectRule.loadByInputId(id),
        tags = TagInputAssociation.loadTagsBySearchInputId(id),
        isActive = input.isActive,
        comment = input.comment)
    }
  }

  /**
    * For displaying a list of search inputs with only some properties set.
    */
  def loadWithUndirectedSynonymsAndTagsForSolrIndexId(solrIndexId: SolrIndexId)(implicit connection: Connection): List[SearchInputWithRules] = {
    val inputs = SearchInput.loadAllForIndex(solrIndexId)
    val rules = SynonymRule.loadUndirectedBySearchInputIds(inputs.map(_.id))
    val tags = TagInputAssociation.loadTagsBySearchInputIds(inputs.map(_.id))

    inputs.map { input =>
      SearchInputWithRules(input.id, input.term,
        synonymRules = rules.getOrElse(input.id, Nil).toList,
        tags = tags.getOrElse(input.id, Seq.empty),
        isActive = input.isActive,
        comment = input.comment) // TODO consider only transferring "hasComment" for list overview
    }
  }

  def update(searchInput: SearchInputWithRules)(implicit connection: Connection): Unit = {
    SearchInput.update(searchInput.id, searchInput.term, searchInput.isActive, searchInput.comment)

    SynonymRule.updateForSearchInput(searchInput.id, searchInput.synonymRules)
    UpDownRule.updateForSearchInput(searchInput.id, searchInput.upDownRules)
    FilterRule.updateForSearchInput(searchInput.id, searchInput.filterRules)
    DeleteRule.updateForSearchInput(searchInput.id, searchInput.deleteRules)
    RedirectRule.updateForSearchInput(searchInput.id, searchInput.redirectRules)

    TagInputAssociation.updateTagsForSearchInput(searchInput.id, searchInput.tags.map(_.id))
  }

  def delete(id: SearchInputId)(implicit connection: Connection): Int = {
    val deleted = SearchInput.delete(id)
    if (deleted > 0) {
      for (rule <- Rule.allRules) {
        rule.deleteBySearchInput(id)
      }
      TagInputAssociation.deleteBySearchInputId(id)
    }
    deleted
  }

}