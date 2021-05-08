package services

import models.ApplicationTestBase
import models.input.SearchInputWithRules
import models.rules.SynonymRule
import org.scalatest.{FlatSpec, Matchers}

class RulesTxtImportServiceWithTaggingAndPredefinedTagsSpec extends FlatSpec with Matchers with ApplicationTestBase {

  private lazy val service = injector.instanceOf[RulesTxtImportService]

  override protected lazy val additionalAppConfig = Seq(
    "toggle.rule-tagging" -> true,
    "toggle.predefined-tags-file" -> "./test/resources/TestRulesTxtImportTags.json"
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

  it should "import tags having existing matching tag in the db" in {
    var rules: String = s"""input_1 =>
                           |  DOWN(10): down_x
                           |  @{
                           |    "_log" : "should be ignored by import",
                           |    "_id" : "should be ignored by import",
                           |    "tenant" : "AA"
                           |  }@
                           |
                           |input_2 =>
                           |  DOWN(10): down_x
                           |  @{
                           |    "tenant" : ["AA", "BB"]
                           |  }@""".stripMargin

    val (
      retstatCountRulesTxtInputs,
      retstatCountRulesTxtLinesSkipped,
      retstatCountRulesTxtUnkownConvert,
      retstatCountConsolidatedInputs,
      retstatCountConsolidatedRules
      ) = service.importFromFilePayload(rules, core1Id)

    val searchInputWithRules = repo.listAllSearchInputsInclDirectedSynonyms(core1Id)
    searchInputWithRules.size shouldBe 2

    val searchInput_1 = loadSearchInputWithRules("input_1", searchInputWithRules)
    searchInput_1.tags.size shouldBe 1

    val searchInput_2 = loadSearchInputWithRules("input_2", searchInputWithRules)
    searchInput_2.tags.size shouldBe 2
  }

  it should "fail for unknown (not predefined) tags" in {
    var rules: String = s"""input_1 =>
                           |  DOWN(10): down_x
                           |  @{
                           |    "tenant" : "AA",
                           |  }@
                           |
                           |input_2 =>
                           |  DOWN(10): down_x
                           |  @{
                           |    "NOT_EXISTING_NAME" : "AA"
                           |  }@""".stripMargin

    an [IllegalArgumentException] should be thrownBy service.importFromFilePayload(rules, core1Id)
    repo.listAllSearchInputsInclDirectedSynonyms(core1Id).size shouldBe 0

     rules = s"""input_1 =>
                           |input_1 =>
                           |  DOWN(10): down_x
                           |  @{
                           |    "tenant" : ["AA", "BB"]
                           |    "NOT_EXISTING_NAME" : "de"
                           |  }@""".stripMargin
    an [IllegalArgumentException] should be thrownBy service.importFromFilePayload(rules, core1Id)
    repo.listAllSearchInputsInclDirectedSynonyms(core1Id).size shouldBe 0

    rules = s"""input_1 =>
               |input_1 =>
               |  DOWN(10): down_x
               |  @{
               |    "tenant" : ["AA", "NOT_EXISTING_VALUE"]
               |  }@""".stripMargin
    an [IllegalArgumentException] should be thrownBy service.importFromFilePayload(rules, core1Id)
    repo.listAllSearchInputsInclDirectedSynonyms(core1Id).size shouldBe 0
  }

  it should "merge searchInputs with matching undirected synonyms and same tags" in {
    var rules: String = s"""merged_1_0 =>
                           |  SYNONYM: merged_1_1
                           |  DOWN(10): down_x
                           |  @{
                           |    "tenant" : ["AA", "BB"]
                           |  }@
                           |
                           |merged_1_1 =>
                           |  SYNONYM: merged_1_0
                           |  DOWN(10): down_x
                           |  @{
                           |    "tenant" : ["AA", "BB"]
                           |  }@
                           |
                           |merged_2_0 =>
                           |  SYNONYM: merged_2_1
                           |  @{
                           |    "tenant" : ["AA","BB"],
                           |    "lang" : "de"
                           |  }@
                           |
                           |merged_2_1 =>
                           |  SYNONYM: merged_2_0
                           |  @{
                           |    "lang" : "de",
                           |    "tenant" : ["BB", "AA"]
                           |  }@""".stripMargin

    val (
      retstatCountRulesTxtInputs,
      retstatCountRulesTxtLinesSkipped,
      retstatCountRulesTxtUnkownConvert,
      retstatCountConsolidatedInputs,
      retstatCountConsolidatedRules
      ) = service.importFromFilePayload(rules, core1Id)

    val searchInputWithRules = repo.listAllSearchInputsInclDirectedSynonyms(core1Id)
    searchInputWithRules.size shouldBe 2

    val searchInput_1 = loadSearchInputWithRules("merged_1_0", searchInputWithRules)
    searchInput_1.synonymRules.head.synonymType shouldBe SynonymRule.TYPE_UNDIRECTED
    searchInput_1.synonymRules.head.term shouldBe "merged_1_1"

    val searchInput_2 = loadSearchInputWithRules("merged_2_0", searchInputWithRules)
    searchInput_2.synonymRules.head.synonymType shouldBe SynonymRule.TYPE_UNDIRECTED
    searchInput_2.synonymRules.head.term shouldBe "merged_2_1"
  }

    it should "NOT merge searchInputs with matching undirected synonyms and different tags" in {
      var rules: String = s"""not_merged_1_0 =>
                             |  SYNONYM: not_merged_1_1
                             |  @{
                             |    "tenant" : ["AA", "BB"]
                             |  }@
                             |
                             |not_merged_1_1 =>
                             |  SYNONYM: not_merged_1_0
                             |  @{
                             |    "tenant" : ["AA"]
                             |  }@
                             |
                             |not_merged_2_0 =>
                             |  SYNONYM: not_merged_2_1
                             |  @{
                             |    "tenant" : "AA"
                             |  }@
                             |
                             |not_merged_2_1 =>
                             |  SYNONYM: not_merged_2_0
                             |
                             |not_merged_3_0 =>
                             |  SYNONYM: not_merged_3_1
                             |  @{
                             |    "tenant" : ["AA", "BB"],
                             |    "lang" : "de"
                             |  }@
                             |
                             |not_merged_3_1 =>
                             |  SYNONYM: not_merged_3_0
                             |  @{
                             |    "tenant" : ["AA", "BB"],
                             |    "lang" : "en"
                             |  }@""".stripMargin

      val (
        retstatCountRulesTxtInputs,
        retstatCountRulesTxtLinesSkipped,
        retstatCountRulesTxtUnkownConvert,
        retstatCountConsolidatedInputs,
        retstatCountConsolidatedRules
        ) = service.importFromFilePayload(rules, core1Id)

      val searchInputWithRules = repo.listAllSearchInputsInclDirectedSynonyms(core1Id)
      searchInputWithRules.size shouldBe 6

      val searchInput_1 = loadSearchInputWithRules("not_merged_1_0", searchInputWithRules)
      searchInput_1.synonymRules.head.synonymType shouldBe SynonymRule.TYPE_DIRECTED
      searchInput_1.synonymRules.head.term shouldBe "not_merged_1_1"

      val searchInput_2 = loadSearchInputWithRules("not_merged_1_1", searchInputWithRules)
      searchInput_2.synonymRules.head.synonymType shouldBe SynonymRule.TYPE_DIRECTED
      searchInput_2.synonymRules.head.term shouldBe "not_merged_1_0"
    }

}
