package models

import org.h2.jdbc.JdbcSQLException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import utils.WithInMemoryDB


class SolrIndexSpec extends AnyFlatSpec with Matchers with WithInMemoryDB with TestData {

  "SolrIndex" should "be creatable" in {
    db.withConnection { implicit conn =>
      SolrIndex.insert(indexEn)
      SolrIndex.insert(indexDe)

      val solrIndexes = SolrIndex.listAll
      solrIndexes.size shouldBe 2

    }
  }

  it should "not allow inserting the same SolrIndex more than once" in {
    db.withConnection { implicit connection =>
      SolrIndex.insert(indexEn)

      var solrIndexes = SolrIndex.listAll
      solrIndexes.size shouldBe 1

      // now try and do a duplicate!
      db.withConnection { implicit connection =>
        intercept[JdbcSQLException] {
          SolrIndex.insert(indexEn)
        }
      }

      // different solr index
      SolrIndex.insert(indexDe)
      solrIndexes = SolrIndex.listAll
      solrIndexes.size shouldBe 2
    }
  }
}
