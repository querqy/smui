package models

import models.FeatureToggleModel.FeatureToggleService
import models.SearchManagementModel._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}

class QuerqyRulesTxtGeneratorSpec extends FlatSpec with Matchers with MockitoSugar {

  val searchManagementRepository = mock[SearchManagementRepository]
  val featureToggleService = mock[FeatureToggleService]

  val generator = new QuerqyRulesTxtGenerator(searchManagementRepository, featureToggleService)

  "Rules Text Generation" should "consider up/down rules correctly" in {
    val upDownRules = List(
      UpDownRule(None, 0, 10, "notebook", true),
      UpDownRule(None, 0, 10, "lenovo", false),
      UpDownRule(None, 1, 10, "battery", true)
    )
    val rulesTxt = generator.renderSearchInputRulesForTerm("laptop", SearchInput(term = "laptop", upDownRules = upDownRules))

    rulesTxt should be(
      s"""|laptop =>
          |\tUP(10): notebook
          |\tDOWN(10): battery
          |""".stripMargin)
  }


  "Rules Text Generation" should "correctly write a DELETE rules" in {
    val deleteRules = List (DeleteRule(None, "freddy", true))

    val rulesTxt  = generator.renderSearchInputRulesForTerm("queen", SearchInput(term = "queen", deleteRules = deleteRules))
    rulesTxt should be(
      s"""|queen =>
          |\tDELETE: freddy
          |""".stripMargin)
  }

  "Rules Text Generation" should "correctly write a undirected SYNONYM rules" in {
    val synonymRules = List (SynonymRule(None, 0, "mercury", true))

    val rulesTxt  = generator.renderSearchInputRulesForTerm("queen", SearchInput(term = "queen", synonymRules = synonymRules))
    rulesTxt should be(
      s"""|queen =>
          |\tSYNONYM: mercury
          |""".stripMargin)
  }


  "Rules Text Generation" should "correctly add FILTER rules" in {
    val filterRules = List (FilterRule(None, "zz top", true))

    val rulesTxt  = generator.renderSearchInputRulesForTerm("abba", SearchInput(term = "abba", filterRules = filterRules))
    rulesTxt should be(
      s"""|abba =>
          |\tFILTER: zz top
          |""".stripMargin)
  }

  "Rules Text Generation" should "correctly combine SYNONYM, FILTER, DELETE and UPDOWN Rules" in {
    val synonymRules = List (SynonymRule(None, 0, "mercury", true))
    val upDownRules = List(
      UpDownRule(None, 0, 10, "notebook", true),
      UpDownRule(None, 0, 10, "lenovo", false),
      UpDownRule(None, 1, 10, "battery", true)
    )
    val deleteRules = List (DeleteRule(None, "freddy", true))
    val filterRules = List (FilterRule(None, "zz top", true))
    val rulesTxt  = generator.renderSearchInputRulesForTerm("aerosmith",
      SearchInput(term = "aerosmith", filterRules = filterRules,
        synonymRules = synonymRules, deleteRules = deleteRules, upDownRules = upDownRules))

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

    val synonymRules = List (SynonymRule(None, 0, "mercury", true))

    val classUnderTest = new QuerqyRulesTxtGenerator(searchManagementRepository, featureToggleMock)
    val rulesTxt  = classUnderTest.renderSearchInputRulesForTerm("queen",
      SearchInput(id = Some("rule-id"), "queen", synonymRules = synonymRules))
    rulesTxt should be(
      s"""|queen =>
          |\tSYNONYM: mercury
          |\t@_log: "rule-id"
          |""".stripMargin)
  }

  // TODO outsource whole rules.txt file to an external test ressource
  val VALID_RULES_TXT =s""""handy" =>
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
    generator.validateQuerqyRulesTxtToErrMsg(VALID_RULES_TXT) should be (None)
  }

  "rules.txt validation" should "return an error when validating an invalid rules.txt" in {
    generator.validateQuerqyRulesTxtToErrMsg(VALID_RULES_TXT + "\nADD AN INVALID INSTRUCTION") should be
      Some("Line 31: Cannot parse line: ADD AN INVALID INSTRUCTION")
  }

  // TODO add tests for validateSearchInputToErrMsg

}
