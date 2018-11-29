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


  "Rules Text Generation" should "correctly decorate SYNONYM" in {
    val featureToggleMock = mock[FeatureToggleService]
    when(featureToggleMock.getToggleRuleDeploymentAutoDecorateExportHash).thenReturn(true)
    val synonymRules = List (SynonymRule(None, 0, "mercury", true))

    val classUnderTest = new QuerqyRulesTxtGenerator(searchManagementRepository, featureToggleMock)
    val rulesTxt  = classUnderTest.renderSearchInputRulesForTerm("queen", SearchInput(term = "queen", synonymRules = synonymRules))
    rulesTxt should startWith(
      s"""|queen =>
          |\tSYNONYM: mercury
          |\tDECORATE: [ {
          |"intent":"smui.auto-decorate.export-hash",
          |"payload": {
          | "ruleExportDate":"""".stripMargin)
        .and(endWith(
          """|"ruleExportHash":"31581570"
             |}
             |} ]""".stripMargin))
  }


}
