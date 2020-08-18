package models.reports

import java.sql.Connection

import play.api.libs.json.{Json, OFormat}
import play.api.Logging
import models.SolrIndexId
import models.input.SearchInput
import models.spellings.CanonicalSpellingWithAlternatives

case class RulesReportItem(
  rule: String,
  isActive: Boolean,
  input: String
)

case class RulesReport(items: Seq[RulesReportItem])

object RulesReport extends Logging {

  implicit val jsonFormatRulesReportItem: OFormat[RulesReportItem] = Json.format[RulesReportItem]
  implicit val jsonFormatRulesReport: OFormat[RulesReport] = Json.format[RulesReport]

  def loadForSolrIndexId(solrIndexId: SolrIndexId)(implicit connection: Connection): RulesReport = {

    val searchInputs = SearchInput.loadAllForIndex(solrIndexId)
    val spellings = CanonicalSpellingWithAlternatives.loadAllForIndex(solrIndexId)

    val reportItems = searchInputs.map(i => {
      RulesReportItem(
        rule = "Dummy",
        isActive = true,
        input = i.term
      )
    })

    // return report

    RulesReport(
      items = reportItems
    )
  }

}
