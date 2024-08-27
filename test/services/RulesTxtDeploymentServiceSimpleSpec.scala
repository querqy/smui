package services

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.zip.ZipInputStream

import models.ApplicationTestBase
import models.input.SearchInputId
import org.apache.commons.io.IOUtils
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class RulesTxtDeploymentServiceSpec extends AnyFlatSpec with Matchers with ApplicationTestBase {

  private lazy val service = injector.instanceOf[RulesTxtDeploymentService]
  private var inputIds: Seq[SearchInputId] = Seq.empty

  override protected def beforeAll(): Unit = {
    super.beforeAll()

    createTestCores()
    createTestSpellings()
    inputIds = createTestRule()
  }

  private def rulesFileContent(ruleIds: Seq[SearchInputId]): String = s"""aerosmith =>
                           |	SYNONYM: directed
                           |	SYNONYM: mercury
                           |	DOWN(10): battery
                           |	UP(10): notebook
                           |	FILTER: zz top
                           |	@{
                           |	  "_log" : "${ruleIds.head}"
                           |	}@
                           |
                           |mercury =>
                           |	SYNONYM: aerosmith
                           |	SYNONYM: directed
                           |	DOWN(10): battery
                           |	UP(10): notebook
                           |	FILTER: zz top
                           |	@{
                           |	  "_log" : "${ruleIds.head}"
                           |	}@
                           |
                           |shipping =>
                           |	DECORATE: REDIRECT http://xyz.com/shipping
                           |	@{
                           |	  "_log" : "${ruleIds.last}"
                           |	}@""".stripMargin

  private def replaceRulesFileContent(): String =
    s"""frazer; freazer; frezer => freezer
       |mechine => machine""".stripMargin

  "RulesTxtDeploymentService" should "generate rules files with correct file names" in {
    val rulesTxt = service.generateRulesTxtContentWithFilenames(core1Id, "LIVE", logDebug = false)
    rulesTxt.solrIndexId shouldBe core1Id
    rulesTxt.decompoundRules shouldBe empty
    rulesTxt.regularRules.content.trim shouldBe rulesFileContent(inputIds)

    rulesTxt.regularRules.sourceFileName shouldBe "/tmp/search-management-ui_rules-txt.tmp"
    rulesTxt.regularRules.destinationFileName shouldBe "/usr/bin/solr/liveCore/conf/rules.txt"

    rulesTxt.replaceRules.get.content.trim shouldBe replaceRulesFileContent()
    rulesTxt.replaceRules.get.sourceFileName shouldBe "/tmp/search-management-ui_replace-rules-txt.tmp"
    rulesTxt.replaceRules.get.destinationFileName shouldBe "/usr/bin/solr/liveCore/conf/replace-rules.txt"
  }

  it should "validate the rules files correctly" in {
    val rulesTxt = service.generateRulesTxtContentWithFilenames(core1Id, "LIVE", logDebug = false)
    service.validateCompleteRulesTxts(rulesTxt, logDebug = false) shouldBe empty

    val badRulesTxt = rulesTxt.copy(regularRules = rulesTxt.regularRules.copy(content = "a very bad rules file"))
    service.validateCompleteRulesTxts(badRulesTxt, logDebug = false) shouldBe List("Line 1: Missing input for instruction")

    val badReplaceRules = rulesTxt.replaceRules.get.copy(content = "a very bad repalce rules file")
    val badReplaceRulesTxt = rulesTxt.copy(replaceRules = Some(badReplaceRules))
    service.validateCompleteRulesTxts(badReplaceRulesTxt, logDebug = false).head should
      include("Each non-empty line must either start with # or contain a rule")
  }

  it should "provide a zip file with all rules files" in {
    val out = new ByteArrayOutputStream()
    service.writeAllRulesTxtFilesAsZipFileToStream(out)

    val bytes = out.toByteArray
    val zipStream = new ZipInputStream(new ByteArrayInputStream(bytes))

    val expectedZipContent = Map(
      "rules_core1.txt" -> rulesFileContent(inputIds),
      "replace-rules_core1.txt" -> replaceRulesFileContent(),
      "rules_core2.txt" -> "",
      "replace-rules_core2.txt" -> ""
    )

    expectedZipContent.foreach { case (name: String, content: String) =>
      validateRulesInZipFile(zipStream, name, content)
    }
  }

  private def validateRulesInZipFile(zipStream: ZipInputStream, expectedName: String, expectedContent: String): Unit = {
    zipStream.getNextEntry.getName shouldBe expectedName
    IOUtils.toString(zipStream, "UTF-8").trim shouldBe expectedContent
  }

}
