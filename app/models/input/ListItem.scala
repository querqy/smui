package models.input

import models.input.ListItemType.ListItemType
import models.spellings.{CanonicalSpelling, CanonicalSpellingWithAlternatives}
import play.api.libs.json.{Format, Json, OFormat}
import services.RulesUsage

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
                    additionalTermsForSearch: Seq[String] = Seq.empty,
                    usageFrequency: Option[Int] = None)

object ListItem {

  def create(searchInputs: Seq[SearchInputWithRules],
             spellings: Seq[CanonicalSpellingWithAlternatives],
             optRuleUsageStatistics: Option[Seq[RulesUsage]]): Seq[ListItem] = {
    val listItems = listItemsForRules(searchInputs) ++ listItemsForSpellings(spellings)
    // augment with usage statistics, only if available, pass through otherwise
    val listItemsWithUsageStatistics = optRuleUsageStatistics match {
      case Some(rulesUsage) if rulesUsage.isEmpty => listItems
      case Some(ruleUsage)                        => augmentRulesWithUsage(listItems, ruleUsage)
      case None                                   => listItems
    }
    listItemsWithUsageStatistics.sortBy(_.term.trim.toLowerCase.replace("\"", ""))
  }

  private def augmentRulesWithUsage(listItems: Seq[ListItem], ruleUsage: Seq[RulesUsage]): Seq[ListItem] = {
    // there can be multiple rule usage items for the the same rule, one per keyword combination that triggered the usage
    val combinedRuleUsageFrequency = ruleUsage.groupBy(_.inputId.id).view.mapValues(_.map(_.frequency).sum)
    listItems.map { listItem =>
      listItem.copy(usageFrequency = combinedRuleUsageFrequency.get(listItem.id))
    }
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
