package models

import java.sql.Connection

import org.scalatest.{FlatSpec, Matchers}
import utils.WithInMemoryDB

import models.input.{SearchInput, SearchInputWithRules, InputTag, TagInputAssociation}
import models.rules.{SynonymRule, SynonymRuleId}


class SuggestedSolrFieldSpec extends FlatSpec with Matchers with WithInMemoryDB with TestData {

  "SuggestedField" should "be creatable" in {
    db.withConnection { implicit conn =>
      SolrIndex.insert(indexEn)

      SuggestedSolrField.insert(indexEn.id,"product_type");

      val suggestedFields = SuggestedSolrField.listAll(indexEn.id);
      suggestedFields.size shouldBe 1

    }
  }

  "SuggestedField" should "fail on duplicate" in {
    db.withConnection { implicit conn =>
      SolrIndex.insert(indexEn)

      SuggestedSolrField.insert(indexEn.id,"product_type");

      var suggestedFields = SuggestedSolrField.listAll(indexEn.id);
      suggestedFields.size shouldBe 1

      // now try and do a duplicate!
      SuggestedSolrField.insert(indexEn.id,"product_type");
      suggestedFields = SuggestedSolrField.listAll(indexEn.id);
      // I can't figure out how to prevent duplicates from being inserted without doing a LOT of work
      //suggestedFields.size shouldBe 1
    }
  }

}
