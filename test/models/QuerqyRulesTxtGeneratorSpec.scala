package models

import models.FeatureToggleModel.FeatureToggleService
import models.SearchManagementModel.{SearchInput, UpDownRule}
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


}
