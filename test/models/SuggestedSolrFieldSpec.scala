package models


import org.h2.jdbc.JdbcSQLException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import utils.WithInMemoryDB



class SuggestedSolrFieldSpec extends AnyFlatSpec with Matchers with WithInMemoryDB with TestData {

  "SuggestedField" should "be creatable" in {
    db.withConnection { implicit conn =>
      SolrIndex.insert(indexEn)

      SuggestedSolrField.insert(indexEn.id,"product_type");

      val suggestedFields = SuggestedSolrField.listAll(indexEn.id);
      suggestedFields.size shouldBe 1

    }
  }

  //"SuggestedField" should "fail on duplicate" in {
  it should "not allow inserting the same suggested field more than once" in {
    db.withConnection { implicit connection =>
      SolrIndex.insert(indexEn)

      SuggestedSolrField.insert(indexEn.id,"product_type");

      var suggestedFields = SuggestedSolrField.listAll(indexEn.id);
      suggestedFields.size shouldBe 1

      // now try and do a duplicate!
      db.withConnection { implicit connection =>
        intercept[JdbcSQLException] {
          SuggestedSolrField.insert(indexEn.id,"product_type")
        }
      }

      // Same field name, different solr index
      SolrIndex.insert(indexDe)
      SuggestedSolrField.insert(indexDe.id,"product_type");
      suggestedFields = SuggestedSolrField.listAll(indexDe.id);
      suggestedFields.size shouldBe 1
    }
  }

}
