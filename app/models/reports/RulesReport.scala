package models.reports

import java.sql.Connection
import java.time.LocalDateTime

import anorm._
import anorm.SqlParser.get
import models.input.{SearchInput, SearchInputId, SearchInputWithRules}
import models.spellings.{CanonicalSpelling, CanonicalSpellingId, CanonicalSpellingWithAlternatives}
import play.api.libs.json.{Json, OFormat}
import play.api.Logging
import models.{SolrIndexId, Status}

case class RulesReportItem(
  inputId: String, // TODO inputType needed?
  term: String,
  details: String,
  isActive: Boolean,
  modified: LocalDateTime,
  inputTerm: String,
  inputModified: LocalDateTime,
  inputTags: Seq[String]
)

case class RulesReport(items: Seq[RulesReportItem])

object RulesReport extends Logging {

  implicit val jsonFormatRulesReportItem: OFormat[RulesReportItem] = Json.format[RulesReportItem]
  implicit val jsonFormatRulesReport: OFormat[RulesReport] = Json.format[RulesReport]

  private def loadReportForTable(solrIndexId: SolrIndexId, tblRuleName: String, detailsDescr: String, termFieldName: String = "term", tblInputName: String = SearchInput.TABLE_NAME, refKeyFieldName: String = "search_input_id")(implicit connection: Connection): Seq[RulesReportItem] = {

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
          isActive = Status.isActiveFromStatus(ruleStatus) && Status.isActiveFromStatus(inputStatus),
          modified = ruleLastUpdate,
          inputTerm = inputTerm,
          inputModified = inputLastUpdate,
          // TODO consider writing one join-SQL to retrieve tags as well (or at least just one further SQL per input; not rule) ==> performance
          inputTags = tblInputName match {
            case SearchInput.TABLE_NAME => SearchInputWithRules.loadById(SearchInputId(inputId)).get.tags.map(t => t.displayValue)
            case CanonicalSpelling.TABLE_NAME => Nil
          }
        )
      }
    }

    // TODO consider removing i and r for the real table names
    // TODO consider making SQL business logic part of rules/spelling entities (e.g. models/rules/SynonymRule.scala)
    // TODO referencing SQL tables/fields with/without #
    SQL"select i.id, r.#$termFieldName, r.status, r.last_update, i.term, i.status, i.last_update from #$tblInputName as i join #$tblRuleName as r on i.id = r.#$refKeyFieldName where i.solr_index_id = $solrIndexId".as(sqlParser.*)
  }

  private def sortAllRules(unsortedRules: Seq[RulesReportItem]): Seq[RulesReportItem] = {
    // sort/group by
    def compareRulesReportItem(a: RulesReportItem, b: RulesReportItem): Int = {
      if (a.modified.equals(b.modified)) {
        if (a.inputModified.equals(b.inputModified)) {
          // sorting prio 3) inputTerm (case insensitive! quotations!)
          // TODO maybe make this part of Input (as it is needed by sorting the result list as well)
          def normalisedTerm(term: String): String = {
            // kill first character, if there is a quotation
            (if (term.trim.charAt(0).equals('"'))
              term.substring(1).trim
            else
              term.trim)
              // lowercase everything
              .toLowerCase
          }
          // normalise & compare
          val normA = normalisedTerm(a.inputTerm)
          val normB = normalisedTerm(b.inputTerm)
          normA.compareTo(normB)
        } else {
          // sorting prio 2) inputModified (of rule)
          a.inputModified.compareTo(b.inputModified)
        }
      } else {
        // sorting prio 1) modified date (of rule)
        a.modified.compareTo(b.modified)
      }
    }
    unsortedRules.sortWith((a,b) => (compareRulesReportItem(a,b) < 0))
  }

  //CJM 7
  // TODO write test
  def loadForSolrIndexId(solrIndexId: SolrIndexId)(implicit connection: Connection): RulesReport = {

    val allSynonymRules = loadReportForTable(solrIndexId, "synonym_rule", "SYNONYM")
    val allUpDownRules = loadReportForTable(solrIndexId, "up_down_rule", "UP/DOWN")
    val allFilterRules = loadReportForTable(solrIndexId, "filter_rule", "FILTER")
    val allDeleteRules = loadReportForTable(solrIndexId, "delete_rule", "DELETE")
    val allRedirectRules = loadReportForTable(solrIndexId, "redirect_rule", "REDIRECT", termFieldName = "target")
    val allSpellings = loadReportForTable(solrIndexId, "alternative_spelling", "SPELLING", tblInputName = CanonicalSpelling.TABLE_NAME, refKeyFieldName = "canonical_spelling_id")

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
