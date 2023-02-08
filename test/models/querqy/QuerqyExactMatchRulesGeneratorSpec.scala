package models.querqy

import models.FeatureToggleModel.FeatureToggleService
import models.SearchManagementRepository
import models.input.{SearchInputId, SearchInput, SearchInputWithRules}
import models.rules._
import models.querqy.QuerqyRulesTxtGenerator

import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}

class QuerqyExactMatchRulesGeneratorSpec extends FlatSpec with Matchers with MockitoSugar {

    val searchManagementRepository = mock[SearchManagementRepository]
    val featureToggleService = mock[FeatureToggleService]

    val generator = new QuerqyRulesTxtGenerator(searchManagementRepository, featureToggleService)

    // Create some stressfull real-world use case (exact matching) example. Capital letters are on purpose.
    val synonymRules = List(
        SynonymRule(
            SynonymRuleId(),
            synonymType = SynonymRule.TYPE_UNDIRECTED,
            term = "Notebook",
            isActive = true
        ),
        SynonymRule(
            SynonymRuleId(),
            synonymType = SynonymRule.TYPE_UNDIRECTED,
            term = "\"Schläppi\"",
            isActive = true
        ),
        SynonymRule(
            SynonymRuleId(),
            synonymType = SynonymRule.TYPE_DIRECTED,
            term = "Portable PC",
            isActive = true
        ),
        SynonymRule(
            SynonymRuleId(),
            synonymType = SynonymRule.TYPE_DIRECTED,
            term = "Netbook",
            isActive = true
        )
    )
    val searchInputWithRules = SearchInputWithRules(
        SearchInputId(),
        term = "\"Laptop\"", // Exact match
        synonymRules = synonymRules,
        isActive = true,
        comment = ""
    )

    "Exact matching input" should "be identified as such" in {

        SearchInput.isExactMatch("    \"Laptop\"  ") shouldBe true
        SearchInput.isExactMatch("\"Laptop   \"") shouldBe true
        SearchInput.isExactMatch(searchInputWithRules.term) shouldBe true
    }
    
    it should "not be identified, if not completely defined" in {
        SearchInput.isExactMatch("Laptop") shouldBe false
        SearchInput.isExactMatch("\"Laptop") shouldBe false
        SearchInput.isExactMatch("Laptop\"") shouldBe false
    }

    // TODO Consider this specification to be changed with an upcoming (breaking) release
    "rules.txt" should "not specifically treat exact matching inputs nor undirected synonyms" in {
        val rulesTxt = generator.renderListSearchInputRules( List(searchInputWithRules) )

        rulesTxt should be(      
s""""Laptop" =>
	SYNONYM: Notebook
	SYNONYM: "Schläppi"
	SYNONYM: Portable PC
	SYNONYM: Netbook

Notebook =>
	SYNONYM: "Laptop"
	SYNONYM: "Schläppi"
	SYNONYM: Portable PC
	SYNONYM: Netbook

"Schläppi" =>
	SYNONYM: "Laptop"
	SYNONYM: Notebook
	SYNONYM: Portable PC
	SYNONYM: Netbook

""".stripMargin
        )

    }

}
