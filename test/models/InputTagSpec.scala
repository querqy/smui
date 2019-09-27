package models

import java.time.LocalDateTime

import org.h2.jdbc.JdbcBatchUpdateException
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}
import utils.WithInMemoryDB

class InputTagSpec extends FlatSpec with Matchers with BeforeAndAfterEach with WithInMemoryDB {

  private val index = SolrIndex(name = "de", description = "German")
  private val inputTags = Seq(
    InputTag.create(None, None, "good rule", exported = false),
    InputTag.create(None, Some("tenant"), "MO", exported = true),
    InputTag.create(Some(index.id), Some("tenant"), "MO_DE", exported = true),
    InputTag.create(Some(index.id), Some("tenant"), "MO_AT", exported = true)
  )

  "InputTag" should "be saved to the database and read out again" in {
    db.withConnection { implicit connection =>
      SolrIndex.insert(index)
      InputTag.insert(inputTags: _*)

      val loaded = InputTag.loadAll()

      // Accuracy of lastUpdate before/after database insert should omit nano seconds
      val inputTagsAdjustedTimeAccuracy = inputTags.map(inputTag =>
        inputTag.copy(lastUpdate = inputTag.lastUpdate.withNano(0))
      )
      val loadedAdjustedTimeAccuracy = loaded.map(inputTag =>
        inputTag.copy(lastUpdate = inputTag.lastUpdate.withNano(0))
      )

      loadedAdjustedTimeAccuracy.toSet shouldBe inputTagsAdjustedTimeAccuracy.toSet
    }

  }

  it should "not allow inserting the same tag more than once" in {
    val tag = InputTag.create(None, Some("tenant"), "MO", exported = true)

    db.withConnection { implicit connection =>
      intercept[JdbcBatchUpdateException] {
        InputTag.insert(tag, tag)
      }
    }
  }

  it should "be assignable to search-inputs" in {
    db.withConnection { implicit connection =>
      SolrIndex.insert(index)

      val term1 = SearchInput.insert(index.id, "term1")
      val term2 = SearchInput.insert(index.id, "term2")

      InputTag.insert(inputTags: _*)

      TagInputAssociation.updateTagsForSearchInput(term1.id, inputTags.map(_.id))

      TagInputAssociation.loadTagsBySearchInputId(term1.id).toSet shouldBe inputTags.toSet
      TagInputAssociation.loadTagsBySearchInputId(term2.id) shouldBe Nil

      TagInputAssociation.updateTagsForSearchInput(term1.id, Seq(inputTags.head.id))
      TagInputAssociation.loadTagsBySearchInputId(term1.id) shouldBe Seq(inputTags.head)
    }
  }

  it should "update predefined tags from a json file" in {
    db.withConnection { implicit connection =>
      SolrIndex.insert(index)
      val frIndex = SolrIndex(name = "fr", description = "French")
      SolrIndex.insert(frIndex)
      InputTag.insert(inputTags: _*)

      val predefined = PredefinedTag.fromStream(getClass.getResourceAsStream("/TestPredefinedTags.json"))
      val (deleted, inserted) = PredefinedTag.updateInDB(predefined)
      deleted.size shouldBe 0
      // 9 tags, but 3 of them were already present before (defined here in inputTags)
      inserted.size shouldBe 6

      // Insert again, should have no effect:
      val (deleted2, inserted2) = PredefinedTag.updateInDB(predefined)
      deleted2.size shouldBe 0
      inserted2.size shouldBe 0

      val loaded = InputTag.loadAll()
      loaded.count(_.predefined) shouldBe 6
      loaded.count(!_.predefined) shouldBe 4
      val loadedPredef = loaded.filter(_.predefined)
      loadedPredef.flatMap(_.solrIndexId).toSet shouldBe Set(index.id, frIndex.id)

      val (fixedId, now) = (InputTagId(), LocalDateTime.now())
      loadedPredef.find(_.value == "MO_FR").get.copy(id = fixedId, lastUpdate = now) shouldBe InputTag(fixedId,
        Some(frIndex.id), Some("tenant"), "MO_FR", exported = true, predefined = true, lastUpdate = now)

      // Update with only one predefined tag:
      val (deleted3, inserted3) = PredefinedTag.updateInDB(Seq(predefined.last))
      deleted3.size shouldBe 5
      inserted3.size shouldBe 0

      InputTag.loadAll().count(_.predefined) shouldBe 1
    }
  }



}
