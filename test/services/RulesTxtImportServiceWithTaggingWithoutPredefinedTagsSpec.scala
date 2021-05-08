package services

import models.ApplicationTestBase
import models.input.SearchInputWithRules
import models.rules.SynonymRule
import org.scalatest.{FlatSpec, Matchers}

class RulesTxtImportServiceWithTaggingWithoutPredefinedTagsSpec extends FlatSpec with Matchers with ApplicationTestBase {

  private lazy val service = injector.instanceOf[RulesTxtImportService]

  override protected lazy val additionalAppConfig = Seq(
    "toggle.rule-tagging" -> true,
    "toggle.predefined-tags-file" -> ""
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

  it should "import all tags by creating new tags and linking to existing ones" in {
    var rules: String = s"""input_1 =>
                           |  DOWN(10): down_x
                           |  @{
                           |    "_id" : "1",
                           |    "_log" : "ignored",
                           |    "new1" : "new1_val1"
                           |  }@
                           |
                           |input_2 =>
                           |  DOWN(10): down_x
                           |  @{
                           |    "_id" : "2",
                           |    "_log" : "ignored",
                           |    "new1" : ["new1_val1", "new1_val2"],
                           |    "new2" : ["new2_val1", "new2_val2"],
                           |    "new3" : "new3_val1"
                           |  }@
                           |
                           |input_3_merged =>
                           |  SYNONYM: input_4_merged
                           |  @{
                           |    "new1" : ["new1_val1", "new1_val2"],
                           |    "new2" : ["new2_val1", "new2_val2"],
                           |    "new3" : ["new3_val1", "new3_val2"]
                           |  }@
                           |
                           |input_4_merged =>
                           |  SYNONYM: input_3_merged
                           |  @{
                           |    "new1" : ["new1_val1", "new1_val2"],
                           |    "new2" : ["new2_val1", "new2_val2"],
                           |    "new3" : ["new3_val1", "new3_val2"]
                           |  }@
                           |
                           |input_5 =>
                           |  DOWN(10): down_x
                           |  @{
                           |    "new1" : ["new1_val1", "new1_val2"],
                           |    "new2" : ["new2_val1", "new2_val2"],
                           |    "new3" : ["new3_val1", "new3_val2", "new3_val3"],
                           |    "new4" : "new4_val1"
                           |  }@""".stripMargin

    val (
      retstatCountRulesTxtInputs,
      retstatCountRulesTxtLinesSkipped,
      retstatCountRulesTxtUnkownConvert,
      retstatCountConsolidatedInputs,
      retstatCountConsolidatedRules
      ) = service.importFromFilePayload(rules, core1Id)

    val searchInputWithRules = repo.listAllSearchInputsInclDirectedSynonyms(core1Id)
    searchInputWithRules.size shouldBe 4

    repo.listAllInputTags().size shouldBe 8

    loadSearchInputWithRules("input_1", searchInputWithRules).tags.size shouldBe 1
    loadSearchInputWithRules("input_2", searchInputWithRules).tags.size shouldBe 5
    loadSearchInputWithRules("input_3_merged", searchInputWithRules).tags.size shouldBe 6
    loadSearchInputWithRules("input_3_merged", searchInputWithRules).synonymRules.head.synonymType shouldBe SynonymRule.TYPE_UNDIRECTED
    loadSearchInputWithRules("input_5", searchInputWithRules).tags.size shouldBe 8
  }

}
