package models.spellings

import org.scalatest.{FlatSpec, Matchers}

class CanonicalSpellingValidatorSpec extends FlatSpec with Matchers {

  import models.spellings.{CanonicalSpellingValidator => validator}

  "Validation" should "reject empty alternatives" in {
    validator.validateNoEmptyAlternatives(alt("accessoire", Seq("assesoire", "   "))) shouldBe
      Some("Empty alternative term for 'accessoire'")
  }

  "Validation" should "reject duplicate alternatives" in {
    validator.validateNoDuplicateAlternativeSpellings(alt("accessoire", Seq("akzessor", "assesoire", "akzessor "))) shouldBe
      Some("Duplicate alternative spellings for 'accessoire': akzessor,akzessor")
  }

  "Validation" should "reject spelling alternatives that are the same as the canonical" in {
    validator.validateAlternativeSpellingNotCanonical(alt("accessoire", Seq("akzessor", " accessoire"))) shouldBe
      Some("Alternative spelling is same as the canonical spelling 'accessoire'")
  }

  "Validation" should "reject spelling alternatives that are the same as another canonical" in {
    val accessoire = alt("accessoire", Seq("assesoire", "zubehör"))
    val zubehoer = alt("zubehör", Seq("zubehor"))
    validator.validateAlternativeSpellingEqualsNoOtherCanonical(accessoire, List(accessoire, zubehoer)) shouldBe
      Some("Alternative spelling(s) exist as canonical spelling: zubehör")
  }

  "Validation" should "reject spelling canonicals that are the same as another alternative" in {
    val accessoire = alt("accessoire", Seq("assesoire", "zubehör"))
    val zubehoer = alt("zubehör", Seq("zubehor"))
    validator.validateCanonicalEqualsNoOtherAlternative(zubehoer, List(accessoire, zubehoer)) shouldBe
      Some("Canonical spelling zubehör is already an alternative spelling of accessoire")
  }

  "Validation" should "reject spelling if a wildcard is present and multiple  other alternatives" in {
    val accessoire = alt("accessoire", Seq("assesoire*", "zubehör"))
    validator.validateNoMultipleAlternativesWhenWildcard(accessoire) shouldBe
      Some("For suffix and prefix rules, only one input can be defined per output, e. g. a* => b")

    val zubehoer = alt("zubehör", Seq("assesoire", "zubehor*"))
    validator.validateNoMultipleAlternativesWhenWildcard(zubehoer) shouldBe
      Some("For suffix and prefix rules, only one input can be defined per output, e. g. a* => b")
  }

  private def alt(term: String, alternatives: Seq[String]): CanonicalSpellingWithAlternatives = {
    CanonicalSpellingWithAlternatives(
      CanonicalSpellingId("id"),
      term,
      alternatives.map(a => AlternativeSpelling(AlternativeSpellingId("altId"), CanonicalSpellingId("id"), a)).toList)
  }

}
