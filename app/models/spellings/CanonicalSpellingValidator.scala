package models.spellings

import models.querqy.QuerqyReplaceRulesGenerator

object CanonicalSpellingValidator {

  def validateCanonicalSpellingsAndAlternatives(spellings: CanonicalSpellingWithAlternatives,
                                                allCanonicalSpellings: List[CanonicalSpellingWithAlternatives]): Seq[String] = {
    Seq(
      validateNoEmptyAlternatives(spellings),
      validateNoDuplicateAlternateSpellings(spellings),
      validateAlternateSpellingNotCanonical(spellings),
      validateAlternateSpellingEqualsNoOtherCanonical(spellings, allCanonicalSpellings),
      validateCanonicalEqualsNoOtherAlternative(spellings, allCanonicalSpellings),
      QuerqyReplaceRulesGenerator.validateQuerqyReplaceRulesTxtToErrMsg(spellings)
    ).flatten
  }

  def validateNoEmptyAlternatives(spellings: CanonicalSpellingWithAlternatives): Option[String] = {
    spellings.alternateSpellings.map(_.term.trim).find(_.isEmpty).map { _ =>
      s"Empty alternative term for '${spellings.term}'"
    }
  }

  def validateNoDuplicateAlternateSpellings(spellings: CanonicalSpellingWithAlternatives): Option[String] = {
    val alternateSpellingDuplicates = spellings.alternateSpellings.map(_.term.trim).groupBy(identity).filter(_._2.size > 1).values.flatten.toSeq.sorted.mkString(",")
    if (alternateSpellingDuplicates.nonEmpty) {
      Some(s"Duplicate alternate spellings for '${spellings.term}': $alternateSpellingDuplicates")
    } else {
      None
    }
  }

  def validateAlternateSpellingNotCanonical(spellings: CanonicalSpellingWithAlternatives): Option[String] = {
    val alternateSpellings = spellings.alternateSpellings.map(_.term.trim)
    if (alternateSpellings.contains(spellings.term.trim)) {
      Some(s"Alternate spelling is same as the canonical spelling '${spellings.term}'")
    } else {
      None
    }
  }

  def validateAlternateSpellingEqualsNoOtherCanonical(spellings: CanonicalSpellingWithAlternatives,
                                                      allCanonicalSpellings: List[CanonicalSpellingWithAlternatives]): Option[String] = {
    val alternateSpellings = spellings.alternateSpellings.map(_.term)
    val allCanonicalTerms = allCanonicalSpellings.map(_.term)
    val intersection = allCanonicalTerms.intersect(alternateSpellings)
    if (intersection.nonEmpty) {
      Some(s"Alternate spelling(s) exist as canonical spelling: ${intersection.mkString(",")}")
    } else {
      None
    }
  }

  def validateCanonicalEqualsNoOtherAlternative(spellings: CanonicalSpellingWithAlternatives,
                                                allCanonicalSpellings: List[CanonicalSpellingWithAlternatives]): Option[String] = {
    val canonical = spellings.term
    // create map of all alternatives -> their canonical(s)
    val allSpellings = allCanonicalSpellings
      .flatMap(canonical => canonical.alternateSpellings.map(alternate => alternate.term -> canonical.term))
      .groupBy(_._1)
      .mapValues(_.map(_._2))
    allSpellings.get(canonical).map { canonicalsHavingThatAlternative =>
      s"Canonical spelling $canonical is already a managed alternative of ${canonicalsHavingThatAlternative.mkString(",")}"
    }
  }

}
