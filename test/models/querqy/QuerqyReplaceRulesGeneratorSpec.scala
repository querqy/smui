package models.querqy

import models.spellings.{AlternativeSpelling, AlternativeSpellingId, CanonicalSpellingId, CanonicalSpellingWithAlternatives}
import org.scalatest.{FlatSpec, Matchers}

class QuerqyReplaceRulesGeneratorSpec extends FlatSpec with Matchers {

  import models.querqy.{QuerqyReplaceRulesGenerator => generator}

  "Replace Rule Generation" should "correctly create REPLACE rules from spellings" in {
    val canonicalSpellingId = CanonicalSpellingId()
    val canonicalSpelling = CanonicalSpellingWithAlternatives(
      canonicalSpellingId, "freezer", true, "", List(
        AlternativeSpelling(AlternativeSpellingId(), canonicalSpellingId, "frozer", true),
        AlternativeSpelling(AlternativeSpellingId(), canonicalSpellingId, "frazer", true),
        AlternativeSpelling(AlternativeSpellingId(), canonicalSpellingId, "fräzer", true),
        AlternativeSpelling(AlternativeSpellingId(), canonicalSpellingId, "frsadsadsv", true),
      )
    )

    val replaceRule = generator.renderReplaceRule(canonicalSpelling)
    replaceRule shouldBe Some("frazer; frozer; frsadsadsv; fräzer => freezer")
  }

  it should "not render a replace rule if the spelling is marked inactive" in {
    val canonicalSpellingId = CanonicalSpellingId()
    val canonicalSpelling = CanonicalSpellingWithAlternatives(
      canonicalSpellingId, "freezer", isActive = false, "", List(
        AlternativeSpelling(AlternativeSpellingId(), canonicalSpellingId, "frozer", true),
        AlternativeSpelling(AlternativeSpellingId(), canonicalSpellingId, "frazer", true),
        AlternativeSpelling(AlternativeSpellingId(), canonicalSpellingId, "fräzer", true),
        AlternativeSpelling(AlternativeSpellingId(), canonicalSpellingId, "frsadsadsv", true),
      )
    )

    val replaceRule = generator.renderReplaceRule(canonicalSpelling)
    replaceRule shouldBe None
  }

  it should "not render an alternative spelling if it is inactive" in {
    val canonicalSpellingId = CanonicalSpellingId()
    val canonicalSpelling = CanonicalSpellingWithAlternatives(
      canonicalSpellingId, "freezer", true, "", List(
        AlternativeSpelling(AlternativeSpellingId(), canonicalSpellingId, "frozer", false),
        AlternativeSpelling(AlternativeSpellingId(), canonicalSpellingId, "frazer", true),
        AlternativeSpelling(AlternativeSpellingId(), canonicalSpellingId, "fräzer", false),
      )
    )

    val replaceRule = generator.renderReplaceRule(canonicalSpelling)
    replaceRule shouldBe Some("frazer => freezer")
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
