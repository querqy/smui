//CJM 5
package models.input

import models.input.ListItemType.ListItemType
import models.spellings.{CanonicalSpelling, CanonicalSpellingWithAlternatives}
import play.api.libs.json.{Format, Json, OFormat}

object ListItemType extends Enumeration {
  type ListItemType = Value
  val RuleManagement, Spelling = Value

  implicit val jsonFormat: Format[models.input.ListItemType.Value] = Json.formatEnum(this)
}

case class ListItem(id: String,
                    term: String,
                    itemType: ListItemType,
                    isActive: Boolean = true,
                    synonyms: Seq[String] = Seq.empty,
                    tags: Seq[InputTag] = Seq.empty,
                    comment: String = "",
                    additionalTermsForSearch: Seq[String] = Seq.empty)

object ListItem {
  def create(searchInputs: Seq[SearchInputWithRules], spellings: Seq[CanonicalSpellingWithAlternatives]): Seq[ListItem] = {
    val listItems = listItemsForRules(searchInputs) ++ listItemsForSpellings(spellings)
    listItems.sortBy(_.term.trim.toLowerCase.replace("\"", ""))
  }

  private def listItemsForRules(searchInputs: Seq[SearchInputWithRules]): Seq[ListItem] = {
    searchInputs.map { searchInput =>
      val synonyms = searchInput.synonymRules
        .filter(rule => rule.isActive && rule.synonymType == 0 )
        .map(_.term)

      ListItem(
        searchInput.id.toString,
        searchInput.term,
        ListItemType.RuleManagement,
        searchInput.isActive,
        synonyms,
        searchInput.tags,
        searchInput.comment
      )
    }
  }

  private def listItemsForSpellings(spellings: Seq[CanonicalSpellingWithAlternatives]): Seq[ListItem] = {
    spellings.map { spelling =>
      ListItem(
        spelling.id.toString,
        spelling.term,
        ListItemType.Spelling,
        spelling.isActive,
        additionalTermsForSearch = spelling.alternativeSpellings.map(_.term),
        comment = spelling.comment
      )
    }
  }

  implicit val jsonFormat: OFormat[ListItem] = Json.format[ListItem]
}

case class SearchRulesAndSpellingsForList(items: Seq[ListItem])
