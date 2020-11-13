package models

import models.FeatureToggleModel.FeatureToggleService
import models.input.{SearchInputId, SearchInputWithRules, InputTag}
import models.querqy.QuerqyRulesTxtGenerator
import models.rules._
import models.spellings.{AlternativeSpelling, AlternativeSpellingId, CanonicalSpellingId, CanonicalSpellingWithAlternatives}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}

class QuerqyRulesTxtGeneratorSpec extends FlatSpec with Matchers with MockitoSugar {

  val searchManagementRepository = mock[SearchManagementRepository]
  val featureToggleService = mock[FeatureToggleService]

  val generator = new QuerqyRulesTxtGenerator(searchManagementRepository, featureToggleService)

  "Rules Text Generation" should "consider up/down rules correctly" in {
    val upDownRules = List(
      UpDownRule(UpDownRuleId(), 0, 10, "notebook", true),
      UpDownRule(UpDownRuleId(), 0, 10, "lenovo", false),
      UpDownRule(UpDownRuleId(), 1, 10, "battery", true)
    )
    val rulesTxt = generator.renderSearchInputRulesForTerm("laptop",
      SearchInputWithRules(SearchInputId(), term = "laptop", upDownRules = upDownRules, isActive = true, comment = ""))

    rulesTxt should be(
      s"""|laptop =>
          |\tUP(10): notebook
          |\tDOWN(10): battery
          |""".stripMargin)
  }


  "Rules Text Generation" should "correctly write a DELETE rules" in {
    val deleteRules = List(DeleteRule(DeleteRuleId(), "freddy", true))

    val rulesTxt = generator.renderSearchInputRulesForTerm("queen",
      SearchInputWithRules(SearchInputId(), term = "queen", deleteRules = deleteRules, isActive = true, comment = ""))
    rulesTxt should be(
      s"""|queen =>
          |\tDELETE: freddy
          |""".stripMargin)
  }

  "Rules Text Generation" should "correctly write a undirected SYNONYM rules" in {
    val synonymRules = List(SynonymRule(SynonymRuleId(), 0, "mercury", true))

    val rulesTxt = generator.renderSearchInputRulesForTerm("queen",
      SearchInputWithRules(SearchInputId(), term = "queen", synonymRules = synonymRules, isActive = true, comment = ""))
    rulesTxt should be(
      s"""|queen =>
          |\tSYNONYM: mercury
          |""".stripMargin)
  }


  "Rules Text Generation" should "correctly add FILTER rules" in {
    val filterRules = List(FilterRule(FilterRuleId(), "zz top", true))

    val rulesTxt = generator.renderSearchInputRulesForTerm("abba",
      SearchInputWithRules(SearchInputId(), term = "abba", filterRules = filterRules, isActive = true, comment = ""))
    rulesTxt should be(
      s"""|abba =>
          |\tFILTER: zz top
          |""".stripMargin)
  }

  "Rules Text Generation" should "correctly combine SYNONYM, FILTER, DELETE and UPDOWN Rules" in {
    val synonymRules = List(SynonymRule(SynonymRuleId(), 0, "mercury", true))
    val upDownRules = List(
      UpDownRule(UpDownRuleId(), 0, 10, "notebook", true),
      UpDownRule(UpDownRuleId(), 0, 10, "lenovo", false),
      UpDownRule(UpDownRuleId(), 1, 10, "battery", true)
    )
    val deleteRules = List(DeleteRule(DeleteRuleId(), "freddy", true))
    val filterRules = List(FilterRule(FilterRuleId(), "zz top", true))
    val rulesTxt = generator.renderSearchInputRulesForTerm("aerosmith",
      SearchInputWithRules(SearchInputId(), term = "aerosmith", filterRules = filterRules,
        synonymRules = synonymRules, deleteRules = deleteRules, upDownRules = upDownRules, isActive = true, comment = ""))

    rulesTxt should be(
      s"""|aerosmith =>
          |\tSYNONYM: mercury
          |\tUP(10): notebook
          |\tDOWN(10): battery
          |\tFILTER: zz top
          |\tDELETE: freddy
          |""".stripMargin
    )

  }

  "Rules Text Generation" should "add an @_log decorator with the id of the rule" in {
    val featureToggleMock = mock[FeatureToggleService]
    when(featureToggleMock.getToggleRuleDeploymentLogRuleId).thenReturn(true)

    val synonymRules = List(SynonymRule(SynonymRuleId(), 0, "mercury", true))

    val classUnderTest = new QuerqyRulesTxtGenerator(searchManagementRepository, featureToggleMock)
    val rulesTxt = classUnderTest.renderSearchInputRulesForTerm("queen",
      SearchInputWithRules(SearchInputId("rule-id"), "queen", synonymRules = synonymRules, isActive = true, comment = ""))
    rulesTxt should be(
      s"""|queen =>
          |\tSYNONYM: mercury
          |\t@{
          |\t  "_log" : "rule-id"
          |\t}@
          |""".stripMargin)
  }

  it should "add tags to the rules as json properties if rule tagging is active" in {
    val featureToggleMock = mock[FeatureToggleService]
    when(featureToggleMock.getToggleRuleDeploymentLogRuleId).thenReturn(true)
    when(featureToggleMock.isRuleTaggingActive).thenReturn(true)

    val synonymRules = List(SynonymRule(SynonymRuleId(), 0, "mercury", isActive = true))
    val input = SearchInputWithRules(SearchInputId("rule-id"), "queen", synonymRules = synonymRules,
      tags = Seq(
        InputTag.create(None, Some("tenant"), "MO", exported = true),
        InputTag.create(None, Some("tenant"), "MO_AT", exported = true),
        InputTag.create(None, Some("color"), "red", exported = true),
        InputTag.create(None, None, "dummy", exported = true), // should not be exported, since no property is set
        InputTag.create(None, Some("notExported"), "value", exported = false)
      ),
      isActive = true,
      comment = "")

    val classUnderTest = new QuerqyRulesTxtGenerator(searchManagementRepository, featureToggleMock)
    val rulesTxt = classUnderTest.renderSearchInputRulesForTerm("queen", input)
    rulesTxt should be(
      s"""|queen =>
          |\tSYNONYM: mercury
          |\t@{
          |\t  "_log" : "rule-id",
          |\t  "tenant" : [ "MO", "MO_AT" ],
          |\t  "color" : [ "red" ]
          |\t}@
          |""".stripMargin)

  }

  "Rules Text Generation" should "ignore inactive inputs completely" in {

    val listSearchInput = List(
      SearchInputWithRules(id = SearchInputId("1"), term = "notebook",
        synonymRules = List(SynonymRule(SynonymRuleId(), 1, "laptop", true)),
        isActive = true, comment = ""),
      SearchInputWithRules(id = SearchInputId("2"), term = "battery",
        synonymRules = List(SynonymRule(SynonymRuleId(), 1, "power supply", true)),
        isActive = true, comment = ""),
      SearchInputWithRules(id = SearchInputId("3"), term = "wolf",
        synonymRules = List(SynonymRule(SynonymRuleId(), 0, "dog", true)),
        isActive = false, comment = ""),
      SearchInputWithRules(id = SearchInputId("4"), term = "cable",
        synonymRules = List(SynonymRule(SynonymRuleId(), 0, "wire", true)),
        isActive = true, comment = "")
    )

    val rulesTxt = generator.renderListSearchInputRules(listSearchInput)

    rulesTxt should be(
      s"""|notebook =>
          |\tSYNONYM: laptop
          |
          |battery =>
          |\tSYNONYM: power supply
          |
          |cable =>
          |\tSYNONYM: wire
          |
          |wire =>
          |\tSYNONYM: cable
          |
          |""".stripMargin
    )
  }

  // TODO outsource whole rules.txt file to an external test ressource
  val VALID_RULES_TXT =
    s""""handy" =>
       |	SYNONYM: smartphone
       |	UP(100): smartphone
       |	@_log: "5b683c9e-d2df-11e9-bb65-2a2ae2dbcce4"
       |
       |cheap iphone =>
       |	SYNONYM: iphone 3g
       |	UP(100): * price:[* TO 50000]
       |	DELETE: cheap
       |	@_log: "884c067a-48b7-4170-a0d9-a1d5e70bbf80"
       |
       |notebook =>
       |	SYNONYM: laptop
       |	SYNONYM: netbook
       |	UP(10): asus
       |	DOWN(100): Optical
       |	DOWN(5): Power Cord
       |	FILTER: * -title:accessory
       |	FILTER: * -title:notebook
       |	@_log: "ea16b373-6776-469c-9cc7-1449a97f1a79"
       |
       |laptop =>
       |	SYNONYM: notebook
       |	SYNONYM: netbook
       |	UP(10): asus
       |	DOWN(100): Optical
       |	DOWN(5): Power Cord
       |	FILTER: * -title:accessory
       |	FILTER: * -title:notebook
       |	@_log: "88bb6558-e6af-45fc-a862-0dbbe6dec32f"""".stripMargin

  "rules.txt validation" should "positively validate valid rules.txt" in {
    generator.validateQuerqyRulesTxtToErrMsg(VALID_RULES_TXT) should be(None)
  }

  "rules.txt validation" should "return an error when validating an invalid rules.txt" in {
    generator.validateQuerqyRulesTxtToErrMsg(VALID_RULES_TXT + "\nADD AN INVALID INSTRUCTION") should be
    Some("Line 31: Cannot parse line: ADD AN INVALID INSTRUCTION")
  }

  /**
    * Validation extension spec, see https://github.com/querqy/smui/issues/51
    */

  def staticDownRuleWithTerm(term: String) = {
    UpDownRule(
      upDownType = UpDownRule.TYPE_DOWN,
      boostMalusValue = 50,
      term = term,
      isActive = true
    )
  }

  def staticFilterRuleWithTerm(term: String) = {
    FilterRule(
      term = term,
      isActive = true
    )
  }

  def staticSynonymRuleWithTerm(term: String) = {
    SynonymRule(
      synonymType = SynonymRule.TYPE_UNDIRECTED,
      term = term,
      isActive = true
    )
  }

  def staticDeleteRuleWithTerm(term: String) = {
    DeleteRule(
      term = term,
      isActive = true
    )
  }

  "SearchInputWithRules with empty rules" should "return a validation error" in {
    val invalidInput = SearchInputWithRules(
      id = SearchInputId(),
      term = "test query",
      synonymRules = List(
        // add this to make the rules.txt partial valid in general (for querqy validation)
        staticSynonymRuleWithTerm("valid synonym"),
        // start with the invalid rules (SMUI validated)
        staticSynonymRuleWithTerm(""),
        staticSynonymRuleWithTerm("     ")
      ),
      upDownRules = List(
        staticDownRuleWithTerm(""),
        staticDownRuleWithTerm("     ")
      ),
      filterRules = List(
        staticFilterRuleWithTerm(""),
        staticFilterRuleWithTerm("     ")
      ),
      deleteRules = List(
        staticDeleteRuleWithTerm(""),
        staticDeleteRuleWithTerm("     ")
      ),
      redirectRules = Nil,
      tags = Seq.empty,
      isActive = true,
      comment = ""
    )

    val errors = generator.validateSearchInputToErrMsg(invalidInput)

    errors shouldBe Some("Rule '' is empty, Rule '     ' is empty, Rule '' is empty, Rule '     ' is empty, Rule '' is empty, Rule '     ' is empty, Rule '' is empty, Rule '     ' is empty")
  }

  "SearchInputWithRules with invalid native queries" should "return a validation error" in {

    val invalidInput = SearchInputWithRules(
      id = SearchInputId(),
      term = "test query",
      synonymRules = Nil,
      upDownRules = List(
        staticDownRuleWithTerm("*"),
        staticDownRuleWithTerm("   *      "),
        staticDownRuleWithTerm("* manufacturer:"),
        staticDownRuleWithTerm("* :Wiko"),
        staticDownRuleWithTerm("* manufacturer:     "),
        staticDownRuleWithTerm("*      :Wiko"),
      ),
      filterRules = List(
        staticFilterRuleWithTerm("*"),
        staticFilterRuleWithTerm("   *      "),
        staticFilterRuleWithTerm("* -searchText:"),
        staticFilterRuleWithTerm("* :\"Mi Smart Plug\""),
        staticFilterRuleWithTerm("* -searchText:     "),
        staticFilterRuleWithTerm("*      :\"Mi Smart Plug\"")
      ),
      deleteRules = Nil,
      redirectRules = Nil,
      tags = Seq.empty,
      isActive = true,
      comment = ""
    )

    val errors = generator.validateSearchInputToErrMsg(invalidInput)

    // TODO There should also be a "Missing raw query" validation error for "Line 3" and 8 and 9 (first FILTER expressions), where there is also no native query - maybe a querqy issue?
    errors shouldBe Some(
      "Line 2: Missing raw query after * in line: DOWN(50): *, No FIELD_NAME:FIELD_VALUE pattern given for native query rule = * manufacturer:, No FIELD_NAME:FIELD_VALUE pattern given for native query rule = * :Wiko, No FIELD_NAME:FIELD_VALUE pattern given for native query rule = * manufacturer:     , No FIELD_NAME:FIELD_VALUE pattern given for native query rule = *      :Wiko, No FIELD_NAME:FIELD_VALUE pattern given for native query rule = * -searchText:, No FIELD_NAME:FIELD_VALUE pattern given for native query rule = * :\"Mi Smart Plug\", No FIELD_NAME:FIELD_VALUE pattern given for native query rule = * -searchText:     , No FIELD_NAME:FIELD_VALUE pattern given for native query rule = *      :\"Mi Smart Plug\""
    )
  }

  "SearchInputWithRules with valid native queries" should "return no validation error" in {

    val validInput = SearchInputWithRules(
      id = SearchInputId(),
      term = "test query",
      synonymRules = Nil,
      upDownRules = List(
        staticDownRuleWithTerm("* a:a"),
        staticDownRuleWithTerm("*     a:    a    "),
        staticDownRuleWithTerm("* manufacturer:Wiko"),
        staticDownRuleWithTerm("manufacturer:ZTE"),
        staticDownRuleWithTerm("searchText:\"Iphone 11\"")
      ),
      filterRules = List(
        staticFilterRuleWithTerm("* a:a"),
        staticFilterRuleWithTerm("*     a:    a    "),
        staticFilterRuleWithTerm("* -searchText:\"Mi Smart Plug\"\""),
        staticFilterRuleWithTerm("* -searchText:\"Roboter-Staubsauger\"")

      ),
      deleteRules = Nil,
      redirectRules = Nil,
      tags = Seq.empty,
      isActive = true,
      comment = ""
    )

    val errors = generator.validateSearchInputToErrMsg(validInput)

    errors shouldBe None
  }

  // TODO add more tests for validateSearchInputToErrMsg

}
