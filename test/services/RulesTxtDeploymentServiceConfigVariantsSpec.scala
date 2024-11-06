package services

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.Suite
import models.ApplicationTestBase
import models.input.SearchInputWithRules
import models.rules.{SynonymRule, SynonymRuleId}

// TODO maybe group test classes into RulesTxtDeploymentServiceConfigVariantsSpec (Suite?)

trait CommonRulesTxtDeploymentServiceConfigVariantsSpecBase extends ApplicationTestBase {
  self: Suite =>

  // TODO maybe share those definitions / instructions with RulesTxtDeploymentServiceSpec as well?

  protected lazy val service = injector.instanceOf[RulesTxtDeploymentService]

  override protected lazy val activateSpelling = false

  override protected def beforeAll(): Unit = {
    super.beforeAll()

    createTestCores()
    if (activateSpelling) {
      createTestSpellings()
    }
    createTestRule()
  }

  protected def createDecompoundRule() = {

    val damenInputId = repo.addNewSearchInput(core1Id, "damen*", Seq(), None)
    val damenInput = SearchInputWithRules(
      id = damenInputId,
      term = "damen*",
      synonymRules = List(
        SynonymRule(
          id = SynonymRuleId(),
          synonymType = SynonymRule.TYPE_DIRECTED,
          term = "damen $1",
          isActive = true
        )
      ),
      upDownRules = Nil,
      filterRules = Nil,
      deleteRules = Nil,
      redirectRules = Nil,
      tags = Seq.empty,
      isActive = true,
      comment = "German prefix to match all different kind women's wear as decompound prefix."
    )
    repo.updateSearchInput(damenInput, None)
  }

}

/**
  * Variants for different rules.txt, replace-rules.txt, decompound-rules.txt
  */

class RulesTxtOnlyDeploymentConfigVariantSpec extends AnyFlatSpec with Matchers with CommonRulesTxtDeploymentServiceConfigVariantsSpecBase {

  override protected lazy val additionalAppConfig = Seq(
    "smui2solr.SRC_TMP_FILE" -> "/changed-common-rules-temp-path/search-management-ui_rules-txt.tmp",
    "toggle.rule-deployment.pre-live.present" -> true,
  )

  "RulesTxtDeploymentService" should "provide only the (common) rules.txt" in {
    val deploymentDescriptor = service.generateRulesTxtContentWithFilenames(core1Id, logDebug = false)

    deploymentDescriptor.solrIndexId shouldBe core1Id
    deploymentDescriptor.regularRules.content should include ("aerosmith") // simply cross check content
    deploymentDescriptor.regularRules.sourceFileName shouldBe "/changed-common-rules-temp-path/search-management-ui_rules-txt.tmp"
    deploymentDescriptor.replaceRules shouldBe None
    deploymentDescriptor.decompoundRules shouldBe None
  }
}

class RulesAndReplaceTxtDeploymentConfigVariantSpec extends AnyFlatSpec with Matchers with CommonRulesTxtDeploymentServiceConfigVariantsSpecBase {

  override protected lazy val activateSpelling = true

  override protected lazy val additionalAppConfig = Seq(
    "smui2solr.SRC_TMP_FILE" -> "/changed-common-rules-temp-path/search-management-ui_rules-txt.tmp",
    "toggle.rule-deployment.pre-live.present" -> true,
    // spelling is activated (@see /smui/test/models/ApplicationTestBase.scala)
    "smui2solr.replace-rules-tmp-file" -> "/changed-replace-rules-temp-path/search-management-ui_replace-rules-txt.tmp"
  )

  "RulesTxtDeploymentService" should "provide the (common) rules.txt and a replace-rules.txt" in {
    val deploymentDescriptor = service.generateRulesTxtContentWithFilenames(core1Id, logDebug = false)

    deploymentDescriptor.solrIndexId shouldBe core1Id
    deploymentDescriptor.regularRules.content should include ("aerosmith") // simply cross check content
    deploymentDescriptor.regularRules.sourceFileName shouldBe "/changed-common-rules-temp-path/search-management-ui_rules-txt.tmp"

    val replaceRules = deploymentDescriptor.replaceRules.get
    replaceRules.content should include ("freezer") // simply cross check content
    replaceRules.sourceFileName shouldBe "/changed-replace-rules-temp-path/search-management-ui_replace-rules-txt.tmp"
    deploymentDescriptor.decompoundRules shouldBe None
  }

}

class RulesAndDecompoundTxtDeploymentConfigVariantSpec extends AnyFlatSpec with Matchers with CommonRulesTxtDeploymentServiceConfigVariantsSpecBase {

  override protected lazy val activateSpelling = false

  override protected lazy val additionalAppConfig = Seq(
    "smui2solr.SRC_TMP_FILE" -> "/changed-common-rules-temp-path/search-management-ui_rules-txt.tmp",
    "toggle.rule-deployment.pre-live.present" -> true,
    "toggle.rule-deployment.split-decompound-rules-txt" -> true
  )

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    createDecompoundRule()
  }

  "RulesTxtDeploymentService" should "provide the (common) rules.txt and a decompound-rules.txt" in {
    val deploymentDescriptor = service.generateRulesTxtContentWithFilenames(core1Id, logDebug = false)

    deploymentDescriptor.solrIndexId shouldBe core1Id
    deploymentDescriptor.regularRules.content should include ("aerosmith") // simply cross check content
    deploymentDescriptor.regularRules.sourceFileName shouldBe "/changed-common-rules-temp-path/search-management-ui_rules-txt.tmp"
    deploymentDescriptor.replaceRules shouldBe None

    val decompoundRules = deploymentDescriptor.decompoundRules.get
    decompoundRules.content should include ("damen")
    decompoundRules.sourceFileName shouldBe "/changed-common-rules-temp-path/search-management-ui_rules-txt.tmp-2" // auto generated (by spec)
  }

}

class RulesReplaceAndDecompoundTxtDeploymentConfigVariantSpec extends AnyFlatSpec with Matchers with CommonRulesTxtDeploymentServiceConfigVariantsSpecBase {

  override protected lazy val activateSpelling = true

  override protected lazy val additionalAppConfig = Seq(
    // (common) rules.txt config
    "smui2solr.SRC_TMP_FILE" -> "/changed-common-rules-temp-path/search-management-ui_rules-txt.tmp",
    "toggle.rule-deployment.pre-live.present" -> true,
    // replace-rules.txt config
    "smui2solr.replace-rules-tmp-file" -> "/changed-replace-rules-temp-path/search-management-ui_replace-rules-txt.tmp",
    // decompound-rules.txt config
    "toggle.rule-deployment.split-decompound-rules-txt" -> true
  )

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    createDecompoundRule()
  }

  "RulesTxtDeploymentService" should "provide the (common) rules.txt, replace-rules.txt and a decompound-rules.txt" in {
    val deploymentDescriptor = service.generateRulesTxtContentWithFilenames(core1Id, logDebug = false)

    deploymentDescriptor.solrIndexId shouldBe core1Id
    deploymentDescriptor.regularRules.content should include ("aerosmith") // simply cross check content
    deploymentDescriptor.regularRules.sourceFileName shouldBe "/changed-common-rules-temp-path/search-management-ui_rules-txt.tmp"

    val replaceRules = deploymentDescriptor.replaceRules.get
    replaceRules.content should include ("freezer") // simply cross check content
    replaceRules.sourceFileName shouldBe "/changed-replace-rules-temp-path/search-management-ui_replace-rules-txt.tmp"

    val decompoundRules = deploymentDescriptor.decompoundRules.get
    decompoundRules.content should include ("damen")
    decompoundRules.sourceFileName shouldBe "/changed-common-rules-temp-path/search-management-ui_rules-txt.tmp-2" // auto generated (by spec)
  }

}

/**
  * Interface with deployment script (for regular and "GIT" target alike)
  */

class RulesTxtDeploymentRegularTargetSpec extends AnyFlatSpec with Matchers with CommonRulesTxtDeploymentServiceConfigVariantsSpecBase {

  override protected lazy val activateSpelling = true

  override protected lazy val additionalAppConfig = Seq(
    // (common) rules.txt config
    "smui2solr.SRC_TMP_FILE" -> "/changed-common-rules-temp-path/search-management-ui_rules-txt.tmp",
    "toggle.rule-deployment.pre-live.present" -> true,
    // replace-rules.txt config
    "smui2solr.replace-rules-tmp-file" -> "/changed-replace-rules-temp-path/search-management-ui_replace-rules-txt.tmp",
    // decompound-rules.txt config
    "toggle.rule-deployment.split-decompound-rules-txt" -> true,
    // test script
    "toggle.rule-deployment.custom-script" -> true,
    "toggle.rule-deployment.custom-script-SMUI2SOLR-SH_PATH" -> "test/resources/smui2test.sh"
  )

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    createDecompoundRule()
  }

  "interfaceSmui2SolrSh" should "interface the test script should return all rules.txts for PRELIVE" in {
    val deploymentDescriptor = service.generateRulesTxtContentWithFilenames(core1Id, logDebug = false)
    val res = service.executeDeploymentScript(deploymentDescriptor, "PRELIVE")
    res.success shouldBe true
    res.output shouldBe s"""$$1 = >>>/changed-common-rules-temp-path/search-management-ui_rules-txt.tmp
                           |$$2 = >>>/changed-replace-rules-temp-path/search-management-ui_replace-rules-txt.tmp
                           |$$3 = >>>PRELIVE
                           |$$4 = >>>core1
                           |""".stripMargin
  }

  "interfaceSmui2SolrSh" should "interface the test script should return all rules.txts for LIVE" in {
    val deploymentDescriptor = service.generateRulesTxtContentWithFilenames(core1Id, logDebug = false)
    val res = service.executeDeploymentScript(deploymentDescriptor, "LIVE")
    res.success shouldBe true
    res.output shouldBe s"""$$1 = >>>/changed-common-rules-temp-path/search-management-ui_rules-txt.tmp
                           |$$2 = >>>/changed-replace-rules-temp-path/search-management-ui_replace-rules-txt.tmp
                           |$$3 = >>>LIVE
                           |$$4 = >>>core1
                           |""".stripMargin
  }

}

class RulesTxtDeploymentGitTargetSpec extends AnyFlatSpec with Matchers with CommonRulesTxtDeploymentServiceConfigVariantsSpecBase {

  override protected lazy val activateSpelling = true

  override protected lazy val additionalAppConfig = Seq(
    // switch to GIT for LIVE deployment
    "toggle.rule-deployment.git.enable" -> true,
    "smui.deployment.git.repo-url" -> "ssh://git@changed-git-server.tld/repos/smui_rulestxt_repo.git",
    "smui2solr.deployment.git.filename.common-rules-txt" -> "common-rules.txt",
    // (common) rules.txt config
    "smui2solr.SRC_TMP_FILE" -> "/changed-common-rules-temp-path/search-management-ui_rules-txt.tmp",
    "toggle.rule-deployment.pre-live.present" -> true,
    // replace-rules.txt config
    "smui2solr.replace-rules-tmp-file" -> "/changed-replace-rules-temp-path/search-management-ui_replace-rules-txt.tmp",
    // decompound-rules.txt config
    "toggle.rule-deployment.split-decompound-rules-txt" -> true
  )

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    createDecompoundRule()
  }

  // TODO think about make the interface to smui2git.sh also interchangable (like with smui2solr.sh) to inject a echoing test script
  // TODO think about bootstrapping a local git server (docker) within test, to test the whole roundtrip

  "interfaceSmui2GitSh" should "interface the test script should return all rules.txts for LIVE" in {

    val deploymentDescriptor = service.generateRulesTxtContentWithFilenames(core1Id, logDebug = false)
    val res = service.executeDeploymentScript(deploymentDescriptor, "LIVE")

    // TODO script result itself failed - this is cheesy, but cristal clear, as we don't have a local git server running or a test script instead

    res.output should include ("SRC_TMP_FILE:                 /changed-common-rules-temp-path/search-management-ui_rules-txt.tmp")
    res.output should include ("SMUI_GIT_REPOSITORY:          ssh://git@changed-git-server.tld/repos/smui_rulestxt_repo.git")
    res.output should include ("SMUI_GIT_FN_COMMON_RULES_TXT: common-rules.txt")

    // TODO As of v3.11.7 there is no option:
    // TODO ... to deploy to different git hosts / repos / branches (it all makes sense)
    // TODO ... to deploy further rules.txts (like replace-rules.txt, decompound-rules.txt)

  }

  "interfacing git configured SMUI" should "stick with file copy deployment configuration PRELIVE" in {

    val deploymentDescriptor = service.generateRulesTxtContentWithFilenames(core1Id, logDebug = false)
    val res = service.executeDeploymentScript(deploymentDescriptor, "PRELIVE")

    // TODO script result itself failed - this is cheesy, but cristal clear, as we don't have a local git server running or a test script instead

    res.exitCode shouldBe -1
    res.output shouldBe "Git deployment only support for LIVE target"

    // TODO As of v3.11.7 there is no option:
    // TODO ... to deploy to different git hosts / repos / branches (it all makes sense)
    // TODO ... to deploy further rules.txts (like replace-rules.txt, decompound-rules.txt)

  }


}
