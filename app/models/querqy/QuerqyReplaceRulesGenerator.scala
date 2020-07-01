package models.querqy

import java.io.{ByteArrayInputStream, InputStreamReader}
import java.nio.charset.StandardCharsets

import models.spellings.CanonicalSpellingWithAlternatives
import querqy.rewrite.commonrules.WhiteSpaceQuerqyParserFactory
import querqy.rewrite.contrib.replace.ReplaceRewriterParser

import scala.util.{Failure, Success, Try}

object QuerqyReplaceRulesGenerator {

  def renderAllCanonicalSpellingsToReplaceRules(allSpellings: Seq[CanonicalSpellingWithAlternatives]): String = {
    allSpellings.filter(_.exportToReplaceFile).map(renderReplaceRule).mkString("\n")
  }

  def renderReplaceRule(spelling: CanonicalSpellingWithAlternatives): String = {
    val alternativeSpellings = spelling.alternativeSpellings.map(_.term).mkString("; ")
    s"$alternativeSpellings => ${spelling.term}"
  }

  def validateQuerqyReplaceRulesTxtToErrMsg(spellings: CanonicalSpellingWithAlternatives): Option[String] = {
    validateQuerqyReplaceRulesTxtToErrMsg(renderReplaceRule(spellings))
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
