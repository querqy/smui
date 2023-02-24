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

    // Test exact identification methods

    "Whole exact matching input" should "be identified as such" in {
        SearchInput.isTermExact("    \"Laptop\"  ") shouldBe true
        SearchInput.isTermExact("\"Laptop   \"") shouldBe true
        SearchInput.isTermExact("\"Laptop\"") shouldBe true

        SearchInput.hasTermAnyExactInstruction("\"Laptop\"") shouldBe true
    }
    
    it should "not be identified, if not completely defined" in {
        SearchInput.isTermExact("Laptop") shouldBe false
        SearchInput.isTermExact("\"Laptop") shouldBe false
        SearchInput.isTermExact("Laptop\"") shouldBe false
    }

    "Left exact matching input" should "be identified as such" in {
        SearchInput.isTermLeftExact("    \"Laptop  ") shouldBe true
        SearchInput.isTermLeftExact("\"Laptop   ") shouldBe true
        SearchInput.isTermLeftExact("\"Laptop") shouldBe true
        // TODO This spec could be discussed in the future (vs. exact matches)
        SearchInput.isTermLeftExact("\"Laptop\"") shouldBe true

        SearchInput.hasTermAnyExactInstruction("\"Laptop") shouldBe true
    }
    
    it should "not be identified, if not defined" in {
        SearchInput.isTermExact("Laptop") shouldBe false
        SearchInput.isTermExact("Laptop\"") shouldBe false
    }

    "Right exact matching input" should "be identified as such" in {
        SearchInput.isTermRightExact("    Laptop\"  ") shouldBe true
        SearchInput.isTermRightExact("Laptop\"   ") shouldBe true
        SearchInput.isTermRightExact("Laptop\"") shouldBe true
        // TODO This spec could be discussed in the future (vs. exact matches)
        SearchInput.isTermLeftExact("\"Laptop\"") shouldBe true

        SearchInput.hasTermAnyExactInstruction("Laptop\"") shouldBe true
    }
    
    it should "not be identified, if not defined" in {
        SearchInput.isTermRightExact("Laptop") shouldBe false
        SearchInput.isTermRightExact("\"Laptop") shouldBe false
    }

    "No exact matching instruction" should "not be identified" in {
        SearchInput.hasTermAnyExactInstruction("Laptop") shouldBe false
    }

    // Test the function to derive an alternative input term (from an undirected synonym)

    "Deriven alternative input term" should "contain whole exact matching instruction" in {
        SearchInput.deriveAlternativeInput(
            "\"Laptop\"",
            "Notebook"
        ) shouldBe "\"Notebook\""
    }

    it should "contain left exact matching instruction" in {
        SearchInput.deriveAlternativeInput(
            "\"Laptop",
            "Notebook"
        ) shouldBe "\"Notebook"
    }

    it should "contain right exact matching instruction" in {
        SearchInput.deriveAlternativeInput(
            "Laptop\"",
            "Notebook"
        ) shouldBe "Notebook\""
    }

    it should "not contain any exact matching instruction, if the synonym term already contains one" in {
        SearchInput.deriveAlternativeInput(
            "\"Laptop\"",
            "\"Notebook"
        ) shouldBe "\"Notebook"

        SearchInput.deriveAlternativeInput(
            "\"Laptop\"",
            "Notebook\""
        ) shouldBe "Notebook\""

        SearchInput.deriveAlternativeInput(
            "\"Laptop\"",
            "\"Notebook\""
        ) shouldBe "\"Notebook\""
    }

    // TODO Test, how an exact matching input term is stripped down to a synonym

    "Stripping down an input to a synonym" should "work for whole exact matching term" in {
        SearchInput.stripDownInputToSynonym("\"Laptop\"") shouldBe "Laptop"
    }

    it should "work for left exact matching term" in {
        SearchInput.stripDownInputToSynonym("\"Laptop") shouldBe "Laptop"
    }
    
    it should "work for right exact matching term" in {
        SearchInput.stripDownInputToSynonym("\"Laptop") shouldBe "Laptop"
    }

    // Integrate the tests with the rules.txt generator

    def listSearchInputWithRules(forTerm: String, synonymRuleTermsTypes: Map[String, Int]) = {

        val listSynonymRules = synonymRuleTermsTypes map {
            case (synTerm: String, synType: Int) => {
                SynonymRule(
                    SynonymRuleId(),
                    synonymType = synType,
                    term = s"$synTerm",
                    isActive = true
                )
            }
        }
        
        List(
            SearchInputWithRules(
                SearchInputId(),
                term = forTerm,
                synonymRules = listSynonymRules.toList,
                isActive = true,
                comment = ""
            )
        )
    }

    "rules.txt generation" should "normally take over whole exact matching for undirected synonyms" in  {
        val rulesTxt = generator.renderListSearchInputRules(
            listSearchInputWithRules(
                "\"Laptop\"", // Whole exact match
                Map(
                    "Notebook" // NOT exact
                    -> SynonymRule.TYPE_UNDIRECTED
                )
            )
        )

//        print("[DEBUG] rulesTxt = <<<" + rulesTxt + ">>>")

        rulesTxt should be(
s""""Laptop" =>
	SYNONYM: Notebook

"Notebook" =>
	SYNONYM: Laptop

""".stripMargin
        )
    }

    it should "normally take over left exact matching for undirected synonyms" in  {
        val rulesTxt = generator.renderListSearchInputRules(
            listSearchInputWithRules(
                "\"Laptop", // Left exact match
                Map(
                    "Notebook" // NOT exact
                    -> SynonymRule.TYPE_UNDIRECTED
                )
            )
        )

//        print("[DEBUG] rulesTxt = <<<" + rulesTxt + ">>>")

        rulesTxt should be(
s""""Laptop =>
	SYNONYM: Notebook

"Notebook =>
	SYNONYM: Laptop

""".stripMargin
        )
    }

    it should "normally take over right exact matching for undirected synonyms" in  {
        val rulesTxt = generator.renderListSearchInputRules(
            listSearchInputWithRules(
                "Laptop\"", // Right exact match
                Map(
                    "Notebook" // NOT exact
                    -> SynonymRule.TYPE_UNDIRECTED
                )
            )
        )

//        print("[DEBUG] rulesTxt = <<<" + rulesTxt + ">>>")

        rulesTxt should be(
s"""Laptop" =>
	SYNONYM: Notebook

Notebook" =>
	SYNONYM: Laptop

""".stripMargin
        )
    }

    it should "not take over any exact matching instructions from the input to an undirected synonym, if those already contain exact matching instructions" in {
        val rulesTxt = generator.renderListSearchInputRules(
            listSearchInputWithRules(
                "\"Laptop\"", // Whole exact match
                Map(
                    "\"Notebook\"" // Also, whole exact match
                    -> SynonymRule.TYPE_UNDIRECTED,
                    "\"Schläppi" // Left exact match
                    -> SynonymRule.TYPE_UNDIRECTED,
                    "Portable PC\"" // Right exact match
                    -> SynonymRule.TYPE_UNDIRECTED
                )
            )
        )

//        print("[DEBUG] rulesTxt = <<<" + rulesTxt + ">>>")

        rulesTxt should be(
s""""Laptop" =>
	SYNONYM: "Notebook"
	SYNONYM: "Schläppi
	SYNONYM: Portable PC"

"Notebook" =>
	SYNONYM: Laptop
	SYNONYM: "Schläppi
	SYNONYM: Portable PC"

"Schläppi =>
	SYNONYM: Laptop
	SYNONYM: "Notebook"
	SYNONYM: Portable PC"

Portable PC" =>
	SYNONYM: Laptop
	SYNONYM: "Notebook"
	SYNONYM: "Schläppi

""".stripMargin
        )
    }

    it should "not take over nothing for directed synonyms" in {
        val rulesTxt = generator.renderListSearchInputRules(
            listSearchInputWithRules(
                "\"Laptop\"", // Whole exact match
                Map(
                    "Notebook"
                    -> SynonymRule.TYPE_UNDIRECTED,
                    "Portable PC"
                    -> SynonymRule.TYPE_UNDIRECTED,
                    "MacBook"
                    -> SynonymRule.TYPE_DIRECTED,
                    "Netbook"
                    -> SynonymRule.TYPE_DIRECTED
                )
            )
        )

//        print("[DEBUG] rulesTxt = <<<" + rulesTxt + ">>>")

        rulesTxt should be(
s""""Laptop" =>
	SYNONYM: Notebook
	SYNONYM: Portable PC
	SYNONYM: MacBook
	SYNONYM: Netbook

"Notebook" =>
	SYNONYM: Laptop
	SYNONYM: Portable PC
	SYNONYM: MacBook
	SYNONYM: Netbook

"Portable PC" =>
	SYNONYM: Laptop
	SYNONYM: Notebook
	SYNONYM: MacBook
	SYNONYM: Netbook

""".stripMargin
        )
    }

}
