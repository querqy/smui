package models

import java.time.LocalDateTime

import models.rules._
import org.scalatest.{FlatSpec, Matchers}
import play.api.db.Database

abstract class DBCompatibilitySpec extends FlatSpec with Matchers with TestData {

  protected def db: Database

  // Set millis/nanos of second to 0 since MySQL does not save them
  // and so comparisons would fail if they were set
  private val now = LocalDateTime.now().withNano(0)

  "Most important DB queries" should "work using this database" in {
    db.withConnection { implicit conn =>
      SolrIndex.insert(indexDe)
      SolrIndex.loadNameById(indexDe.id) shouldBe indexDe.name
      SolrIndex.listAll shouldBe Seq(indexDe)

      val tag = InputTag(InputTagId(), Some(indexDe.id), Some("testProperty"), "testValue",
        exported = true, predefined = false, now)
      InputTag.insert(tag)
      InputTag.loadAll() shouldBe Seq(tag)

      val input = SearchInput.insert(indexDe.id, "test")
      val inputWithRules = SearchInputWithRules(input.id, input.term,
        List(SynonymRule(SynonymRuleId(), SynonymRule.TYPE_UNDIRECTED, "testSynonym", isActive = true)),
        List(UpDownRule(UpDownRuleId(), UpDownRule.TYPE_UP, 5, "upDownTerm", isActive = true)),
        List(FilterRule(FilterRuleId(), "filterTerm", isActive = true)),
        List(DeleteRule(DeleteRuleId(), "deleteTerm", isActive = true)),
        List(RedirectRule(RedirectRuleId(), "/testTarget", isActive = true)),
        List(tag),
        true,
        "Some search input comment."
      )
      SearchInputWithRules.update(inputWithRules)
      SearchInputWithRules.loadById(input.id) shouldBe Some(inputWithRules)

      SearchInputWithRules.loadWithUndirectedSynonymsAndTagsForSolrIndexId(indexDe.id) shouldBe Seq(
        inputWithRules.copy(upDownRules = Nil, filterRules = Nil, deleteRules = Nil, redirectRules = Nil)
      )

      SearchInputWithRules.delete(input.id)
      SearchInputWithRules.loadById(input.id) shouldBe None

      val field1 = SuggestedSolrField.insert(indexDe.id, "title")
      val field2 = SuggestedSolrField.insert(indexDe.id, "description")
      SuggestedSolrField.listAll(indexDe.id).toSet shouldBe Set(field1, field2)

      InputTag.deleteByIds(Seq(tag.id))
      InputTag.loadAll() shouldBe Nil
    }
  }

}
