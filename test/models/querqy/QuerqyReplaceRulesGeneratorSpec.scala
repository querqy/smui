package models.querqy

import models.spellings.{AlternateSpelling, AlternateSpellingId, CanonicalSpellingId, CanonicalSpellingWithAlternatives}
import org.scalatest.{FlatSpec, Matchers}

class QuerqyReplaceRulesGeneratorSpec extends FlatSpec with Matchers {

  import models.querqy.{QuerqyReplaceRulesGenerator => generator}

  "Replace Rule Generation" should "correctly create REPLACE rules from spellings" in {
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
    replaceRule shouldBe "frozer; frazer; fräzer; frsadsadsv => freezer"
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

}
