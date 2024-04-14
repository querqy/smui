package models.reports

import models.input.{SearchInput, SearchInputId, SearchInputWithRules}
import play.api.Logging
import play.api.libs.json.{Json, OFormat}
import services.RulesUsage

case class RulesUsageReportEntry(
  // the search input ID from the report
  searchInputId: String,
  // the stored search input term if it could be found in the DB
  searchInputTerm: Option[String],
  // the user keywords/query that the search input was triggered on
  keywords: String,
  // the frequency that rule was triggered for the particular keywords
  frequency: Int
)

case class RulesUsageReport(items: Seq[RulesUsageReportEntry])

object RulesUsageReport extends Logging {

  implicit val jsonFormatRulesUsageReportEntry: OFormat[RulesUsageReportEntry] = Json.format[RulesUsageReportEntry]
  implicit val jsonFormatRulesUsageReport: OFormat[RulesUsageReport] = Json.format[RulesUsageReport]

  def create(searchInputs: Seq[SearchInputWithRules], rulesUsageStatistics: Seq[RulesUsage]): RulesUsageReport = {
    // perform a "full outer join" of the rules usage with the existing search inputs
    val searchInputsById = searchInputs.map(searchInput => searchInput.id.id -> searchInput).toMap
    val searchInputIdsFromAnalytics = rulesUsageStatistics.map(_.inputId.id).toSet
    val searchInputIdsNotFound = searchInputIdsFromAnalytics -- searchInputsById.keySet
    val searchInputIdsNotUsed = searchInputsById.keySet -- searchInputIdsFromAnalytics
    logger.info(s"Creating report from ${searchInputIdsFromAnalytics.size} used search inputs" +
      s" and ${searchInputsById.size} search inputs currently configured" +
      s" with ${searchInputIdsNotFound.size} search inputs not found" +
      s" and ${searchInputIdsNotUsed.size} search inputs not used")

    val reportEntriesUsedSearchInputs = rulesUsageStatistics.map { rulesUsage: RulesUsage =>
      RulesUsageReportEntry(
        rulesUsage.inputId.id,
        searchInputsById.get(rulesUsage.inputId.id).map(_.term),
        rulesUsage.keywords,
        rulesUsage.frequency
      )
    }
    val reportEntriesUnusedSearchInputs = searchInputIdsNotUsed.map { searchInputId =>
      RulesUsageReportEntry(
        searchInputId,
        searchInputsById.get(searchInputId).map(_.term),
        "",
        0
      )
    }
    RulesUsageReport(reportEntriesUsedSearchInputs ++ reportEntriesUnusedSearchInputs)
  }

}
