package models.eventhistory

import java.time.LocalDateTime

import models.ApplicationTestBase
import models.input.{FullSearchInputWithRules, SearchInputId, SearchInputWithRules}
import models.spellings.{AlternativeSpelling, CanonicalSpellingId, CanonicalSpellingWithAlternatives, FullCanonicalSpellingWithAlternatives}
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.Json

class EventHistorySpec extends FlatSpec with Matchers with ApplicationTestBase {

  private var inputIds: Seq[SearchInputId] = Seq.empty
  private var spellingIds: Seq[CanonicalSpellingId] = Seq.empty

  private var tStart: LocalDateTime = LocalDateTime.now()

  override protected lazy val additionalAppConfig = Seq(
      "toggle.activate-eventhistory" -> true
    )

  override protected def beforeAll(): Unit = {
    super.beforeAll()

    tStart = LocalDateTime.now()

    createTestCores()
    inputIds = createTestRule()
    spellingIds = createTestSpellings()
  }

  "CREATED events" should "be present for search inputs" in {
    db.withConnection { implicit conn =>

      // input#0 (aerosmith)

      val inputEvents0 = InputEvent.loadForId(inputIds(0).id)
      inputEvents0.size shouldBe 2

      inputEvents0(0).eventSource shouldBe "SearchInput"
      inputEvents0(0).eventType shouldBe SmuiEventType.CREATED.id
      (inputEvents0(0).eventTime.compareTo(tStart) >= 0) shouldBe true
      inputEvents0(0).userInfo shouldBe None
      inputEvents0(0).inputId shouldBe inputIds(0).id

      // whole payload should match
      val readSearchInput0 = Json.parse(inputEvents0(1).jsonPayload.get).validate[FullSearchInputWithRules].asOpt.get
      readSearchInput0.term shouldBe "aerosmith"
      readSearchInput0.isActive shouldBe true
      // ... incl a solrIndexId
      readSearchInput0.solrIndexId shouldBe core1Id
    }
  }

  "CREATED events" should "be present for spellings" in {
    db.withConnection { implicit conn =>

      // spelling#0 (freezer)

      val spellingEvents0 = InputEvent.loadForId(spellingIds(0).id)
      spellingEvents0.size shouldBe 2

      spellingEvents0(0).eventSource shouldBe "CanonicalSpelling"
      spellingEvents0(0).eventType shouldBe SmuiEventType.CREATED.id
      (spellingEvents0(0).eventTime.compareTo(tStart) >= 0) shouldBe true
      spellingEvents0(0).userInfo shouldBe None
      spellingEvents0(0).inputId shouldBe spellingIds(0).id

      // whole payload should match
      val readSpelling0 = Json.parse(spellingEvents0(1).jsonPayload.get).validate[FullCanonicalSpellingWithAlternatives].asOpt.get
      readSpelling0.term shouldBe "freezer"
      readSpelling0.isActive shouldBe true
      // ... incl a solrIndexId
      readSpelling0.solrIndexId shouldBe core1Id
    }
  }

  "ActivityLog for search input" should "have initial \"created\" event (in correct order)" in {
    db.withConnection { implicit conn =>

      // input#0 (aerosmith)

      val inputLog0 = ActivityLog.loadForId(inputIds(0).id)
      inputLog0.items.size shouldBe 2

      // TODO test inputLog0.items(1).formattedDateTime
      inputLog0.items(1).userInfo shouldBe None
      inputLog0.items(1).diffSummary.size shouldBe 1

      inputLog0.items(1).diffSummary(0).entity shouldBe "INPUT"
      inputLog0.items(1).diffSummary(0).eventType shouldBe "created"
      inputLog0.items(1).diffSummary(0).before shouldBe None
      inputLog0.items(1).diffSummary(0).after shouldBe Some("aerosmith (activated)")
    }
  }

  "ActivityLog for spellings" should "have initial \"created\" event (in correct order)" in {
    db.withConnection { implicit conn =>

      // spelling#0 (freezer)

      val spellingLog0 = ActivityLog.loadForId(spellingIds(0).id)
      spellingLog0.items.size shouldBe 2

      // TODO test inputLog0.items(1).formattedDateTime
      spellingLog0.items(1).userInfo shouldBe None
      spellingLog0.items(1).diffSummary.size shouldBe 1

      spellingLog0.items(1).diffSummary(0).entity shouldBe "SPELLING"
      spellingLog0.items(1).diffSummary(0).eventType shouldBe "created"
      spellingLog0.items(1).diffSummary(0).before shouldBe None
      spellingLog0.items(1).diffSummary(0).after shouldBe Some("freezer (activated)")
    }
  }

  "changes on search input" should "have resulted in UPDATED event (including all its associated rules as well)" in {
    db.withConnection { implicit conn =>

      // input#0 (aerosmith)

      val inputEvents0 = InputEvent.loadForId(inputIds(0).id)
      inputEvents0.size shouldBe 2

      inputEvents0(1).eventSource shouldBe "SearchInput"
      inputEvents0(1).eventType shouldBe SmuiEventType.UPDATED.id
      (inputEvents0(1).eventTime.compareTo(tStart) >= 0) shouldBe true
      inputEvents0(1).userInfo shouldBe None
      inputEvents0(1).inputId shouldBe inputIds(0).id

      // whole payload should match
      val readSearchInput0 = Json.parse(inputEvents0(1).jsonPayload.get).validate[SearchInputWithRules].asOpt.get
      readSearchInput0 shouldBe SearchInputWithRules.loadById(inputIds(0)).get
    }
  }

  "changes on spelling" should "have resulted in UPDATED event (including all its associated misspellings as well)" in {
    db.withConnection { implicit conn =>

      // spelling#0 (freezer)

      val spellingEvents0 = InputEvent.loadForId(spellingIds(0).id)
      spellingEvents0.size shouldBe 2

      spellingEvents0(1).eventSource shouldBe "CanonicalSpelling"
      spellingEvents0(1).eventType shouldBe SmuiEventType.UPDATED.id
      (spellingEvents0(1).eventTime.compareTo(tStart) >= 0) shouldBe true
      spellingEvents0(1).userInfo shouldBe None
      spellingEvents0(1).inputId shouldBe spellingIds(0).id

      // whole payload should match
      val readSpelling0 = Json.parse(spellingEvents0(1).jsonPayload.get).validate[CanonicalSpellingWithAlternatives].asOpt.get
      readSpelling0 shouldBe CanonicalSpellingWithAlternatives.loadById(spellingIds(0)).get
    }
  }

  "ActivityLog for search input" should "has nested rule creation on top" in {
    db.withConnection { implicit conn =>

      // input#0 (aerosmith)

      val inputLog0 = ActivityLog.loadForId(inputIds(0).id)
      inputLog0.items.size shouldBe 2

      // TODO test inputLog0.items(0).formattedDateTime
      inputLog0.items(0).userInfo shouldBe None
      inputLog0.items(0).diffSummary.size shouldBe 7

      inputLog0.items(0).diffSummary(0).entity shouldBe "RULE"
      inputLog0.items(0).diffSummary(0).eventType shouldBe "created"
      inputLog0.items(0).diffSummary(0).before shouldBe None
      inputLog0.items(0).diffSummary(0).after shouldBe Some("directed (activated)")

      inputLog0.items(0).diffSummary(1).entity shouldBe "RULE"
      inputLog0.items(0).diffSummary(1).eventType shouldBe "created"
      inputLog0.items(0).diffSummary(1).before shouldBe None
      inputLog0.items(0).diffSummary(1).after shouldBe Some("inactive (deactivated)")

      inputLog0.items(0).diffSummary(2).entity shouldBe "RULE"
      inputLog0.items(0).diffSummary(2).eventType shouldBe "created"
      inputLog0.items(0).diffSummary(2).before shouldBe None
      inputLog0.items(0).diffSummary(2).after shouldBe Some("mercury (activated)")

      inputLog0.items(0).diffSummary(3).entity shouldBe "RULE"
      inputLog0.items(0).diffSummary(3).eventType shouldBe "created"
      inputLog0.items(0).diffSummary(3).before shouldBe None
      inputLog0.items(0).diffSummary(3).after shouldBe Some("battery (activated)")

      inputLog0.items(0).diffSummary(4).entity shouldBe "RULE"
      inputLog0.items(0).diffSummary(4).eventType shouldBe "created"
      inputLog0.items(0).diffSummary(4).before shouldBe None
      inputLog0.items(0).diffSummary(4).after shouldBe Some("lenovo (deactivated)")

      inputLog0.items(0).diffSummary(5).entity shouldBe "RULE"
      inputLog0.items(0).diffSummary(5).eventType shouldBe "created"
      inputLog0.items(0).diffSummary(5).before shouldBe None
      inputLog0.items(0).diffSummary(5).after shouldBe Some("notebook (activated)")

      /*
      inputLog0.items(0).diffSummary(6).entity shouldBe "RULE"
      inputLog0.items(0).diffSummary(6).eventType shouldBe "created"
      inputLog0.items(0).diffSummary(6).before shouldBe None
      inputLog0.items(0).diffSummary(6).after shouldBe Some("freddy (activated)")
      */

      inputLog0.items(0).diffSummary(6).entity shouldBe "RULE"
      inputLog0.items(0).diffSummary(6).eventType shouldBe "created"
      inputLog0.items(0).diffSummary(6).before shouldBe None
      inputLog0.items(0).diffSummary(6).after shouldBe Some("zz top (activated)")
    }
  }

  "ActivityLog for spelling" should "has nested misspelling creation on top" in {
    db.withConnection { implicit conn =>

      // spelling#0 (freezer)

      val spellingLog0 = ActivityLog.loadForId(spellingIds(0).id)
      spellingLog0.items.size shouldBe 2

      // TODO test spellingLog0.items(0).formattedDateTime
      spellingLog0.items(0).userInfo shouldBe None
      spellingLog0.items(0).diffSummary.size shouldBe 3

      spellingLog0.items(0).diffSummary(0).entity shouldBe "MISSPELLING"
      spellingLog0.items(0).diffSummary(0).eventType shouldBe "created"
      spellingLog0.items(0).diffSummary(0).before shouldBe None
      spellingLog0.items(0).diffSummary(0).after shouldBe Some("frazer (activated)")

      spellingLog0.items(0).diffSummary(1).entity shouldBe "MISSPELLING"
      spellingLog0.items(0).diffSummary(1).eventType shouldBe "created"
      spellingLog0.items(0).diffSummary(1).before shouldBe None
      spellingLog0.items(0).diffSummary(1).after shouldBe Some("freazer (activated)")

      spellingLog0.items(0).diffSummary(2).entity shouldBe "MISSPELLING"
      spellingLog0.items(0).diffSummary(2).eventType shouldBe "created"
      spellingLog0.items(0).diffSummary(2).before shouldBe None
      spellingLog0.items(0).diffSummary(2).after shouldBe Some("frezer (activated)")
    }
  }

  "empty changes on search input & spelling" should "result in UPDATED event, but no additional ActivityLog entry" in {
    db.withConnection { implicit conn =>

      // input#0 (aerosmith)

      val searchInput0 = SearchInputWithRules.loadById(inputIds(0)).get
      repo.updateSearchInput(searchInput0)
      val inputEvents0 = InputEvent.loadForId(inputIds(0).id)
      inputEvents0.size shouldBe 3
      inputEvents0(2).eventType shouldBe SmuiEventType.UPDATED.id
      val inputLog0 = ActivityLog.loadForId(inputIds(0).id)
      inputLog0.items.size shouldBe 2

      // spelling#0 (freezer)

      val spelling0 = CanonicalSpellingWithAlternatives.loadById(spellingIds(0)).get
      repo.updateSpelling(spelling0)
      val spellingEvents0 = InputEvent.loadForId(spellingIds(0).id)
      spellingEvents0.size shouldBe 3
      spellingEvents0(2).eventType shouldBe SmuiEventType.UPDATED.id
      val spellingLog0 = ActivityLog.loadForId(spellingIds(0).id)
      spellingLog0.items.size shouldBe 2
    }
  }

  "deletion & updates of nested search input rules" should "result in correct ActivityLog entries" in {
    db.withConnection { implicit conn =>

      // input#0 (aerosmith)

      val searchInput0 = SearchInputWithRules.loadById(inputIds(0)).get
      val newSearchInput0 = searchInput0.copy(
        synonymRules = searchInput0.synonymRules
          .filter(r => !(r.term.equals("mercury")))
          .map(r => {
            if (r.term == "directed") {
              r.copy(
                synonymType = 0,
                term = "undirected",
                isActive = false
              )
            } else {
              r
            }
          })
      )
      repo.updateSearchInput(newSearchInput0)

      val inputEvents0 = InputEvent.loadForId(inputIds(0).id)
      inputEvents0.size shouldBe 4
      inputEvents0(3).eventType shouldBe SmuiEventType.UPDATED.id

      val inputLog0 = ActivityLog.loadForId(inputIds(0).id)
      inputLog0.items.size shouldBe 3

      // TODO test inputLog0.items(2).formattedDateTime
      inputLog0.items(0).userInfo shouldBe None
      inputLog0.items(0).diffSummary.size shouldBe 2

      inputLog0.items(0).diffSummary(0).entity shouldBe "RULE"
      inputLog0.items(0).diffSummary(0).eventType shouldBe "deleted"
      inputLog0.items(0).diffSummary(0).before shouldBe Some("mercury (activated)")
      inputLog0.items(0).diffSummary(0).after shouldBe None

      inputLog0.items(0).diffSummary(1).entity shouldBe "RULE"
      inputLog0.items(0).diffSummary(1).eventType shouldBe "updated"
      inputLog0.items(0).diffSummary(1).before shouldBe Some("directed (activated)")
      inputLog0.items(0).diffSummary(1).after shouldBe Some("undirected (deactivated)")

    }
  }

  "deletion & updates of nested misspellings" should "result in correct ActivityLog entries" in {
    db.withConnection { implicit conn =>

      // spelling#0 (freezer)

      val spelling0 = CanonicalSpellingWithAlternatives.loadById(spellingIds(0)).get
      val newSpelling0 = spelling0.copy(
        alternativeSpellings = spelling0.alternativeSpellings
          .filter(s => !(s.term.equals("frazer")))
          .map(s => {
            if (s.term.equals("frezer")) {
              AlternativeSpelling(s.id, spelling0.id, "freeeeeezy", false)
            } else {
              s
            }
          })
      )
      repo.updateSpelling(newSpelling0)

      val spellingEvents0 = InputEvent.loadForId(spellingIds(0).id)
      spellingEvents0.size shouldBe 4
      spellingEvents0(3).eventType shouldBe SmuiEventType.UPDATED.id

      val spellingLog0 = ActivityLog.loadForId(spellingIds(0).id)
      spellingLog0.items.size shouldBe 3

      // TODO test spellingLog0.items(0).formattedDateTime
      spellingLog0.items(0).userInfo shouldBe None
      spellingLog0.items(0).diffSummary.size shouldBe 2

      spellingLog0.items(0).diffSummary(0).entity shouldBe "MISSPELLING"
      spellingLog0.items(0).diffSummary(0).eventType shouldBe "deleted"
      spellingLog0.items(0).diffSummary(0).before shouldBe Some("frazer (activated)")
      spellingLog0.items(0).diffSummary(0).after shouldBe None

      spellingLog0.items(0).diffSummary(1).entity shouldBe "MISSPELLING"
      spellingLog0.items(0).diffSummary(1).eventType shouldBe "updated"
      spellingLog0.items(0).diffSummary(1).before shouldBe Some("frezer (activated)")
      spellingLog0.items(0).diffSummary(1).after shouldBe Some("freeeeeezy (deactivated)")
    }
  }

  "comment updates on search input" should "result in a corresponding \"updated\" ActivityLog entries" in {
    db.withConnection { implicit conn =>

      // input#0 (aerosmith)

      val NEW_COMMENT = "I prefer Rap music. Will change the rule to a rule for the Wu-Tang Clan soon ..."

      val searchInput0 = SearchInputWithRules.loadById(inputIds(0)).get
      val newSearchInput0 = searchInput0.copy(
        comment = NEW_COMMENT
      )
      repo.updateSearchInput(newSearchInput0)

      val inputEvents0 = InputEvent.loadForId(inputIds(0).id)
      inputEvents0.size shouldBe 5
      inputEvents0(4).eventType shouldBe SmuiEventType.UPDATED.id

      val inputLog0 = ActivityLog.loadForId(inputIds(0).id)
      inputLog0.items.size shouldBe 4

      // TODO test inputLog0.items(0).formattedDateTime
      inputLog0.items(0).userInfo shouldBe None
      inputLog0.items(0).diffSummary.size shouldBe 1

      inputLog0.items(0).diffSummary(0).entity shouldBe "COMMENT"
      inputLog0.items(0).diffSummary(0).eventType shouldBe "updated"
      inputLog0.items(0).diffSummary(0).before shouldBe Some("") // TODO return None here seems semantically better ==> adjust @see models/eventhistory/ActivityLog.scala
      inputLog0.items(0).diffSummary(0).after shouldBe Some(NEW_COMMENT)

    }
  }

  "comment updates on spelling" should "result in a corresponding \"updated\" ActivityLog entries" in {
    db.withConnection { implicit conn =>

      // spelling#0 (freezer)

      val NEW_COMMENT = "Fo' shizzle my nizzle like the salt on the freezle???"

      val spelling0 = CanonicalSpellingWithAlternatives.loadById(spellingIds(0)).get
      val newSpelling0 = spelling0.copy(
        comment = NEW_COMMENT
      )
      repo.updateSpelling(newSpelling0)

      val spellingEvents0 = InputEvent.loadForId(spellingIds(0).id)
      spellingEvents0.size shouldBe 5
      spellingEvents0(4).eventType shouldBe SmuiEventType.UPDATED.id

      val spellingLog0 = ActivityLog.loadForId(spellingIds(0).id)
      spellingLog0.items.size shouldBe 4

      // TODO test spellingLog0.items(0).formattedDateTime
      spellingLog0.items(0).userInfo shouldBe None
      spellingLog0.items(0).diffSummary.size shouldBe 1

      spellingLog0.items(0).diffSummary(0).entity shouldBe "COMMENT"
      spellingLog0.items(0).diffSummary(0).eventType shouldBe "updated"
      spellingLog0.items(0).diffSummary(0).before shouldBe Some("") // TODO return None here seems semantically better ==> adjust @see models/eventhistory/ActivityLog.scala
      spellingLog0.items(0).diffSummary(0).after shouldBe Some(NEW_COMMENT)

    }
  }


  "deletion of search input" should "result in a DELETED event" in {
    db.withConnection { implicit conn =>

      repo.deleteSearchInput(inputIds(0).id)

      val inputEvents0 = InputEvent.loadForId(inputIds(0).id)
      inputEvents0.size shouldBe 6

      inputEvents0(5).eventSource shouldBe "SearchInput"
      inputEvents0(5).eventType shouldBe SmuiEventType.DELETED.id
      (inputEvents0(5).eventTime.compareTo(tStart) >= 0) shouldBe true
      inputEvents0(5).userInfo shouldBe None
      inputEvents0(5).inputId shouldBe inputIds(0).id
      inputEvents0(5).jsonPayload shouldBe None
    }
  }

  "deletion of spelling" should "result in a DELETED event" in {
    db.withConnection { implicit conn =>

      repo.deleteSpelling(spellingIds(0).id)

      val spellingEvents0 = InputEvent.loadForId(spellingIds(0).id)
      spellingEvents0.size shouldBe 6

      spellingEvents0(5).eventSource shouldBe "CanonicalSpelling"
      spellingEvents0(5).eventType shouldBe SmuiEventType.DELETED.id
      (spellingEvents0(5).eventTime.compareTo(tStart) >= 0) shouldBe true
      spellingEvents0(5).userInfo shouldBe None
      spellingEvents0(5).inputId shouldBe spellingIds(0).id
      spellingEvents0(5).jsonPayload shouldBe None
    }
  }

  /**
    * Test Activity Report for all the changes done above (especially for DELETED events).
    */

  "ActivityReport" should "inform about all changes for search input & spellings (incl deletion)" in {
    db.withConnection { implicit conn =>

      val tEnd = LocalDateTime.now()

      val activityReport = ActivityLog.reportForSolrIndexIdInPeriod(core1Id, tStart, tEnd)

      println(s"activityReport = >>>$activityReport")

      activityReport.items.size shouldBe 10



    }
  }

  // TODO add test for ActivityReport incl a deployment to PRELIVE/LIVE

}
