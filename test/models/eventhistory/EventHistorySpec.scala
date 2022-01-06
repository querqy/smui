package models.eventhistory

import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.matchers.{BeMatcher, MatchResult}

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import play.api.libs.json.Json
import models.ApplicationTestBase
import models.input.{FullSearchInputWithRules, SearchInputId, SearchInputWithRules}
import models.reports.ActivityReport
import models.spellings.{AlternativeSpelling, CanonicalSpellingId, CanonicalSpellingWithAlternatives, FullCanonicalSpellingWithAlternatives}

trait CustomerMatchers {

  class DateEqualOrAfter(bTime: LocalDateTime) extends BeMatcher[LocalDateTime] {
    override def apply(aTime: LocalDateTime) =
      MatchResult(
        (aTime.isEqual(bTime)) || (bTime.isAfter(aTime)),
        s"$bTime is equal or after $aTime",
        s"$bTime is before $aTime - this is unexpected"
      )
  }

  def dateEqualOrAfter(bTime: LocalDateTime) = new DateEqualOrAfter(bTime)

}

class EventHistorySpec extends FlatSpec with Matchers with CustomerMatchers with ApplicationTestBase {

  private var inputIds: Seq[SearchInputId] = Seq.empty
  private var spellingIds: Seq[CanonicalSpellingId] = Seq.empty

  private var tStart: LocalDateTime = LocalDateTime.now()

  override protected lazy val additionalAppConfig = Seq(
      "toggle.activate-eventhistory" -> true
    )

  override protected val MILLIS_BETWEEN_CHANGE_EVENTS = 2000

  override protected def beforeAll(): Unit = {
    super.beforeAll()

    tStart = LocalDateTime.now()

    createTestCores()
    inputIds = createTestRule()
    spellingIds = createTestSpellings()
  }

  "CREATED events for createdTestRule" should "be present for search input InputEvent" in {
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

  "CREATED events for createTestSpellings" should "be present for spelling InputEvent" in {
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

      // NOTE: all created events for rules in (lowercase based) alphabetic order

      inputLog0.items(0).diffSummary(0).entity shouldBe "RULE"
      inputLog0.items(0).diffSummary(0).eventType shouldBe "created"
      inputLog0.items(0).diffSummary(0).before shouldBe None
      inputLog0.items(0).diffSummary(0).after shouldBe Some("battery (activated)")

      inputLog0.items(0).diffSummary(1).entity shouldBe "RULE"
      inputLog0.items(0).diffSummary(1).eventType shouldBe "created"
      inputLog0.items(0).diffSummary(1).before shouldBe None
      inputLog0.items(0).diffSummary(1).after shouldBe Some("directed (activated)")

      inputLog0.items(0).diffSummary(2).entity shouldBe "RULE"
      inputLog0.items(0).diffSummary(2).eventType shouldBe "created"
      inputLog0.items(0).diffSummary(2).before shouldBe None
      inputLog0.items(0).diffSummary(2).after shouldBe Some("inactive (deactivated)")

      inputLog0.items(0).diffSummary(3).entity shouldBe "RULE"
      inputLog0.items(0).diffSummary(3).eventType shouldBe "created"
      inputLog0.items(0).diffSummary(3).before shouldBe None
      inputLog0.items(0).diffSummary(3).after shouldBe Some("lenovo (deactivated)")

      inputLog0.items(0).diffSummary(4).entity shouldBe "RULE"
      inputLog0.items(0).diffSummary(4).eventType shouldBe "created"
      inputLog0.items(0).diffSummary(4).before shouldBe None
      inputLog0.items(0).diffSummary(4).after shouldBe Some("mercury (activated)")

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

      // NOTE: all created events for misspellings in (lowercase based) alphabetic order

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
      repo.updateSearchInput(searchInput0, None)
      Thread.sleep(MILLIS_BETWEEN_CHANGE_EVENTS)

      val inputEvents0 = InputEvent.loadForId(inputIds(0).id)
      inputEvents0.size shouldBe 3
      inputEvents0(2).eventType shouldBe SmuiEventType.UPDATED.id
      val inputLog0 = ActivityLog.loadForId(inputIds(0).id)
      inputLog0.items.size shouldBe 2

      // spelling#0 (freezer)

      val spelling0 = CanonicalSpellingWithAlternatives.loadById(spellingIds(0)).get
      repo.updateSpelling(spelling0, None)
      Thread.sleep(MILLIS_BETWEEN_CHANGE_EVENTS)

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
      repo.updateSearchInput(newSearchInput0, None)
      Thread.sleep(MILLIS_BETWEEN_CHANGE_EVENTS)

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
          // emulate deletion
          .filter(s => !(s.term.equals("frazer")))
          // emulation update
          .map(s => {
            if (s.term.equals("frezer")) {
              AlternativeSpelling(s.id, spelling0.id, "freeeeeezy", false)
            } else {
              s
            }
          })
      )
      repo.updateSpelling(newSpelling0, None)
      Thread.sleep(MILLIS_BETWEEN_CHANGE_EVENTS)

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

      val NEW_INPUT0_COMMENT = "I prefer Rap music. Will change the rule to a rule for the Wu-Tang Clan soon ..."

      val searchInput0 = SearchInputWithRules.loadById(inputIds(0)).get
      val newSearchInput0 = searchInput0.copy(
        comment = NEW_INPUT0_COMMENT
      )
      repo.updateSearchInput(newSearchInput0, None)
      Thread.sleep(MILLIS_BETWEEN_CHANGE_EVENTS)

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
      inputLog0.items(0).diffSummary(0).after shouldBe Some(NEW_INPUT0_COMMENT)

    }
  }

  "comment updates on spelling" should "result in a corresponding \"updated\" ActivityLog entries" in {
    db.withConnection { implicit conn =>

      // spelling#0 (freezer)

      val NEW_SPELLING0_COMMENT = "Fo' shizzle my nizzle like the salt on the freezle???"

      val spelling0 = CanonicalSpellingWithAlternatives.loadById(spellingIds(0)).get
      val newSpelling0 = spelling0.copy(
        comment = NEW_SPELLING0_COMMENT
      )
      repo.updateSpelling(newSpelling0, None)
      Thread.sleep(MILLIS_BETWEEN_CHANGE_EVENTS)

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
      spellingLog0.items(0).diffSummary(0).after shouldBe Some(NEW_SPELLING0_COMMENT)

    }
  }

  "deletion of search input" should "result in a DELETED event" in {
    db.withConnection { implicit conn =>

      repo.deleteSearchInput(inputIds(0).id, None)
      Thread.sleep(MILLIS_BETWEEN_CHANGE_EVENTS)

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

      repo.deleteSpelling(spellingIds(0).id, None)
      Thread.sleep(MILLIS_BETWEEN_CHANGE_EVENTS)

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

      val activityReport = ActivityReport.reportForSolrIndexIdInPeriod(core1Id, tStart, tEnd)

      activityReport.items.size shouldBe 11

      // check, that report is sorted by modificationTime

      val items = activityReport.items

      (items zip items.tail)
        .map(itemPair => {
          val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
          val aTime = LocalDateTime.parse(itemPair._1.modificationTime, formatter)
          val bTime = LocalDateTime.parse(itemPair._2.modificationTime, formatter)

          bTime shouldBe dateEqualOrAfter(aTime)
        })

      // TODO impractical, but theoretically possible: test only creation of search input & spellings

      // check Activity Report content of past activities in Spec

      items(0).inputTerm shouldBe "freezer"
      items(0).entity shouldBe "SPELLING"
      items(0).eventType shouldBe "deleted"
      items(0).before shouldBe Some("freezer (activated)")
      items(0).after shouldBe None

      items(1).inputTerm shouldBe "aerosmith"
      items(1).entity shouldBe "INPUT"
      items(1).eventType shouldBe "deleted"
      items(1).before shouldBe Some("aerosmith (activated)")
      items(1).after shouldBe None

      // NOTE: No further RULE, MISSPELLING and/or COMMENT updates or deleted expected, as whole search & canonical spelling input got deleted in period

      items(2).inputTerm shouldBe "pants"
      items(2).entity shouldBe "INPUT"
      items(2).eventType shouldBe "updated"
      items(2).before shouldBe Some("pants (activated)")
      items(2).after shouldBe Some("pants (deactivated)")

      items(3).inputTerm shouldBe "pants"
      items(3).entity shouldBe "MISSPELLING"
      items(3).eventType shouldBe "created"
      items(3).before shouldBe None
      items(3).after shouldBe Some("pands (activated)")

      items(4).inputTerm shouldBe "pants"
      items(4).entity shouldBe "MISSPELLING"
      items(4).eventType shouldBe "created"
      items(4).before shouldBe None
      items(4).after shouldBe Some("pents (activated)")

      items(5).inputTerm shouldBe "pants"
      items(5).entity shouldBe "COMMENT"
      items(5).eventType shouldBe "updated"
      items(5).before shouldBe Some("") // TODO see above (maybe require None?)
      items(5).after shouldBe Some("This is a comment")

      items(6).inputTerm shouldBe "machine"
      items(6).entity shouldBe "MISSPELLING"
      items(6).eventType shouldBe "created"
      items(6).before shouldBe None
      items(6).after shouldBe Some("machin (deactivated)")

      items(7).inputTerm shouldBe "machine"
      items(7).entity shouldBe "MISSPELLING"
      items(7).eventType shouldBe "created"
      items(7).before shouldBe None
      items(7).after shouldBe Some("mechine (activated)")

      items(8).inputTerm shouldBe "inactive"
      items(8).entity shouldBe "INPUT"
      items(8).eventType shouldBe "updated"
      items(8).before shouldBe Some("inactive (activated)")
      items(8).after shouldBe Some("inactive (deactivated)")

      items(9).inputTerm shouldBe "inactive"
      items(9).entity shouldBe "COMMENT"
      items(9).eventType shouldBe "updated"
      items(9).before shouldBe Some("") // TODO maybe require None here?
      items(9).after shouldBe Some("inactive")

      items(10).inputTerm shouldBe "shipping"
      items(10).entity shouldBe "RULE"
      items(10).eventType shouldBe "created"
      items(10).before shouldBe None
      items(10).after shouldBe Some("URL: http://xyz.com/shipping (activated)")

    }
  }

}
