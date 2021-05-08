package services

import models.ApplicationTestBase
import models.input.SearchInputWithRules
import org.scalatest.{FlatSpec, Matchers}

class RulesTxtImportServiceSpec extends FlatSpec with Matchers with ApplicationTestBase {

  private lazy val service = injector.instanceOf[RulesTxtImportService]

  override protected lazy val additionalAppConfig = Seq(
    "toggle.rule-tagging" -> false
  )


  override protected def beforeAll(): Unit = {
    super.beforeAll()
    createTestCores()
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    deleteAllSearchInputsFromDb(core1Id)
  }

  def loadSearchInputWithRules(searchInputTerm: String, searchInputs: List[SearchInputWithRules]): SearchInputWithRules = {
    for(searchInput <- searchInputs) {
      val searchInputWithRules = repo.getDetailedSearchInput(searchInput.id)
      if (searchInputWithRules.get.term == searchInputTerm) {
        return searchInputWithRules.get
      }
    }
    return null
  }


  "RulesTxtImportService" should "import all rule types from a valid rules.txt" in {
    var rules: String = s"""input_1 =>
                           |	SYNONYM: synomyn_1
                           |	DOWN(10): down_1
                           |	UP(10): up_1
                           |	FILTER: filter_1
                           |	DELETE: input_1
                           |
                           |input_2 =>
                           |	SYNONYM: synomyn_2
                           |	DOWN(10): down_2
                           |	UP(10): up_2
                           |	FILTER: filter_2
                           |	DELETE: input_2""".stripMargin

    val (
      retstatCountRulesTxtInputs,
      retstatCountRulesTxtLinesSkipped,
      retstatCountRulesTxtUnkownConvert,
      retstatCountConsolidatedInputs,
      retstatCountConsolidatedRules
      ) = service.importFromFilePayload(rules, core1Id, repo)

    val searchInputWithRules = repo.listAllSearchInputsInclDirectedSynonyms(core1Id)
    searchInputWithRules.size shouldBe 2
    retstatCountRulesTxtInputs shouldBe 2
    retstatCountRulesTxtLinesSkipped shouldBe 1
    retstatCountRulesTxtUnkownConvert shouldBe 0
    retstatCountConsolidatedInputs shouldBe 2
    retstatCountConsolidatedRules shouldBe 10

    val searchInput_1 = loadSearchInputWithRules("input_1", searchInputWithRules)
    searchInput_1.synonymRules.size shouldBe 1
    searchInput_1.upDownRules.size shouldBe 2
    searchInput_1.deleteRules.size shouldBe 1
    searchInput_1.filterRules.size shouldBe 1
    searchInput_1.redirectRules.size shouldBe 0
    searchInput_1.tags.size shouldBe 0
    searchInput_1.isActive shouldBe true
    searchInput_1.comment shouldBe "Added by rules.txt import."

    val searchInput_2 = loadSearchInputWithRules("input_2", searchInputWithRules)
    searchInput_2.synonymRules.size shouldBe 1
    searchInput_2.upDownRules.size shouldBe 2
    searchInput_2.deleteRules.size shouldBe 1
    searchInput_2.filterRules.size shouldBe 1
    searchInput_2.redirectRules.size shouldBe 0
    searchInput_2.tags.size shouldBe 0
    searchInput_2.isActive shouldBe true
    searchInput_2.comment shouldBe "Added by rules.txt import."
  }

  it should "fail for invalid syntax" in {
    var rules: String = s"""input_1 =
                           |	SYNONYM: synomyn_1""".stripMargin
    an [IllegalArgumentException] should be thrownBy service.importFromFilePayload(rules, core1Id, repo)

    rules = s"""input_1 => SYNONYM: synomyn_1""".stripMargin
    an [IllegalArgumentException] should be thrownBy service.importFromFilePayload(rules, core1Id, repo)

    rules = s"""input_1 =>
               |	xzy: synomyn_1""".stripMargin
    an [IllegalArgumentException] should be thrownBy service.importFromFilePayload(rules, core1Id, repo)

    rules = s"""input_1 =>
               |	SYNONYM: synomyn_1
               |  @{
               |    "missing_@" : "value"
               |  }""".stripMargin
    an [IllegalArgumentException] should be thrownBy service.importFromFilePayload(rules, core1Id, repo)

    rules = s"""input_1 =>
               |	SYNONYM: synomyn_1
               |  @{
               |    "name" : "value"
               |    "missing_comma" : "abcd"
               |  }@""".stripMargin
    an [IllegalArgumentException] should be thrownBy service.importFromFilePayload(rules, core1Id, repo)
  }

  it should "merge searchInputs with matching undirected synonyms and same rules" in {
    var rules: String = s"""input_1 =>
                           |	SYNONYM: input_2
                           |	DOWN(10): down_x
                           |
                           |input_2 =>
                           |	SYNONYM: input_1
                           |	DOWN(10): down_x""".stripMargin

    val (
      retstatCountRulesTxtInputs,
      retstatCountRulesTxtLinesSkipped,
      retstatCountRulesTxtUnkownConvert,
      retstatCountConsolidatedInputs,
      retstatCountConsolidatedRules
      ) = service.importFromFilePayload(rules, core1Id, repo)

    val searchInputWithRules = repo.listAllSearchInputsInclDirectedSynonyms(core1Id)
    searchInputWithRules.size shouldBe 1

    val searchInput_1 = loadSearchInputWithRules("input_1", searchInputWithRules)
    searchInput_1.synonymRules.size shouldBe 1
    searchInput_1.upDownRules.size shouldBe 1

    val searchInput_2 = loadSearchInputWithRules("input_2", searchInputWithRules)
    searchInput_2 shouldBe null
  }

  it should "NOT merge searchInputs with matching undirected synonyms and different rules" in {
    var rules: String = s"""input_1 =>
                           |	SYNONYM: input_2
                           |	DOWN(999999): down_x
                           |
                           |input_2 =>
                           |	SYNONYM: input_1
                           |	DOWN(10): down_x""".stripMargin

    val (
      retstatCountRulesTxtInputs,
      retstatCountRulesTxtLinesSkipped,
      retstatCountRulesTxtUnkownConvert,
      retstatCountConsolidatedInputs,
      retstatCountConsolidatedRules
      ) = service.importFromFilePayload(rules, core1Id, repo)

    val searchInputWithRules = repo.listAllSearchInputsInclDirectedSynonyms(core1Id)
    searchInputWithRules.size shouldBe 2

    val searchInput_1 = loadSearchInputWithRules("input_1", searchInputWithRules)
    searchInput_1.synonymRules.size shouldBe 1
    searchInput_1.upDownRules.size shouldBe 1
    val searchInput_2 = loadSearchInputWithRules("input_2", searchInputWithRules)
    searchInput_2.synonymRules.size shouldBe 1
    searchInput_2.upDownRules.size shouldBe 1

    //TODO verify retstatXYZ counts
  }

  it should "ignore tags if rule-tagging is not set" in {
    var rules: String = s"""input_1 =>
                           |	SYNONYM: synomyn_1
                           |  @{
                           |    "ignored_name" : "ignored_value"
                           |  }@
                           |
                           |input_2 =>
                           |	UP(10): up_2
                           |  @{
                           |    "ignored_name" : "ignored_value"
                           |  }@""".stripMargin

    val (
      retstatCountRulesTxtInputs,
      retstatCountRulesTxtLinesSkipped,
      retstatCountRulesTxtUnkownConvert,
      retstatCountConsolidatedInputs,
      retstatCountConsolidatedRules
      ) = service.importFromFilePayload(rules, core1Id, repo)

    val searchInputWithRules = repo.listAllSearchInputsInclDirectedSynonyms(core1Id)
    searchInputWithRules.size shouldBe 2

    val searchInput_1 = loadSearchInputWithRules("input_1", searchInputWithRules)
    searchInput_1.synonymRules.size shouldBe 1
    searchInput_1.tags.size shouldBe 0

    val searchInput_2 = loadSearchInputWithRules("input_2", searchInputWithRules)
    searchInput_2.upDownRules.size shouldBe 1
    searchInput_2.tags.size shouldBe 0

    retstatCountRulesTxtUnkownConvert shouldBe 6 //2x3 @ tag-lines
  }

}
