package models.querqy

import java.io.StringReader
import java.net.{URI, URISyntaxException}
import javax.inject.Inject
import models.FeatureToggleModel._
import models.rules._
import models.{SearchManagementRepository, SolrIndexId}
import models.input.{InputTag, SearchInputWithRules}
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json.{JsString, Json}
import querqy.rewrite.commonrules.{SimpleCommonRulesParser, WhiteSpaceQuerqyParserFactory}

import scala.collection.mutable.ListBuffer
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

  def renderSearchInputRulesForTerm(term: String, searchInput: SearchInputWithRules): String = {

    val retSearchInputRulesTxtPartial = new StringBuilder()
    retSearchInputRulesTxtPartial.append(term + " =>\n")

    val allSynonymTerms: List[String] = searchInput.term ::
      searchInput.synonymRules
        .filter(r => r.isActive)
        .map(r => r.term)
        .filter(t => t.trim().nonEmpty)
    for (synonymTerm <- allSynonymTerms) {
      // TODO equals on term-level, evaluate if synonym-term identity should be transferred on id-level
      if (synonymTerm != term) {
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

    val jsonProperties = ListBuffer[(String, JsValueWrapper)]()
    if (featureToggleService.getToggleRuleDeploymentLogRuleId) {
      jsonProperties += (("_log", JsString(searchInput.id.id)))
    }
    if (featureToggleService.isRuleTaggingActive) {
      val tagsByProperty = searchInput.tags.filter(i => i.exported && i.property.nonEmpty).groupBy(_.property.get)
      jsonProperties ++= tagsByProperty.mapValues(tags => Json.toJsFieldJsValueWrapper(tags.map(_.value))).toSeq
    }

    if (jsonProperties.nonEmpty) {
      retSearchInputRulesTxtPartial.append(renderJsonProperties(jsonProperties))
    }

    retSearchInputRulesTxtPartial.toString()
  }

  private def renderJsonProperties(properties: Seq[(String, JsValueWrapper)]): String = {
    val jsonString = Json.prettyPrint(Json.obj(properties: _*)).split('\n').map(s => s"\t$s").mkString("\n").trim()

    s"\t@$jsonString@\n"
  }

  private def renderSearchInputRules(searchInput: SearchInputWithRules): String = {
    val retQuerqyRulesTxtPartial = new StringBuilder()

    // take SearchInput term and all undirected SynonymRules to render related rules
    val allInputTerms: List[String] = searchInput.term ::
      searchInput.synonymRules
        .filter(r => r.isActive && (r.synonymType == SynonymRule.TYPE_UNDIRECTED) && r.term.trim().nonEmpty)
        .map(r => r.term)
    for (inputTerm <- allInputTerms) {
      retQuerqyRulesTxtPartial.append(
        renderSearchInputRulesForTerm(inputTerm, searchInput) +
          "\n")
    }

    retQuerqyRulesTxtPartial.toString()
  }

  def renderListSearchInputRules(listSearchInput: Seq[SearchInputWithRules]): String = {
    val retQuerqyRulesTxt = new StringBuilder()

    // iterate all SearchInput terms and render related (active) rules
    for (searchInput <- listSearchInput.filter(_.isActive)) {
      retQuerqyRulesTxt.append(renderSearchInputRules(searchInput))
    }

    retQuerqyRulesTxt.toString()
  }

  def hasValidInputTagValue(i: SearchInputWithRules, filterTagName: String, inputTag: InputTag): Boolean = {
    // no filter on tags defined
    if (filterTagName == null || filterTagName.isEmpty) {
      return true
    }
    val ruleTags = i.tags.filter(_.property.get.equals(filterTagName))
    // common rules (without the input tag
    if (inputTag == null) {
      ruleTags.isEmpty
    } else {
      ruleTags.map(t => t.id).contains(inputTag.id)
    }
  }

  /**
    * TODO
    *
    * @param solrIndexId             TODO
    * @param separateRulesTxts       Whether to split rules.txt from decompound-rules.txt (true) or not (false).
    * @param renderCompoundsRulesTxt Defining, if decompound-rules.txt (true) or rules.txt (false) should be rendered. Only important, if `separateRulesTxts` is `true`.
    * @return
    */
  // TODO resolve & test logic of render method (change interface to separate decompound from normal rules)
  private def render(solrIndexId: SolrIndexId, separateRulesTxts: Boolean, renderCompoundsRulesTxt: Boolean, filterTagName: String, inputTag: InputTag): String = {

    val retQuerqyRulesTxt = new StringBuilder()

    // retrieve all detail search input data, that have a (trimmed) input term and minimum one rule
    val listSearchInput: Seq[SearchInputWithRules] = searchManagementRepository
      .loadAllInputIdsForSolrIndex(solrIndexId)
      .flatMap(id => searchManagementRepository.getDetailedSearchInput(id))
      .filter(i => i.trimmedTerm.nonEmpty)
      // TODO it needs to be ensured, that a rule not only exists in the list, are active, BUT also has a filled term (after trim)
      .filter(_.hasAnyActiveRules)
      .filter(i => hasValidInputTagValue(i, filterTagName, inputTag))

    // TODO merge decompound identification login with ApiController :: validateSearchInputToErrMsg

    // separate decompound-rules.txt from rules.txt
    def separateRules(listSearchInput: Seq[SearchInputWithRules]) = {
      if (separateRulesTxts) {
        if (renderCompoundsRulesTxt) {
          listSearchInput.filter(_.trimmedTerm.endsWith("*"))
        } else {
          listSearchInput.filter(i => !i.trimmedTerm.endsWith("*"))
        }
      } else {
        listSearchInput
      }
    }

    renderListSearchInputRules(separateRules(listSearchInput))
  }

  def renderSingleRulesTxt(solrIndexId: SolrIndexId, filterTagName: String, inputTag: InputTag): String = {
    render(solrIndexId, false, false, filterTagName, inputTag)
  }

  def renderSeparatedRulesTxts(solrIndexId: SolrIndexId, renderCompoundsRulesTxt: Boolean, filterTagName: String, inputTag: InputTag): String = {
    render(solrIndexId, true, renderCompoundsRulesTxt, filterTagName, inputTag)
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

  def validateNativeQuery(ruleTerm: String): Option[String] = {
    // if there is a native query configured (indicated by *)
    val posNativeQueryIndicator = ruleTerm.indexOf('*')
    if (posNativeQueryIndicator != -1) {
      // ... then is this to be non empty
      val nativeQuery = ruleTerm.substring(posNativeQueryIndicator+1).trim
      if (nativeQuery.isEmpty) {
        // Intuitively this should be a validation error as well, but this constellation is covered by querqy itself.
        // So, this validation will accept empty native queries as querqy does the validation (see validateSearchInputToErrMsg).
        None
      } else {
        // TODO this is Solr only (and might need to be adjusted, in case of native Elasticsearch queries)
        // if there is a field/value indicator present
        val posFieldValueIndicator = nativeQuery.indexOf(':')
        if (posFieldValueIndicator != -1) {
          // ... then this needs to be followed by a (non-empty) FIELD_NAME:FIELD_VALUE pattern
          val fieldName = nativeQuery.substring(0, posFieldValueIndicator).trim
          val fieldValue = nativeQuery.substring(posFieldValueIndicator+1).trim
          if(fieldName.isEmpty || fieldValue.isEmpty) {
            Some(s"No FIELD_NAME:FIELD_VALUE pattern given for native query rule = $ruleTerm")
          } else {
            None
          }
        } else {
          None
        }
      }
    } else {
      None
    }
  }

  /**
    * Validate a {{searchInput}} instance for (1) SMUI plausibility as well as (2) the resulting rules.txt fragment
    * against Querqy.
    *
    * @param searchInput Input instance to be validated.
    * @return None, if no validation error, otherwise a String containing the error.
    */
  // TODO maybe the validation concern should be moved into the input model?
  def validateSearchInputToErrMsg(searchInput: SearchInputWithRules): Option[String] = {

    // validate against SMUI plausibility rules

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

    val nativeQueryValidationError = (
      // native search-engine queries can occur with UP/DOWN and FILTER rules
      searchInput.upDownRules
      ++ searchInput.filterRules
    ).map(r => validateNativeQuery(r.term))

    val emptyRuleValidationError = (searchInput.synonymRules ++ searchInput.upDownRules ++ searchInput.filterRules ++ searchInput.deleteRules)
      .map {
        case r: RuleWithTerm => r
      }
      .map(r => {
        if(r.term.trim.isEmpty) {
          Some(s"Rule '${r.term}' is empty")
        } else {
          None
        }
      })

    // validate as well against querqy parser

    val querqyRuleValidationError = validateQuerqyRulesTxtToErrMsg(renderSearchInputRulesForTerm(searchInput.term, searchInput))

    // aggregate validation feedback

    val errors = List(
      querqyRuleValidationError,
      undirectedSynonymsCheck,
      synonymsDirectedCheck
    ) ++ redirectRuleErrors ++ nativeQueryValidationError ++ emptyRuleValidationError

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
