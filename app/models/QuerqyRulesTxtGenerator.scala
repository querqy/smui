package models

import javax.inject.Inject

import org.joda.time.DateTime

import models.FeatureToggleModel.FEATURE_TOGGLE_RULE_DEPLOYMENT_AUTO_DECORATE_EXPORT_HASH
import models.SearchManagementModel._
import models.FeatureToggleModel._

@javax.inject.Singleton
class QuerqyRulesTxtGenerator @Inject()(searchManagementRepository: SearchManagementRepository,
                                        featureToggleList: FeatureToggleList) {

  var DO_AUTO_DECORATE_EXPORT_HASH = false; // TODO shouldnt be necessary to define default value 'false' twice or more (see HomeController :: index)

  private def renderSynonymRule(synonymTerm: String): String = {
    return "\tSYNONYM: " + synonymTerm + "\n";
  }

  private def renderUpDownRule(upDownRule: UpDownRule): String = {
    return "\t" +
      (upDownRule.upDownType match {
        case 0 => "UP"
        case 1 => "DOWN"
        // TODO handle case _ which would inferr to an inconsistent state
      }) +
      "(" + upDownRule.boostMalusValue + ")" +
      ": " + upDownRule.term + "\n";
  }

  private def renderFilterRule(filterRule: FilterRule): String = {
    return "\tFILTER: " + filterRule.term + "\n";
  }

  private def renderDeleteRule(deleteRule: DeleteRule): String = {
    return "\tDELETE: " + deleteRule.term + "\n";
  }

  private def renderDecorateExportHash(retSearchInputRulesTxtPartial: String): String = {
    return "\tDECORATE: [ {" +
        "\"intent\":\"smui.auto-decorate.export-hash\", " +
        "\"payload\": { " +
          "\"ruleExportDate\":\"" + DateTime.now.toString() + "\", " +
          "\"ruleExportHash\":\"" + retSearchInputRulesTxtPartial.toString().hashCode() + "\" " +
        "} } ]\n"
  }

  def renderSearchInputRulesForTerm(term: String, searchInput: SearchInput): String = {

    var retSearchInputRulesTxtPartial = new StringBuilder();
    retSearchInputRulesTxtPartial.append(term + " =>\n");

    val allSynonymTerms: List[String] = searchInput.term ::
      searchInput.synonymRules.map(r => r.term).filter(t => t.trim().size > 0);
    for (synonymTerm <- allSynonymTerms) {
      // TODO equals on term-level, evaluate if synonym-term identity should be transferred on id-level
      if (!synonymTerm.equals(term)) {
        retSearchInputRulesTxtPartial.append(renderSynonymRule(synonymTerm));
      }
    }
    for (upDownRule <- searchInput.upDownRules.filter(r => r.term.trim().size > 0)) {
      retSearchInputRulesTxtPartial.append(renderUpDownRule(upDownRule));
    }
    for (filterRule <- searchInput.filterRules.filter(r => r.term.trim().size > 0)) {
      retSearchInputRulesTxtPartial.append(renderFilterRule(filterRule));
    }
    for (deleteRule <- searchInput.deleteRules.filter(r => r.term.trim().size > 0)) {
      retSearchInputRulesTxtPartial.append(renderDeleteRule(deleteRule));
    }
    if( DO_AUTO_DECORATE_EXPORT_HASH ) {
      retSearchInputRulesTxtPartial.append(renderDecorateExportHash(retSearchInputRulesTxtPartial.toString()))
    }

    return retSearchInputRulesTxtPartial.toString();
  }

  private def renderSearchInputRules(searchInput: SearchInput): String = {
    var retQuerqyRulesTxtPartial = new StringBuilder();

    // take SearchInput term and according DIRECTED SynonymRule to render related rules
    val allInputTerms: List[String] = searchInput.term ::
      searchInput.synonymRules
        .filter(r => (r.synonymType == 0) && (r.term.trim().size > 0))
        .map(r => r.term);
    for (inputTerm <- allInputTerms) {
      retQuerqyRulesTxtPartial.append(
        renderSearchInputRulesForTerm(inputTerm, searchInput) +
          "\n")
    };

    return retQuerqyRulesTxtPartial.toString();
  }

  /**
    *
    *
    *
    * @param solrIndexId TODO
    * @param separateRulesTxts Whether to split rules.txt from decompound-rules.txt (true) or not (false).
    * @param renderCompoundsRulesTxt Defining, if decompound-rules.txt (true) or rules.txt (false) should be rendered. Only important, if `separateRulesTxts` is `true`.
    * @return
    */
  private def render(solrIndexId: Long, separateRulesTxts: Boolean, renderCompoundsRulesTxt: Boolean): String = {

    // TODO shouldnt be necessary to init DO_AUTO_DECORATE_EXPORT_HASH with every render()
    DO_AUTO_DECORATE_EXPORT_HASH = featureToggleList
      .getToggle(FEATURE_TOGGLE_RULE_DEPLOYMENT_AUTO_DECORATE_EXPORT_HASH) match {
        case None => false // TODO shouldnt be necessary to define default value 'false' twice or more (see HomeController :: index)
        case Some(toggleValue: FeatureToggleValue) => toggleValue.getValue().asInstanceOf[Boolean].booleanValue()
      };

    var retQuerqyRulesTxt = new StringBuilder();

    // retrieve all detail search input data, that have a (trimmed) input term and minimum one rule
    var listSearchInput: List[SearchInput] = searchManagementRepository
      .listAllSearchInputsInclDirectedSynonyms(solrIndexId)
      .filter(i => i.term.trim().size > 0)
      .map(i => {
        searchManagementRepository
          .getDetailedSearchInput(i.id.get)
      })
      // TODO it needs to be ensured, that a rule not only exists in the list, but also has a filled term (after trim)
      .filter(i =>
        (i.synonymRules.size > 0) ||
        (i.upDownRules.size > 0) ||
        (i.filterRules.size > 0) ||
        (i.deleteRules.size > 0)
      );

    // separate decompound-rules.txt from rules.txt
    // TODO could be done in the above filter statements as well, making listSearchInput a val
    // TODO merge decompound identification login with ApiController :: validateSearchInputToErrMsg
    if( separateRulesTxts ) {
      if( renderCompoundsRulesTxt ) {
        listSearchInput = listSearchInput.filter(i => i.term.trim().endsWith("*"));
      } else {
        listSearchInput = listSearchInput.filter(i => !i.term.trim().endsWith("*"));
      }
    }

    // iterate all SearchInput terms and render related rules
    for (searchInput <- listSearchInput) {
      retQuerqyRulesTxt.append( renderSearchInputRules(searchInput) );
    }

    return retQuerqyRulesTxt.toString()
  }

  def renderSingleRulesTxt(solrIndexId: Long): String = {
    return render(solrIndexId: Long, false, false);
  }

  def renderSeparatedRulesTxts(solrIndexId: Long, renderCompoundsRulesTxt: Boolean): String = {
    return render(solrIndexId: Long, true, renderCompoundsRulesTxt);
  }

}
