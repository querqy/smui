package models.eventhistory

import java.time.LocalDateTime

import models.ApplicationTestBase
import models.input.SearchInputId
import models.spellings.CanonicalSpellingId
import org.scalatest.{FlatSpec, Matchers}

class DeactivatedEventHistorySpec extends FlatSpec with Matchers with ApplicationTestBase {

  private var inputIds: Seq[SearchInputId] = Seq.empty
  private var spellingIds: Seq[CanonicalSpellingId] = Seq.empty

  override protected lazy val additionalAppConfig = Seq(
    "toggle.activate-eventhistory" -> false
  )

  override protected def beforeAll(): Unit = {
    super.beforeAll()

    createTestCores()
    inputIds = createTestRule()
    spellingIds = createTestSpellings()
  }

  "No CREATED events" should "be present for search inputs" in {
    db.withConnection { implicit conn =>

      // input#0 (aerosmith)

      val inputEvents0 = InputEvent.loadForId(inputIds(0).id)
      inputEvents0.size shouldBe 0

    }
  }

  "No CREATED events" should "be present for spellings" in {
    db.withConnection { implicit conn =>

      // spelling#0 (freezer)

      val spellingEvents0 = InputEvent.loadForId(spellingIds(0).id)
      spellingEvents0.size shouldBe 0
    }
  }

}
