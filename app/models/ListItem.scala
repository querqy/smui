package models

import models.ListItemType.ListItemType
import models.spellings.{CanonicalSpelling, CanonicalSpellingWithAlternatives}
import play.api.libs.json.{Format, Json, OFormat}

object ListItemType extends Enumeration {
  type ListItemType = Value
  val RuleManagement, Spelling = Value

  implicit val jsonFormat: Format[models.ListItemType.Value] = Json.formatEnum(this)
}

case class ListItem(id: String,
                    term: String,
                    itemType: ListItemType,
                    isActive: Boolean = true,
                    synonyms: Seq[String] = Seq.empty,
                    tags: Seq[InputTag] = Seq.empty,
                    comment: String = "")

object ListItem {
  def createFromRulesAndSpellings(searchInputs: Seq[SearchInputWithRules], spellings: Seq[CanonicalSpelling]): Seq[ListItem] = {
    val listItems =
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
      } ++
      spellings.map { spelling =>
        ListItem(
          spelling.id.toString,
          spelling.term,
          ListItemType.Spelling,
        )
      }

    listItems.sortBy(_.term)
  }

  implicit val jsonFormat: OFormat[ListItem] = Json.format[ListItem]
}

case class SearchRulesAndSpellingsForList(items: Seq[ListItem])