package models

import models.FeatureToggleModel.FeatureToggleService
import models.rules._
import models.spellings.{AlternateSpelling, AlternateSpellingId, CanonicalSpellingId, CanonicalSpellingWithAlternatives}
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

  it should "correctly create REPLACE rules from spellings" in {
    val canonicalSpellingId = CanonicalSpellingId()
    val canonicalSpelling = CanonicalSpellingWithAlternatives(
      canonicalSpellingId, "freezer", List(
        AlternateSpelling(AlternateSpellingId(), canonicalSpellingId, "frozer"),
        AlternateSpelling(AlternateSpellingId(), canonicalSpellingId, "frazer"),
        AlternateSpelling(AlternateSpellingId(), canonicalSpellingId, "fräzer"),
        AlternateSpelling(AlternateSpellingId(), canonicalSpellingId, "frsadsadsv"),
      )
    )

    val replaceRule = generator.renderReplaceRule(canonicalSpelling)
    replaceRule shouldBe "frozer; frazer; fräzer; frsadsadsv => freezer\n"
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

  val VALID_REPLACE_RULES_TXT =
    s"""frezer; freazer; frazer => freezer
       |machin; mechine => machine
       |pands; pents => pants""".stripMargin

  "replace-rules.txt validation" should "positively validate valid rules.txt" in {
    generator.validateQuerqyReplaceRulesTxtToErrMsg(VALID_REPLACE_RULES_TXT) should be(None)
  }

  "replace-rules.txt validation" should "return an error when validating an invalid replace-rules.txt" in {
    val error = generator.validateQuerqyReplaceRulesTxtToErrMsg(VALID_REPLACE_RULES_TXT + "\nADD AN INVALID INSTRUCTION")
    error.get should include("Each non-empty line must either start with # or contain a rule")
  }

  // TODO add tests for validateSearchInputToErrMsg

}
