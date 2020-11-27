package models.spellings

import models.querqy.QuerqyReplaceRulesGenerator

// TODO add test for validator
object CanonicalSpellingValidator {

  def validateCanonicalSpellingsAndAlternatives(spellings: CanonicalSpellingWithAlternatives,
                                                allCanonicalSpellings: List[CanonicalSpellingWithAlternatives]): Seq[String] = {
    Seq(
      validateNoEmptySpelling(spellings.term),
      validateNoEmptyAlternatives(spellings),
      validateNoDuplicateAlternativeSpellingsSameCanonical(spellings),
      validateNoDuplicateAlternativeSpellingsOtherCanonical(spellings, allCanonicalSpellings),
      validateAlternativeSpellingNotCanonical(spellings),
      validateAlternativeSpellingEqualsNoOtherCanonical(spellings, allCanonicalSpellings),
      validateCanonicalEqualsNoOtherAlternative(spellings, allCanonicalSpellings),
      validateNoMultipleAlternativesWhenWildcard(spellings),
      QuerqyReplaceRulesGenerator.validateQuerqyReplaceRulesTxtToErrMsg(spellings)
    ).flatten
  }

  def validateNoEmptySpelling(spellingTerm: String): Option[String] = {
    if(spellingTerm.trim.isEmpty) {
      Some(s"Invalid empty spelling for term '$spellingTerm'")
    } else {
      None
    }
  }

  def validateNoEmptyAlternatives(spellings: CanonicalSpellingWithAlternatives): Option[String] = {
    spellings.alternativeSpellings.map(_.term.trim).find(_.isEmpty).map { _ =>
      s"Empty alternative term for '${spellings.term}'"
    }
  }

  def validateNoDuplicateAlternativeSpellingsSameCanonical(spellings: CanonicalSpellingWithAlternatives): Option[String] = {
    val alternativeSpellingDuplicates = spellings.alternativeSpellings
      .map(_.term.toLowerCase.trim)
      .groupBy(identity).filter(_._2.size > 1)
      .values.flatten.toSeq.sorted.mkString(",")

    if (alternativeSpellingDuplicates.nonEmpty) {
      Some(s"Duplicate alternative spellings for '${spellings.term}': $alternativeSpellingDuplicates")
    } else {
      None
    }
  }

  def validateNoDuplicateAlternativeSpellingsOtherCanonical(spellings: CanonicalSpellingWithAlternatives,
                                                            allCanonicalSpellings: List[CanonicalSpellingWithAlternatives]): Option[String] = {
    val allAlternativesWithCanonical: Seq[(String, String)] = (allCanonicalSpellings :+ spellings)
      .flatMap(spelling => spelling.alternativeSpellings.map(alternative => spelling.term.toLowerCase.trim -> alternative.term.toLowerCase.trim))

    val alternativeSpellingDuplicates: Seq[(String, String)] = allAlternativesWithCanonical
      .groupBy(_._2).filter(_._2.size > 1).values.flatten.toSeq.sorted

    if (alternativeSpellingDuplicates.nonEmpty) {
      val canonicalsWithDuplicate = alternativeSpellingDuplicates
        .filter(_._1 != spellings.term)
        .map(duplicate => s"${duplicate._1} -> ${duplicate._2}")
        .mkString(", ")

      Some(s"Duplicate alternative spellings in other spelling '$canonicalsWithDuplicate'")
    } else {
      None
    }
  }

  def validateAlternativeSpellingNotCanonical(spellings: CanonicalSpellingWithAlternatives): Option[String] = {
    val alternativeSpellings = spellings.alternativeSpellings.map(_.term.toLowerCase.trim)
    if (alternativeSpellings.contains(spellings.term.toLowerCase.trim)) {
      Some(s"Alternative spelling is same as the canonical spelling '${spellings.term}'")
    } else {
      None
    }
  }

  def validateAlternativeSpellingEqualsNoOtherCanonical(spellings: CanonicalSpellingWithAlternatives,
                                                        allCanonicalSpellings: List[CanonicalSpellingWithAlternatives]): Option[String] = {
    val alternativeSpellings = spellings.alternativeSpellings.map(_.term.toLowerCase.trim)
    val allCanonicalTerms = allCanonicalSpellings.map(_.term.toLowerCase.trim)
    val intersection = allCanonicalTerms.intersect(alternativeSpellings)
    if (intersection.nonEmpty) {
      Some(s"Alternative spelling(s) exist as canonical spelling: ${intersection.mkString(",")}")
    } else {
      None
    }
  }

  def validateCanonicalEqualsNoOtherAlternative(spellings: CanonicalSpellingWithAlternatives,
                                                allCanonicalSpellings: List[CanonicalSpellingWithAlternatives]): Option[String] = {
    val canonical = spellings.term.toLowerCase.trim
    // create map of all alternatives -> their canonical(s)
    val allSpellings = allCanonicalSpellings
      .flatMap(canonical => canonical.alternativeSpellings.map(alternative => alternative.term.toLowerCase.trim -> canonical.term.toLowerCase.trim))
      .groupBy(_._1)
      .mapValues(_.map(_._2))
    allSpellings.get(canonical).map { canonicalsHavingThatAlternative =>
      s"Canonical spelling $canonical is already an alternative spelling of ${canonicalsHavingThatAlternative.mkString(",")}"
    }
  }

  def validateNoMultipleAlternativesWhenWildcard(spellings: CanonicalSpellingWithAlternatives): Option[String] = {
    val hasWildcard = spellings.alternativeSpellings.exists(_.term.toLowerCase.trim.contains("*"))
    if (hasWildcard && spellings.alternativeSpellings.length > 1) {
      Some("For suffix and prefix rules, only one input can be defined per output, e. g. a* => b")
    } else {
      None
    }
  }

}
