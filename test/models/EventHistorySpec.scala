package models

import java.time.LocalDateTime

import org.scalatest.{FlatSpec, Matchers}

import models.rules.SynonymRule
import models.input.{SearchInputId, SearchInputWithRules}
import models.eventhistory._

class EventHistorySpec extends FlatSpec with Matchers with ApplicationTestBase {

  private var inputIds: Seq[SearchInputId] = Seq.empty

  private var tStart: LocalDateTime = LocalDateTime.now()

  override protected def beforeAll(): Unit = {
    super.beforeAll()

    tStart = LocalDateTime.now()

    createTestCores()
    inputIds = createTestRule()
  }

  "eventhistory" should "contain a CREATED last event for the inputs" in {
    db.withConnection { implicit conn =>

      // test search input

      val listInputEvents0 = InputEvent.loadForSearchInput(inputIds(0))
      listInputEvents0.size shouldBe 2

      // input#0 (aerosmith)

      listInputEvents0(1).eventType shouldBe EventHistoryType.CREATED.id
      listInputEvents0(1).eventTime.isAfter(tStart) shouldBe true
      listInputEvents0(1).userInfo shouldBe None
      listInputEvents0(1).term shouldBe Some("aerosmith")
      listInputEvents0(1).status shouldBe Some(0x01)
      listInputEvents0(1).comment shouldBe Some("")
      listInputEvents0(1).tagPayload shouldBe None

      // no events for any rules of input#0

      val listRuleEvents0 = RuleEvent.loadForSearchInputEvent(listInputEvents0(1).id)
      listRuleEvents0.size shouldBe 0

      // input#1 (shipping)

      val listInputEvents1 = InputEvent.loadForSearchInput(inputIds(1))
      listInputEvents1.size shouldBe 2
      listInputEvents1(1).eventType shouldBe EventHistoryType.CREATED.id
      listInputEvents1(1).term shouldBe Some("shipping")
    }
  }

  "eventhistory" should "contain an UPDATED first event for the inputs and CREATED events for the test rules" in {
    db.withConnection { implicit conn =>

      // test search input

      val listInputEvents0 = InputEvent.loadForSearchInput(inputIds(0))
      listInputEvents0.size shouldBe 2

      // input#0 (aerosmith)

      listInputEvents0(0).eventType shouldBe EventHistoryType.UPDATED.id
      listInputEvents0(0).eventTime.isAfter(tStart) shouldBe true
      listInputEvents0(0).userInfo shouldBe None
      listInputEvents0(0).term shouldBe None
      listInputEvents0(0).status shouldBe None
      listInputEvents0(0).comment shouldBe None
      listInputEvents0(0).tagPayload shouldBe None

      // test rules belonging to input#0

      val listRuleEvents0 = RuleEvent.loadForSearchInputEvent(listInputEvents0(0).id)
      listRuleEvents0.size shouldBe 5

      // TODO order of events is flaky (due to nanosecond timing of database insert timestamp)

      listRuleEvents0(4).eventType shouldBe EventHistoryType.CREATED.id
      listRuleEvents0(4).eventTime.isAfter(tStart) shouldBe true
      listRuleEvents0(4).term shouldBe Some("mercury")
      listRuleEvents0(4).status shouldBe Some(0x01)
      listRuleEvents0(4).ruleType shouldBe "SYNONYM"
      listRuleEvents0(4).prmPayload shouldBe None

      listRuleEvents0(3).eventType shouldBe EventHistoryType.CREATED.id
      listRuleEvents0(3).term shouldBe Some("notebook")
      listRuleEvents0(3).ruleType shouldBe "UP_DOWN"
      // ...
      listRuleEvents0(0).eventType shouldBe EventHistoryType.CREATED.id
      listRuleEvents0(0).term shouldBe Some("zz top")
      listRuleEvents0(0).ruleType shouldBe "FILTER"

      // input#1 (shipping)

      val listInputEvents1 = InputEvent.loadForSearchInput(inputIds(1))
      listInputEvents1.size shouldBe 2
      listInputEvents1(0).eventType shouldBe EventHistoryType.UPDATED.id
      listInputEvents1(0).term shouldBe None

      // test rules belonging to input#1

      val listRuleEvents1 = RuleEvent.loadForSearchInputEvent(listInputEvents1(0).id)
      listRuleEvents1.size shouldBe 1

      listRuleEvents1(0).eventType shouldBe EventHistoryType.CREATED.id
      listRuleEvents1(0).term shouldBe Some("http://xyz.com/shipping")
      listRuleEvents1(0).ruleType shouldBe "REDIRECT"
    }
  }

  "eventhistory" should "contain an UPDATED last event (with correct content) after modifying the input" in {
    db.withConnection { implicit conn =>

      // input#0 (aerosmith)

      // change input#0

      val input0 = SearchInputWithRules.loadById(inputIds(0)).get
      val newInput0 = SearchInputWithRules(
        input0.id,
        "wu tang clan",
        input0.synonymRules,
        input0.upDownRules,
        input0.filterRules,
        input0.deleteRules,
        input0.redirectRules,
        input0.tags,
        false,
        "I prefer Rap music. Will activate the rule once it feels good ..."
      )
      repo.updateSearchInput(newInput0)

      val listInputEvents0_0 = InputEvent.loadForSearchInput(input0.id)
      listInputEvents0_0.size shouldBe 3

      listInputEvents0_0(0).eventType shouldBe EventHistoryType.UPDATED.id
      listInputEvents0_0(0).eventTime.isAfter(tStart) shouldBe true
      listInputEvents0_0(0).userInfo shouldBe None
      listInputEvents0_0(0).term shouldBe Some("aerosmith")
      listInputEvents0_0(0).status shouldBe Some(0x01)
      listInputEvents0_0(0).comment shouldBe Some("")
      listInputEvents0_0(0).tagPayload shouldBe None

      // assume 5 last empty rule events

      def assertOnlyEmptyRuleEvents(eventId: InputEventId, howMany: Int) = {
        val listRuleEvents = RuleEvent.loadForSearchInputEvent(eventId)
        for (i <- (listRuleEvents.size - howMany) to (listRuleEvents.size - 1)) {
          listRuleEvents(i).eventType shouldBe EventHistoryType.UPDATED.id
          listRuleEvents(i).term shouldBe None
          listRuleEvents(i).status shouldBe None
        }
      }

      assertOnlyEmptyRuleEvents(listInputEvents0_0(0).id, 5)

      // change input#0 back

      repo.updateSearchInput(input0)

      val listInputEvents0_1 = InputEvent.loadForSearchInput(input0.id)
      listInputEvents0_1.size shouldBe 4

      listInputEvents0_1(0).eventType shouldBe EventHistoryType.UPDATED.id
      listInputEvents0_1(0).eventTime.isAfter(tStart) shouldBe true
      listInputEvents0_1(0).userInfo shouldBe None
      listInputEvents0_1(0).term shouldBe Some("wu tang clan")
      listInputEvents0_1(0).status shouldBe Some(0x00)
      listInputEvents0_1(0).comment shouldBe Some("I prefer Rap music. Will activate the rule once it feels good ...")
      listInputEvents0_1(0).tagPayload shouldBe None

      assertOnlyEmptyRuleEvents(listInputEvents0_1(0).id, 5)
    }
  }

  "eventhistory" should "contain CREATED, UPDATED and DELETED events for modified rules for an input" in {
    db.withConnection { implicit conn =>

      // input#0 (aerosmith)

      // change rules of input#0

      val input0 = SearchInputWithRules.loadById(inputIds(0)).get

      val newSynonymRules = SynonymRule(synonymType = 0, term = "good music", isActive = true) :: input0.synonymRules
      val newUpDownRules = input0.upDownRules.filter(r => !(r.id.equals(input0.upDownRules(0).id)))

      val newInput0 = SearchInputWithRules(
        input0.id,
        input0.term,
        newSynonymRules,
        newUpDownRules,
        input0.filterRules,
        input0.deleteRules,
        input0.redirectRules,
        input0.tags,
        input0.isActive,
        input0.comment
      )
      repo.updateSearchInput(newInput0)

      val listInputEvents0 = InputEvent.loadForSearchInput(input0.id)
      listInputEvents0.size shouldBe 5

      listInputEvents0(0).eventType shouldBe EventHistoryType.UPDATED.id

      val listRuleEvents0 = RuleEvent.loadForSearchInputEvent(listInputEvents0(0).id)
      listRuleEvents0.size shouldBe 6

      val eventsUpdatedEmpty = listRuleEvents0.filter(e => e.eventType.equals(EventHistoryType.UPDATED.id) && (e.term.isEmpty))
      eventsUpdatedEmpty.size shouldBe 4
      eventsUpdatedEmpty.filter(e => e.ruleType.equals("SYNONYM")).size shouldBe 1
      eventsUpdatedEmpty.filter(e => e.ruleType.equals("UP_DOWN")).size shouldBe 2
      eventsUpdatedEmpty.filter(e => e.ruleType.equals("FILTER")).size shouldBe 1

      val eventsCreated = listRuleEvents0.filter(e => e.eventType.equals(EventHistoryType.CREATED.id))
      eventsCreated.size shouldBe 1
      val eventCreated = eventsCreated.head
      eventCreated.term shouldBe Some("good music")
      eventCreated.status shouldBe Some(0x01)
      eventCreated.ruleType shouldBe "SYNONYM"

      val eventsDeleted = listRuleEvents0.filter(e => e.eventType.equals(EventHistoryType.DELETED.id))
      eventsDeleted.size shouldBe 1
      val eventDeleted = eventsDeleted.head
      eventDeleted.term shouldBe Some("battery")
      eventDeleted.status shouldBe Some(0x01)
      eventDeleted.ruleType shouldBe "UP_DOWN"
    }
  }

  "eventhistory" should "contain DELETED events, in case an input was deleted" in {
    db.withConnection { implicit conn =>

      repo.deleteSearchInput(inputIds(0).id)

      val listInputEvents0 = InputEvent.loadForSearchInput(inputIds(0))
      listInputEvents0.size shouldBe 6

      listInputEvents0(0).eventType shouldBe EventHistoryType.DELETED.id
      listInputEvents0(0).term shouldBe Some("aerosmith")
      listInputEvents0(0).status shouldBe Some(0x01)
      listInputEvents0(0).comment shouldBe Some("")

      val listRuleEvents0 = RuleEvent.loadForSearchInputEvent(listInputEvents0(0).id)
      listRuleEvents0.size shouldBe 5

      for (i <- 0 to 4) {
        listRuleEvents0(i).eventType shouldBe EventHistoryType.DELETED.id
        listRuleEvents0(i).term should not be None
        listRuleEvents0(i).status should not be None
      }
    }
  }

}
