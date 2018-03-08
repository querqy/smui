package models

import javax.inject.Inject

import models.SearchManagementModel._

@javax.inject.Singleton
class QuerqyRulesTxtGenerator @Inject()(searchManagementRepository: SearchManagementRepository) {

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

    return retSearchInputRulesTxtPartial.toString();
  }

  def renderSearchInputRules(searchInput: SearchInput): String = {
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

  def render(solrIndexId: Long): String = {

    var retQuerqyRulesTxt = new StringBuilder();

    // retrieve all detail search input data, that have a (trimmed) input term and minimum one rule
    val listSearchInput: List[SearchInput] = searchManagementRepository
      .listAllSearchInputsInclDirectedSynonyms(solrIndexId)
      .filter(i => i.term.trim().size > 0)
      .map(i => {
        searchManagementRepository
          .getDetailedSearchInput(i.id.get)
      })
      // TODO it needs to be ensured, that a rule not only exists in the List, but also has a filled term (after trim)
      .filter(i =>
        (i.synonymRules.size > 0) ||
        (i.upDownRules.size > 0) ||
        (i.filterRules.size > 0) ||
        (i.deleteRules.size > 0)
      );

    // iterate all SearchInput terms and render related rules
    for (searchInput <- listSearchInput) {
      retQuerqyRulesTxt.append(
        renderSearchInputRules(searchInput)
      );
    }

    return retQuerqyRulesTxt.toString()
  }

}
