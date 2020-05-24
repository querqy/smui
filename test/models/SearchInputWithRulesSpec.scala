package models

import java.sql.Connection

import models.rules.{SynonymRule, SynonymRuleId}
import org.scalatest.{FlatSpec, Matchers}
import utils.WithInMemoryDB

class SearchInputWithRulesSpec extends FlatSpec with Matchers with WithInMemoryDB with TestData {

  private val tag = InputTag.create(None, Some("tenant"), "MO", exported = true)

  "SearchInputWithRules" should "load lists with hundreds of entries successfully" in {
    db.withConnection { implicit conn =>
      SolrIndex.insert(indexDe)
      SolrIndex.insert(indexEn)
      InputTag.insert(tag)

      insertInputs(300, indexDe.id, "term_de")
      insertInputs(200, indexEn.id, "term_en")

      val inputsDe = SearchInputWithRules.loadWithUndirectedSynonymsAndTagsForSolrIndexId(indexDe.id)
      inputsDe.size shouldBe 300
      for (input <- inputsDe) {
        input.term should startWith("term_de_")
        input.tags.size shouldBe 1
        input.tags.head.displayValue shouldBe "tenant:MO"
        input.synonymRules.size shouldBe 1 // Only undirected synonyms should be loaded
        input.synonymRules.head.term should startWith("term_de_synonym_")
      }

      SearchInputWithRules.loadWithUndirectedSynonymsAndTagsForSolrIndexId(indexEn.id).size shouldBe 200
    }
  }

  private def insertInputs(count: Int, indexId: SolrIndexId, termPrefix: String)(implicit conn: Connection): Unit = {
    for (i <- 0 until count) {
      val input = SearchInput.insert(indexId, s"${termPrefix}_$i")
      SynonymRule.updateForSearchInput(input.id, Seq(
        SynonymRule(SynonymRuleId(), SynonymRule.TYPE_UNDIRECTED, s"${termPrefix}_synonym_$i", isActive = true),
        SynonymRule(SynonymRuleId(), SynonymRule.TYPE_DIRECTED, s"${termPrefix}_directedsyn_$i", isActive = true),
      ))
      TagInputAssociation.updateTagsForSearchInput(input.id, Seq(tag.id))
    }
  }

  "SearchInputWithRules" should "be (de)activatable" in {
    db.withConnection { implicit conn =>
      SolrIndex.insert(indexDe)

      val input = SearchInput.insert(indexDe.id, "my input")
      input.isActive shouldBe true

      SearchInput.update(input.id, input.term, false, input.comment)
      SearchInput.loadById(input.id).get.isActive shouldBe false

      SearchInput.update(input.id, input.term, true, input.comment)
      SearchInput.loadById(input.id).get.isActive shouldBe true
    }
  }

  "SearchInputWithRules" should "have a modifiable comment" in {
    db.withConnection { implicit conn =>
      SolrIndex.insert(indexDe)

      val input = SearchInput.insert(indexDe.id, "my input")
      input.comment shouldBe ""

      SearchInput.update(input.id, input.term, input.isActive, "My #magic comment.")
      SearchInput.loadById(input.id).get.comment shouldBe "My #magic comment."

      SearchInput.update(input.id, input.term, input.isActive, "My #magic comment - updated.")
      SearchInput.loadById(input.id).get.comment shouldBe "My #magic comment - updated."
    }
  }

}
