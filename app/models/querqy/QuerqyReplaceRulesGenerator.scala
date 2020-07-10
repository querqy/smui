package models.querqy

import java.io.{ByteArrayInputStream, InputStreamReader}
import java.nio.charset.StandardCharsets

import models.spellings.CanonicalSpellingWithAlternatives
import querqy.rewrite.commonrules.WhiteSpaceQuerqyParserFactory
import querqy.rewrite.contrib.replace.ReplaceRewriterParser

import scala.util.{Failure, Success, Try}

object QuerqyReplaceRulesGenerator {

  def renderAllCanonicalSpellingsToReplaceRules(allSpellings: Seq[CanonicalSpellingWithAlternatives]): String = {
    allSpellings.filter(_.exportToReplaceFile).flatMap(renderReplaceRule).sorted.mkString("\n")
  }

  def renderReplaceRule(spelling: CanonicalSpellingWithAlternatives): Option[String] = {
    val alternativeSpellings = spelling.alternativeSpellings
      .filter(_.isActive).map(_.term).sorted.mkString("; ")

    if (spelling.isActive) {
      Some(s"$alternativeSpellings => ${spelling.term}")
    } else None
  }

  def validateQuerqyReplaceRulesTxtToErrMsg(spellings: CanonicalSpellingWithAlternatives): Option[String] = {
    val renderedRule = renderReplaceRule(spellings)
    renderedRule.flatMap {
      case rule => validateQuerqyReplaceRulesTxtToErrMsg(rule)
      case _ => None
    }
  }

  def validateQuerqyReplaceRulesTxtToErrMsg(rulesString: String): Option[String] = {
    Try {
      new ReplaceRewriterParser(
        new InputStreamReader(new ByteArrayInputStream(rulesString.getBytes(StandardCharsets.UTF_8))), false, "\n",
        new WhiteSpaceQuerqyParserFactory().createParser()
      ).parseConfig()
    } match {
      case Success(_) => None
      case Failure(e) =>
        // TODO better parse the returned Exception and return a line-wise error object making validation errors assign-able to specific rules
        Some(e.getMessage)
    }
  }

}
