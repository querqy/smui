package models

import models.input.{InputTag, SearchInput}
import models.spellings.CanonicalSpelling
import org.scalatest.{FlatSpec, Matchers}


class SearchManagementRepositorySpec extends FlatSpec with Matchers with TestData with ApplicationTestBase {

  protected def createTestCore(): Unit = {
    repo.addNewSolrIndex(SolrIndex(core1Id, "core1", "First core"))
  }
  it should "not allow deleting a SolrIndex that has links to other objects" in {

    // Test checking for an InputTag
    db.withConnection { implicit connection =>
      createTestCore()
      val tag = InputTag.create(Some(core1Id), Some("tenant"), "MO", exported = true)
      InputTag.insert(tag)
      intercept[Exception] {
        repo.deleteSolrIndex(core1Id.id)
      }
      InputTag.deleteByIds(Seq(tag.id))
      repo.deleteSolrIndex(core1Id.id)
    }

    // Test checking for an CanonicalSpelling
    db.withConnection { implicit connection =>
      createTestCore()
      val spelling = CanonicalSpelling.insert(core1Id, "colour")
      intercept[Exception] {

        repo.deleteSolrIndex(core1Id.id)
      }
      CanonicalSpelling.delete(spelling.id)
      repo.deleteSolrIndex(core1Id.id)
    }

    // Test checking for an SearchInput
    db.withConnection { implicit connection =>
      createTestCore()
      val input = SearchInput.insert(core1Id, "my input")
      intercept[Exception] {
        repo.deleteSolrIndex(core1Id.id)
      }
      SearchInput.delete(input.id)
      repo.deleteSolrIndex(core1Id.id)
    }

    // Currently can't programmatically create a DeploymentLog with a solr_index_id so not testing.
    //val deploymentLog = DeploymentLog.insert()

  }
}
