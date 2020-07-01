package models.spellings

import models.querqy.QuerqyReplaceRulesGenerator

object CanonicalSpellingValidator {

  def validateCanonicalSpellingsAndAlternatives(spellings: CanonicalSpellingWithAlternatives,
                                                allCanonicalSpellings: List[CanonicalSpellingWithAlternatives]): Seq[String] = {
    Seq(
      validateNoEmptyAlternatives(spellings),
      validateNoDuplicateAlternativeSpellings(spellings),
      validateAlternativeSpellingNotCanonical(spellings),
      validateAlternativeSpellingEqualsNoOtherCanonical(spellings, allCanonicalSpellings),
      validateCanonicalEqualsNoOtherAlternative(spellings, allCanonicalSpellings),
      QuerqyReplaceRulesGenerator.validateQuerqyReplaceRulesTxtToErrMsg(spellings)
    ).flatten
  }

  def validateNoEmptyAlternatives(spellings: CanonicalSpellingWithAlternatives): Option[String] = {
    spellings.alternativeSpellings.map(_.term.trim).find(_.isEmpty).map { _ =>
      s"Empty alternative term for '${spellings.term}'"
    }
  }

  def validateNoDuplicateAlternativeSpellings(spellings: CanonicalSpellingWithAlternatives): Option[String] = {
    val alternativeSpellingDuplicates = spellings.alternativeSpellings.map(_.term.trim).groupBy(identity).filter(_._2.size > 1).values.flatten.toSeq.sorted.mkString(",")
    if (alternativeSpellingDuplicates.nonEmpty) {
      Some(s"Duplicate alternative spellings for '${spellings.term}': $alternativeSpellingDuplicates")
    } else {
      None
    }
  }

  def validateAlternativeSpellingNotCanonical(spellings: CanonicalSpellingWithAlternatives): Option[String] = {
    val alternativeSpellings = spellings.alternativeSpellings.map(_.term.trim)
    if (alternativeSpellings.contains(spellings.term.trim)) {
      Some(s"Alternative spelling is same as the canonical spelling '${spellings.term}'")
    } else {
      None
    }
  }

  def validateAlternativeSpellingEqualsNoOtherCanonical(spellings: CanonicalSpellingWithAlternatives,
                                                        allCanonicalSpellings: List[CanonicalSpellingWithAlternatives]): Option[String] = {
    val alternativeSpellings = spellings.alternativeSpellings.map(_.term)
    val allCanonicalTerms = allCanonicalSpellings.map(_.term)
    val intersection = allCanonicalTerms.intersect(alternativeSpellings)
    if (intersection.nonEmpty) {
      Some(s"Alternative spelling(s) exist as canonical spelling: ${intersection.mkString(",")}")
    } else {
      None
    }
  }

  def validateCanonicalEqualsNoOtherAlternative(spellings: CanonicalSpellingWithAlternatives,
                                                allCanonicalSpellings: List[CanonicalSpellingWithAlternatives]): Option[String] = {
    val canonical = spellings.term
    // create map of all alternatives -> their canonical(s)
    val allSpellings = allCanonicalSpellings
      .flatMap(canonical => canonical.alternativeSpellings.map(alternative => alternative.term -> canonical.term))
      .groupBy(_._1)
      .mapValues(_.map(_._2))
    allSpellings.get(canonical).map { canonicalsHavingThatAlternative =>
      s"Canonical spelling $canonical is already an alternative spelling of ${canonicalsHavingThatAlternative.mkString(",")}"
    }
  }

}
