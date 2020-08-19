package models.reports

import java.sql.Connection
import java.time.LocalDateTime

import anorm._
import anorm.SqlParser.get

import play.api.libs.json.{Json, OFormat}
import play.api.Logging
import models.SolrIndexId

case class RulesReportItem(
  inputId: String, // TODO inputType needed?
  term: String,
  details: String,
  isActive: Boolean,
  modified: LocalDateTime,
  inputTerm: String,
  inputModified: LocalDateTime
)

case class RulesReport(items: Seq[RulesReportItem])

object RulesReport extends Logging {

  implicit val jsonFormatRulesReportItem: OFormat[RulesReportItem] = Json.format[RulesReportItem]
  implicit val jsonFormatRulesReport: OFormat[RulesReport] = Json.format[RulesReport]

  private def loadReportForTable(solrIndexId: SolrIndexId, tblRuleName: String, detailsDescr: String, termFieldName: String = "term", tblInputName: String = "search_input", refKeyFieldName: String = "search_input_id")(implicit connection: Connection): Seq[RulesReportItem] = {

    // TODO consider moving this to a dedicated Status model in /app/models/input (for SearchInput and Rule as well)
    def isActive(status: Int): Boolean = {
      (status & 0x01) == 0x01
    }

    val sqlParser: RowParser[RulesReportItem] = {
      get[String](s"$tblInputName.id") ~
        get[String](s"$tblRuleName.$termFieldName") ~
        get[Int](s"$tblRuleName.status") ~
        get[LocalDateTime](s"$tblRuleName.last_update") ~
        get[String](s"$tblInputName.term") ~
        get[Int](s"$tblInputName.status") ~
        get[LocalDateTime](s"$tblInputName.last_update") map { case inputId ~ ruleTerm ~ ruleStatus ~ ruleLastUpdate ~ inputTerm ~ inputStatus ~ inputLastUpdate =>
        RulesReportItem(
          inputId = inputId,
          term = ruleTerm,
          details = detailsDescr,
          isActive = isActive(ruleStatus) && isActive(inputStatus),
          modified = ruleLastUpdate,
          inputTerm = inputTerm,
          inputModified = inputLastUpdate
        )
      }
    }

    // TODO consider removing i and r for the real table names
    // TODO consider making SQL business logic part of rules/spelling entities (e.g. models/rules/SynonymRule.scala)
    // TODO referencing SQL tables/fields with/without #
    SQL"select i.id, r.#$termFieldName, r.status, r.last_update, i.term, i.status, i.last_update from #$tblInputName as i right join #$tblRuleName as r on i.id = r.#$refKeyFieldName where i.solr_index_id = $solrIndexId".as(sqlParser.*)
  }

  private def sortAllRules(unsortedRules: Seq[RulesReportItem]): Seq[RulesReportItem] = {
    // sort/group by: (1) modified (rule), (2) inputModified,
    // TODO maybe sort for (3) inputTerm (case! quotations!)
    def compareRulesReportItem(a: RulesReportItem, b: RulesReportItem): Int = {
      if(a.modified.equals(b.modified)) {
        a.inputModified.compareTo(b.inputModified)
      }
      else {
        a.modified.compareTo(b.modified)
      }
    }
    unsortedRules.sortWith((a,b) => (compareRulesReportItem(a,b) < 0))
  }

  def loadForSolrIndexId(solrIndexId: SolrIndexId)(implicit connection: Connection): RulesReport = {

    val allSynonymRules = loadReportForTable(solrIndexId, "synonym_rule", "SYNONYM")
    val allUpDownRules = loadReportForTable(solrIndexId, "up_down_rule", "UP/DOWN")
    val allFilterRules = loadReportForTable(solrIndexId, "filter_rule", "FILTER")
    val allDeleteRules = loadReportForTable(solrIndexId, "delete_rule", "DELETE")
    val allRedirectRules = loadReportForTable(solrIndexId, "redirect_rule", "REDIRECT", termFieldName = "target")
    val allSpellings = loadReportForTable(solrIndexId, "alternative_spelling", "SPELLING", tblInputName = "canonical_spelling", refKeyFieldName = "canonical_spelling_id")

    val reportItems = sortAllRules(
      allSynonymRules
      ++ allUpDownRules
      ++ allFilterRules
      ++ allDeleteRules
      ++ allRedirectRules
      ++ allSpellings
    )

    // return report

    RulesReport(
      items = reportItems
    )
  }

}
