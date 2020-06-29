package models.spellings

import org.scalatest.{FlatSpec, Matchers}

class CanonicalSpellingValidatorSpec extends FlatSpec with Matchers {

  import models.spellings.{CanonicalSpellingValidator => validator}

  "Validation" should "reject empty alternatives" in {
    validator.validateNoEmptyAlternatives(alt("accessoire", Seq("assesoire", "   "))) shouldBe
      Some("Empty alternative term for 'accessoire'")
  }

  "Validation" should "reject duplicate alternatives" in {
    validator.validateNoDuplicateAlternateSpellings(alt("accessoire", Seq("akzessor", "assesoire", "akzessor "))) shouldBe
      Some("Duplicate alternate spellings for 'accessoire': akzessor,akzessor")
  }

  "Validation" should "reject spelling alternatives that are the same as the canonical" in {
    validator.validateAlternateSpellingNotCanonical(alt("accessoire", Seq("akzessor", " accessoire"))) shouldBe
      Some("Alternate spelling is same as the canonical spelling 'accessoire'")
  }

  "Validation" should "reject spelling alternatives that are the same as another canonical" in {
    val accessoire = alt("accessoire", Seq("assesoire", "zubehör"))
    val zubehoer = alt("zubehör", Seq("zubehor"))
    validator.validateAlternateSpellingEqualsNoOtherCanonical(accessoire, List(accessoire, zubehoer)) shouldBe
      Some("Alternate spelling(s) exist as canonical spelling: zubehör")
  }

  "Validation" should "reject spelling canonicals that are the same as another alternative" in {
    val accessoire = alt("accessoire", Seq("assesoire", "zubehör"))
    val zubehoer = alt("zubehör", Seq("zubehor"))
    validator.validateCanonicalEqualsNoOtherAlternative(zubehoer, List(accessoire, zubehoer)) shouldBe
      Some("Canonical spelling zubehör is already a managed alternative of accessoire")
  }

  private def alt(term: String, alternatives: Seq[String]): CanonicalSpellingWithAlternatives = {
    CanonicalSpellingWithAlternatives(
      CanonicalSpellingId("id"),
      term,
      alternatives.map(a => AlternateSpelling(AlternateSpellingId("altId"), CanonicalSpellingId("id"), a)).toList)
  }

}
