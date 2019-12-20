package models

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.zip.ZipInputStream

import org.apache.commons.io.IOUtils
import org.scalatest.{FlatSpec, Matchers}

class RulesTxtDeploymentServiceSpec extends FlatSpec with Matchers with ApplicationTestBase {

  private lazy val service = injector.instanceOf[RulesTxtDeploymentService]
  private var inputIds: Seq[SearchInputId] = Seq.empty

  override protected def beforeAll(): Unit = {
    super.beforeAll()

    createTestCores()
    inputIds = createTestRule()
  }

  private def rulesFileContent(ruleIds: Seq[SearchInputId]): String = s"""aerosmith =>
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

  "RulesTxtDeploymentService" should "generate rules files with correct file names" in {
    val rulesTxt = service.generateRulesTxtContentWithFilenames(core1Id, "LIVE", logDebug = false)
    rulesTxt.solrIndexId shouldBe core1Id
    rulesTxt.decompoundRules shouldBe empty
    rulesTxt.regularRules.content.trim shouldBe rulesFileContent(inputIds)

    rulesTxt.regularRules.sourceFileName shouldBe "/tmp/search-management-ui_rules-txt.tmp"
    rulesTxt.regularRules.destinationFileName shouldBe "/usr/bin/solr/defaultCore/conf/rules.txt"
  }

  it should "validate the rules files correctly" in {
    val rulesTxt = service.generateRulesTxtContentWithFilenames(core1Id, "LIVE", logDebug = false)
    service.validateCompleteRulesTxts(rulesTxt, logDebug = false) shouldBe empty

    val badRulesTxt = rulesTxt.copy(regularRules = rulesTxt.regularRules.copy(content = "a very bad rules file"))
    service.validateCompleteRulesTxts(badRulesTxt, logDebug = false) shouldBe List("Line 1: Missing input for instruction")
  }

  it should "provide a zip file with all rules files" in {
    val out = new ByteArrayOutputStream()
    service.writeAllRulesTxtFilesAsZipFileToStream(out)

    val bytes = out.toByteArray
    val zipStream = new ZipInputStream(new ByteArrayInputStream(bytes))
    val firstEntry = zipStream.getNextEntry
    firstEntry.getName shouldBe "rules_core1.txt"
    IOUtils.toString(zipStream, "UTF-8").trim shouldBe rulesFileContent(inputIds)
    val secondEntry = zipStream.getNextEntry
    secondEntry.getName shouldBe "rules_core2.txt"
    IOUtils.toString(zipStream, "UTF-8").trim shouldBe ""
  }

}
