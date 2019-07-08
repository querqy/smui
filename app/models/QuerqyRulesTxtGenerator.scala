package models

import java.io.StringReader
import java.net.{URI, URISyntaxException}

import javax.inject.Inject
import org.joda.time.DateTime
import models.SearchManagementModel._
import models.FeatureToggleModel._
import querqy.rewrite.commonrules.WhiteSpaceQuerqyParserFactory
import querqy.rewrite.commonrules.SimpleCommonRulesParser

import scala.util.{Failure, Success, Try}

@javax.inject.Singleton
class QuerqyRulesTxtGenerator @Inject()(searchManagementRepository: SearchManagementRepository,
                                        featureToggleService: FeatureToggleService) {

  // TODO make QuerqyRulesTxtGenerator independent from featureToggleService

  private def renderSynonymRule(synonymTerm: String): String = {
    s"\tSYNONYM: $synonymTerm\n"
  }

  private def renderUpDownRule(upDownRule: UpDownRule): String = {
    "\t" +
      (upDownRule.upDownType match {
        case 0 => "UP"
        case 1 => "DOWN"
        // TODO handle case _ which would inferr to an inconsistent state
      }) +
      "(" + upDownRule.boostMalusValue + ")" +
      ": " + upDownRule.term + "\n"
  }

  private def renderFilterRule(filterRule: FilterRule): String = {
    s"\tFILTER: ${filterRule.term}\n"
  }

  private def renderDeleteRule(deleteRule: DeleteRule): String = {
    s"\tDELETE: ${deleteRule.term}\n"
  }

  private def renderRedirectRule(redirectRule: RedirectRule): String = {
    s"\tDECORATE: REDIRECT ${redirectRule.target}\n"
  }

  private def renderDecorateExportHash(retSearchInputRulesTxtPartial: String): String = {
    // returning resulting auto-decorate for export hash - DECORATE instruction MUST BE IN ONE LINE!
    "\tDECORATE: [ {" +
      "\"intent\":\"smui.auto-decorate.export-hash\", " +
      "\"payload\": { " +
      "\"ruleExportDate\":\"" + DateTime.now.toString() + "\", " +
      "\"ruleExportHash\":\"" + retSearchInputRulesTxtPartial.toString().hashCode() + "\" " +
      "} } ]\n"
  }

  def renderSearchInputRulesForTerm(term: String, searchInput: SearchInput): String = {

    val retSearchInputRulesTxtPartial = new StringBuilder()
    retSearchInputRulesTxtPartial.append(term + " =>\n")

    val allSynonymTerms: List[String] = searchInput.term ::
      searchInput.synonymRules
        .filter(r => r.isActive)
        .map(r => r.term)
        .filter(t => t.trim().nonEmpty)
    for (synonymTerm <- allSynonymTerms) {
      // TODO equals on term-level, evaluate if synonym-term identity should be transferred on id-level
      if (!synonymTerm.equals(term)) {
        retSearchInputRulesTxtPartial.append(renderSynonymRule(synonymTerm))
      }
    }
    for (upDownRule <- searchInput.upDownRules
      .filter(r => r.isActive && r.term.trim().nonEmpty)) {
      retSearchInputRulesTxtPartial.append(renderUpDownRule(upDownRule))
    }
    for (filterRule <- searchInput.filterRules
      .filter(r => r.isActive && r.term.trim().nonEmpty)) {
      retSearchInputRulesTxtPartial.append(renderFilterRule(filterRule))
    }
    for (deleteRule <- searchInput.deleteRules
      .filter(r => r.isActive && r.term.trim().nonEmpty)) {
      retSearchInputRulesTxtPartial.append(renderDeleteRule(deleteRule))
    }
    for (redirectRule <- searchInput.redirectRules
      .filter(r => r.isActive && r.target.trim().nonEmpty)) {
      retSearchInputRulesTxtPartial.append(renderRedirectRule(redirectRule))
    }

    if (featureToggleService.getToggleRuleDeploymentAutoDecorateExportHash) {
      retSearchInputRulesTxtPartial.append(renderDecorateExportHash(retSearchInputRulesTxtPartial.toString()))
    }

    retSearchInputRulesTxtPartial.toString()
  }

  private def renderSearchInputRules(searchInput: SearchInput): String = {
    val retQuerqyRulesTxtPartial = new StringBuilder()

    // take SearchInput term and according DIRECTED SynonymRule to render related rules
    val allInputTerms: List[String] = searchInput.term ::
      searchInput.synonymRules
        .filter(r => r.isActive && (r.synonymType == 0) && r.term.trim().nonEmpty)
        .map(r => r.term)
    for (inputTerm <- allInputTerms) {
      retQuerqyRulesTxtPartial.append(
        renderSearchInputRulesForTerm(inputTerm, searchInput) +
          "\n")
    }

    retQuerqyRulesTxtPartial.toString()
  }

  /**
    * TODO
    *
    * @param solrIndexId             TODO
    * @param separateRulesTxts       Whether to split rules.txt from decompound-rules.txt (true) or not (false).
    * @param renderCompoundsRulesTxt Defining, if decompound-rules.txt (true) or rules.txt (false) should be rendered. Only important, if `separateRulesTxts` is `true`.
    * @return
    */
  private def render(solrIndexId: String, separateRulesTxts: Boolean, renderCompoundsRulesTxt: Boolean): String = {

    val retQuerqyRulesTxt = new StringBuilder()

    // retrieve all detail search input data, that have a (trimmed) input term and minimum one rule
    val listSearchInput: List[SearchInput] = searchManagementRepository
      .listAllSearchInputsInclDirectedSynonyms(solrIndexId)
      .filter(i => i.term.trim().nonEmpty)
      .map(i => {
        searchManagementRepository
          .getDetailedSearchInput(i.id.get)
      })
      // filter all inputs, that do not have any active rule
      // TODO it needs to be ensured, that a rule not only exists in the list, are active, BUT also has a filled term (after trim)
      .filter(i =>
      i.synonymRules.exists(r => r.isActive) ||
        i.upDownRules.exists(r => r.isActive) ||
        i.filterRules.exists(r => r.isActive) ||
        i.deleteRules.exists(r => r.isActive) ||
        i.redirectRules.exists(r => r.isActive)
    )

    // TODO merge decompound identification login with ApiController :: validateSearchInputToErrMsg


    // separate decompound-rules.txt from rules.txt
    def separateRules(listSearchInput: List[SearchInput]) = {
      if (separateRulesTxts) {
        if (renderCompoundsRulesTxt) {
          listSearchInput.filter(i => i.term.trim().endsWith("*"))
        } else {
          listSearchInput.filter(i => !i.term.trim().endsWith("*"))
        }
      } else {
        listSearchInput
      }
    }

    // iterate all SearchInput terms and render related rules
    for (searchInput <- separateRules(listSearchInput)) {
      retQuerqyRulesTxt.append(renderSearchInputRules(searchInput))
    }

    retQuerqyRulesTxt.toString()
  }

  def renderSingleRulesTxt(solrIndexId: String): String = {
    render(solrIndexId, false, false)
  }

  def renderSeparatedRulesTxts(solrIndexId: String, renderCompoundsRulesTxt: Boolean): String = {
    render(solrIndexId, true, renderCompoundsRulesTxt)
  }

  /**
    * Validate a fragment or a complete rules.txt against a Querqy instance.
    *
    * @param strRulesTxt string containing a fragment or a complete rules.txt
    * @return None, if no validation error, otherwise Some(String) containing the error.
    */
  def validateQuerqyRulesTxtToErrMsg(strRulesTxt: String): Option[String] = {

    try {
      val simpleCommonRulesParser: SimpleCommonRulesParser = new SimpleCommonRulesParser(
        new StringReader(strRulesTxt),
        new WhiteSpaceQuerqyParserFactory(),
        true
      )
      simpleCommonRulesParser.parse()
      None
    } catch {
      case e: Exception => {
        // TODO better parse the returned Exception and return a line-wise error object making validation errors assign-able to specific rules
        Some(e.getMessage())
      }
    }
  }

  /**
    * Validate a {{searchInput}} instance for (1) SMUI plausibility as well as (2) the resulting rules.txt fragment
    * against Querqy.
    *
    * @param searchInput Input instance to be validated.
    * @return None, if no validation error, otherwise a String containing the error.
    */
  def validateSearchInputToErrMsg(searchInput: SearchInput): Option[String] = {

    // TODO validation ends with first broken rule, it should collect all errors to a line.
    // TODO decide, if input having no rule at all is legit ... (e.g. newly created). Will currently being filtered.

    // validate against SMUI plausibility rules
    // TODO evaluate to refactor the validation implementation into models/QuerqyRulesTxtGenerator

    // if input contains *-Wildcard, all synonyms must be directed
    // TODO discuss if (1) contains or (2) endsWith is the right interpretation
    val synonymsDirectedCheck: Option[String] = if (searchInput.term.trim().contains("*")) {
      if (searchInput.synonymRules.exists(r => r.synonymType == 0)) {
        Some("Wildcard *-using input ('\" + searchInput.term + \"') has undirected synonym rule")
      } else None
    } else None

    // undirected synonyms must not contain *-Wildcard
    val undirectedSynonymsCheck: Option[String] = if (searchInput.synonymRules.exists(r => r.synonymType == 0 && r.term.trim().contains("*"))) {
      Some("Parsing Search Input: Wildcard *-using undirected synonym for Input ('" + searchInput.term + "')")
    } else None

    val redirectRuleErrors = searchInput.redirectRules.map(r => validateRedirectTarget(r.target))

    val querqyRuleValidationError = validateQuerqyRulesTxtToErrMsg(renderSearchInputRulesForTerm(searchInput.term, searchInput))

    val errors = List(querqyRuleValidationError, undirectedSynonymsCheck, synonymsDirectedCheck) ++ redirectRuleErrors
    //

    // finally validate as well against querqy parser

    // TODO validate both inputs and rules, for all undirected synonym terms in this input
    errors.foldLeft[Option[String]](None) {
      case (Some(res), Some(error)) => Some(res + ", " + error)
      case (Some(res), None) => Some(res)
      case (None, errOpt) => errOpt
    }
  }

  private def validateRedirectTarget(target: String): Option[String] = {
    if (target.isEmpty) {
      Some("No target URL for redirect")
    } else if (target.startsWith("http://") || target.startsWith("https://")) {
      validateHttpUri(target)
    } else if (!target.startsWith("/")) {
      Some("Target is neither absolute HTTP(S) URI nor starts with /")
    } else {
      None
    }
  }

  private def validateHttpUri(target: String): Option[String] = {
    Try(new URI(target)) match {
      case Success(_) => None
      case Failure(e: URISyntaxException) => Some(s"Not a valid URL: $target (${e.getMessage})")
      case Failure(t: Throwable) => Some(s"Error validating $target: ${t.getMessage}")
    }
  }


}
