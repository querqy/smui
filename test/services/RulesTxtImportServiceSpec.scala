package services

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.zip.ZipInputStream

import models.ApplicationTestBase
import models.input.{SearchInputId, SearchInputWithRules}
import org.apache.commons.io.IOUtils
import org.scalatest.{FlatSpec, Matchers}

import querqy.rewrite.commonrules.RuleParseException

class RulesTxtImportServiceSpec extends FlatSpec with Matchers with ApplicationTestBase {

  private lazy val service = injector.instanceOf[RulesTxtImportService]
  private var inputIds: Seq[SearchInputId] = Seq.empty

  override protected def beforeAll(): Unit = {
    super.beforeAll()

    createTestCores()
    createTestTag("tag_1", "tag_1_val_1", core1Id)
    createTestTag("tag_1", "tag_1_val_2", core1Id)
    createTestTag("tag_2", "tag_2_val_1", core1Id)
    //createTestSpellings()
    //inputIds = createTestRule()
    //createTestInputTags()
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    deleteAllSearchInputsFromDb(core1Id)
  }

  def getSearchInputWithRules(searchInputTerm: String, searchInputs: List[SearchInputWithRules]): SearchInputWithRules = {
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
      retstatCountConsolidatedRules,
      retstatUnknownTags
    ) = service.importFromFilePayload(rules, core1Id, repo)

    val searchInputWithRules = repo.listAllSearchInputsInclDirectedSynonyms(core1Id)
    searchInputWithRules.size shouldBe 2

    val searchInput_1 = getSearchInputWithRules("input_1", searchInputWithRules)
    searchInput_1.synonymRules.size shouldBe 1
    searchInput_1.upDownRules.size shouldBe 2
    searchInput_1.deleteRules.size shouldBe 1
    searchInput_1.filterRules.size shouldBe 1
    searchInput_1.redirectRules.size shouldBe 0
    searchInput_1.tags.size shouldBe 0
    searchInput_1.isActive shouldBe true
    searchInput_1.comment shouldBe "Added by rules.txt import."

    val searchInput_2 = getSearchInputWithRules("input_2", searchInputWithRules)
    searchInput_2.synonymRules.size shouldBe 1
    searchInput_2.upDownRules.size shouldBe 2
    searchInput_2.deleteRules.size shouldBe 1
    searchInput_2.filterRules.size shouldBe 1
    searchInput_2.redirectRules.size shouldBe 0
    searchInput_2.tags.size shouldBe 0
    searchInput_2.isActive shouldBe true
    searchInput_2.comment shouldBe "Added by rules.txt import."

    //TODO verify retstatXYZ counts
//    retstatCountRulesTxtInputs shouldBe 2
//    retstatCountRulesTxtLinesSkipped shouldBe 1
//    retstatCountRulesTxtUnkownConvert shouldBe 0
//    retstatCountConsolidatedInputs shouldBe 1
//    retstatCountConsolidatedRules shouldBe 10
//    retstatUnknownTags shouldBe ""
  }

  it should "import tags having existing matching tag in the db and ignore the others " in {
    var rules: String = s"""input_1 =>
                           |  DOWN(10): down_x
                           |  @{
                           |    "tag_1" : "tag_1_val_1",
                           |    "tag_2" : "tag_2_val_1",
                           |    "not_existing_name" : "tag_2_val_2"
                           |  }@
                           |
                           |input_2 =>
                           |  DOWN(10): down_x
                           |  @{
                           |    "tag_1" : ["tag_1_val_1", "tag_1_val_2"],
                           |    "tag_2" : ["tag_2_val_1", "XXX"]
                           |  }@""".stripMargin

    val (
      retstatCountRulesTxtInputs,
      retstatCountRulesTxtLinesSkipped,
      retstatCountRulesTxtUnkownConvert,
      retstatCountConsolidatedInputs,
      retstatCountConsolidatedRules,
      retstatUnknownTags
      ) = service.importFromFilePayload(rules, core1Id, repo)

    val searchInputWithRules = repo.listAllSearchInputsInclDirectedSynonyms(core1Id)
    searchInputWithRules.size shouldBe 2

    val searchInput_1 = getSearchInputWithRules("input_1", searchInputWithRules)
    searchInput_1.tags.size shouldBe 2

    val searchInput_2 = getSearchInputWithRules("input_2", searchInputWithRules)
    searchInput_2.tags.size shouldBe 3

    retstatUnknownTags should include ("not_existing_name : tag_2_val_2")
    retstatUnknownTags should include ("tag_2 : XXX")
  }

  it should "fail for invalid syntax" in {
    var rules: String = s"""input_1 =
                         |	SYNONYM: synomyn_1""".stripMargin
    an [RuleParseException] should be thrownBy service.importFromFilePayload(rules, core1Id, repo)

    rules = s"""input_1 => SYNONYM: synomyn_1""".stripMargin
    an [RuleParseException] should be thrownBy service.importFromFilePayload(rules, core1Id, repo)

    rules = s"""input_1 =>
             |	xzy: synomyn_1""".stripMargin
    an [RuleParseException] should be thrownBy service.importFromFilePayload(rules, core1Id, repo)

    rules = s"""input_1 =>
               |	SYNONYM: synomyn_1
               |  @{
               |    "missing_@" : "value"
               |  }""".stripMargin
    an [RuleParseException] should be thrownBy service.importFromFilePayload(rules, core1Id, repo)

    rules = s"""input_1 =>
               |	SYNONYM: synomyn_1
               |  @{
               |    "name" : "value"
               |    "missing_comma" : "abcd"
               |  }@""".stripMargin
    an [RuleParseException] should be thrownBy service.importFromFilePayload(rules, core1Id, repo)
  }

  it should "consolidate searchInputs with matching undirected synonyms and same rules" in {
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
      retstatCountConsolidatedRules,
      retstatUnknownTags
      ) = service.importFromFilePayload(rules, core1Id, repo)

    val searchInputWithRules = repo.listAllSearchInputsInclDirectedSynonyms(core1Id)
    searchInputWithRules.size shouldBe 1

    val searchInput_1 = getSearchInputWithRules("input_1", searchInputWithRules)
    searchInput_1.synonymRules.size shouldBe 1
    searchInput_1.upDownRules.size shouldBe 1

    val searchInput_2 = getSearchInputWithRules("input_2", searchInputWithRules)
    searchInput_2 shouldBe null

    //TODO verify retstatXYZ counts
  }

  it should "NOT consolidate searchInputs with matching undirected synonyms and different rules" in {
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
      retstatCountConsolidatedRules,
      retstatUnknownTags
      ) = service.importFromFilePayload(rules, core1Id, repo)

    val searchInputWithRules = repo.listAllSearchInputsInclDirectedSynonyms(core1Id)
    searchInputWithRules.size shouldBe 2

    val searchInput_1 = getSearchInputWithRules("input_1", searchInputWithRules)
    searchInput_1.synonymRules.size shouldBe 1
    searchInput_1.upDownRules.size shouldBe 1
    val searchInput_2 = getSearchInputWithRules("input_2", searchInputWithRules)
    searchInput_2.synonymRules.size shouldBe 1
    searchInput_2.upDownRules.size shouldBe 1

    //TODO verify retstatXYZ counts
  }

  // TODO activate tests and implement feature
//  it should "consolidate searchInputs with matching undirected synonyms and same tags" in {
//    var rules: String = s"""input_1 =>
//                           |	SYNONYM: input_2
//                           |  @{
//                           |    "tag_1" : ["tag_1_val_1", "tag_1_val_2"],
//                           |    "ignored_tag" : "does not count"
//                           |  }@
//                           |
//                           |input_2 =>
//                           |	SYNONYM: input_1
//                           |	@{
//                           |    "tag_1" : ["tag_1_val_1", "tag_1_val_2"]
//                           |  }@""".stripMargin
//
//    val (
//      retstatCountRulesTxtInputs,
//      retstatCountRulesTxtLinesSkipped,
//      retstatCountRulesTxtUnkownConvert,
//      retstatCountConsolidatedInputs,
//      retstatCountConsolidatedRules,
//      retstatUnknownTags
//      ) = service.importFromFilePayload(rules, core1Id, repo)
//
//    val searchInputWithRules = repo.listAllSearchInputsInclDirectedSynonyms(core1Id)
//    searchInputWithRules.size shouldBe 1
//
//    val searchInput_1 = getSearchInputWithRules("input_1", searchInputWithRules)
//    searchInput_1.tags.size shouldBe 2
//
//    val searchInput_2 = getSearchInputWithRules("input_2", searchInputWithRules)
//    searchInput_2 shouldBe null
//    //TODO verify retstatXYZ counts
//  }
//
//  it should "NOT consolidate searchInputs with matching undirected synonyms and different tags" in {
//    var rules: String = s"""input_1 =>
//                           |	SYNONYM: input_2
//                           |  @{
//                           |    "tag_1" : ["tag_1_val_1"]
//                           |  }@
//                           |
//                           |input_2 =>
//                           |	SYNONYM: input_1
//                           |	@{
//                           |    "tag_1" : ["tag_1_val_1", "tag_1_val_2"]
//                           |  }@""".stripMargin
//
//    val (
//      retstatCountRulesTxtInputs,
//      retstatCountRulesTxtLinesSkipped,
//      retstatCountRulesTxtUnkownConvert,
//      retstatCountConsolidatedInputs,
//      retstatCountConsolidatedRules,
//      retstatUnknownTags
//      ) = service.importFromFilePayload(rules, core1Id, repo)
//
//    val searchInputWithRules = repo.listAllSearchInputsInclDirectedSynonyms(core1Id)
//    searchInputWithRules.size shouldBe 2
//
//    val searchInput_1 = getSearchInputWithRules("input_1", searchInputWithRules)
//    searchInput_1.tags.size shouldBe 1
//    val searchInput_2 = getSearchInputWithRules("input_2", searchInputWithRules)
//    searchInput_2.tags.size shouldBe 2
//
//    //TODO verify retstatXYZ counts
//  }
}
