package models.reports

import play.api.libs.json.{Json, OFormat}
import play.api.Logging

import models.SolrIndexId

case class RulesReportItem(rule: String)

case class RulesReport(items: Seq[RulesReportItem])

object RulesReport extends Logging {

  implicit val jsonFormatRulesReportItem: OFormat[RulesReportItem] = Json.format[RulesReportItem]
  implicit val jsonFormatRulesReport: OFormat[RulesReport] = Json.format[RulesReport]

  def loadForSolrIndexId(solrIndexId: SolrIndexId): RulesReport = {
    RulesReport(
      items = List(
        RulesReportItem("Hello"),
        RulesReportItem("Rule Report")
      )
    )
  }

}
