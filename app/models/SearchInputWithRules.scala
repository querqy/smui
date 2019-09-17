package models

import java.sql.Connection
import models.rules._
import play.api.libs.json.{Json, OFormat}

case class SearchInputWithRules(id: SearchInputId,
                                term: String,
                                synonymRules: List[SynonymRule] = Nil,
                                upDownRules: List[UpDownRule] = Nil,
                                filterRules: List[FilterRule] = Nil,
                                deleteRules: List[DeleteRule] = Nil,
                                redirectRules: List[RedirectRule] = Nil,
                                tags: Seq[InputTag] = Seq.empty) {

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
        tags = TagInputAssociation.loadTagsBySearchInputId(id))
    }
  }

  def update(searchInput: SearchInputWithRules)(implicit connection: Connection): Unit = {
    SearchInput.update(searchInput.id, searchInput.term)

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