package services

import org.scalatest.{FlatSpec, Matchers, Suite}
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

class RulesTxtOnlyDeploymentConfigVariantSpec extends FlatSpec with Matchers with CommonRulesTxtDeploymentServiceConfigVariantsSpecBase {

  override protected lazy val additionalAppConfig = Seq(
    "smui2solr.SRC_TMP_FILE" -> "/changed-common-rules-temp-path/search-management-ui_rules-txt.tmp",
    "smui2solr.DST_CP_FILE_TO" -> "/deployment-path-live/common-rules.txt",
    "toggle.rule-deployment.pre-live.present" -> true,
    "smui2solr.deploy-prelive-fn-rules-txt" -> "/deployment-path-prelive/common-rules.txt"
  )

  "RulesTxtDeploymentService" should "provide only the (common) rules.txt for PRELIVE" in {
    val deploymentDescriptor = service.generateRulesTxtContentWithFilenames(core1Id, "PRELIVE", logDebug = false)

    deploymentDescriptor.solrIndexId shouldBe core1Id
    deploymentDescriptor.regularRules.content should include ("aerosmith") // simply cross check content
    deploymentDescriptor.regularRules.sourceFileName shouldBe "/changed-common-rules-temp-path/search-management-ui_rules-txt.tmp"
    deploymentDescriptor.regularRules.destinationFileName shouldBe "/deployment-path-prelive/common-rules.txt"
    deploymentDescriptor.replaceRules shouldBe None
    deploymentDescriptor.decompoundRules shouldBe None
  }

  "RulesTxtDeploymentService" should "provide only the (common) rules.txt for LIVE" in {
    val deploymentDescriptor = service.generateRulesTxtContentWithFilenames(core1Id, "LIVE", logDebug = false)

    deploymentDescriptor.solrIndexId shouldBe core1Id
    deploymentDescriptor.regularRules.content should include ("aerosmith") // simply cross check content
    deploymentDescriptor.regularRules.sourceFileName shouldBe "/changed-common-rules-temp-path/search-management-ui_rules-txt.tmp"
    deploymentDescriptor.regularRules.destinationFileName shouldBe "/deployment-path-live/common-rules.txt"
    deploymentDescriptor.replaceRules shouldBe None
    deploymentDescriptor.decompoundRules shouldBe None
  }

}

class RulesAndReplaceTxtDeploymentConfigVariantSpec extends FlatSpec with Matchers with CommonRulesTxtDeploymentServiceConfigVariantsSpecBase {

  override protected lazy val activateSpelling = true

  override protected lazy val additionalAppConfig = Seq(
    "smui2solr.SRC_TMP_FILE" -> "/changed-common-rules-temp-path/search-management-ui_rules-txt.tmp",
    "smui2solr.DST_CP_FILE_TO" -> "/deployment-path-live/common-rules.txt",
    "toggle.rule-deployment.pre-live.present" -> true,
    "smui2solr.deploy-prelive-fn-rules-txt" -> "/deployment-path-prelive/common-rules.txt",
    // spelling is activated (@see /smui/test/models/ApplicationTestBase.scala)
    "smui2solr.replace-rules-tmp-file" -> "/changed-replace-rules-temp-path/search-management-ui_replace-rules-txt.tmp",
    "smui2solr.replace-rules-dst-cp-file-to" -> "/deployment-path-live/replace-rules.txt",
    "smui2solr.deploy-prelive-fn-replace-txt" -> "/deployment-path-prelive/replace-rules.txt"
  )

  "RulesTxtDeploymentService" should "provide the (common) rules.txt and a replace-rules.txt for PRELIVE" in {
    val deploymentDescriptor = service.generateRulesTxtContentWithFilenames(core1Id, "PRELIVE", logDebug = false)

    deploymentDescriptor.solrIndexId shouldBe core1Id
    deploymentDescriptor.regularRules.content should include ("aerosmith") // simply cross check content
    deploymentDescriptor.regularRules.sourceFileName shouldBe "/changed-common-rules-temp-path/search-management-ui_rules-txt.tmp"
    deploymentDescriptor.regularRules.destinationFileName shouldBe "/deployment-path-prelive/common-rules.txt"

    val replaceRules = deploymentDescriptor.replaceRules.get
    replaceRules.content should include ("freezer") // simply cross check content
    replaceRules.sourceFileName shouldBe "/changed-replace-rules-temp-path/search-management-ui_replace-rules-txt.tmp"
    replaceRules.destinationFileName shouldBe "/deployment-path-prelive/replace-rules.txt"
    deploymentDescriptor.decompoundRules shouldBe None
  }

  "RulesTxtDeploymentService" should "provide the (common) rules.txt and a replace-rules.txt for LIVE" in {
    val deploymentDescriptor = service.generateRulesTxtContentWithFilenames(core1Id, "LIVE", logDebug = false)

    deploymentDescriptor.solrIndexId shouldBe core1Id
    deploymentDescriptor.regularRules.content should include ("aerosmith") // simply cross check content
    deploymentDescriptor.regularRules.sourceFileName shouldBe "/changed-common-rules-temp-path/search-management-ui_rules-txt.tmp"
    deploymentDescriptor.regularRules.destinationFileName shouldBe "/deployment-path-live/common-rules.txt"

    val replaceRules = deploymentDescriptor.replaceRules.get
    replaceRules.content should include ("freezer") // simply cross check content
    replaceRules.sourceFileName shouldBe "/changed-replace-rules-temp-path/search-management-ui_replace-rules-txt.tmp"
    replaceRules.destinationFileName shouldBe "/deployment-path-live/replace-rules.txt"
    deploymentDescriptor.decompoundRules shouldBe None
  }

}

class RulesAndDecompoundTxtDeploymentConfigVariantSpec extends FlatSpec with Matchers with CommonRulesTxtDeploymentServiceConfigVariantsSpecBase {

  override protected lazy val activateSpelling = false

  override protected lazy val additionalAppConfig = Seq(
    "smui2solr.SRC_TMP_FILE" -> "/changed-common-rules-temp-path/search-management-ui_rules-txt.tmp",
    "smui2solr.DST_CP_FILE_TO" -> "/deployment-path-live/common-rules.txt",
    "toggle.rule-deployment.pre-live.present" -> true,
    "smui2solr.deploy-prelive-fn-rules-txt" -> "/deployment-path-prelive/common-rules.txt",
    "toggle.rule-deployment.split-decompound-rules-txt" -> true,
    "toggle.rule-deployment.split-decompound-rules-txt-DST_CP_FILE_TO" -> "/deployment-path-live/decompound-rules.txt",
    "smui2solr.deploy-prelive-fn-decompound-txt" -> "/deployment-path-prelive/decompound-rules.txt"
  )

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    createDecompoundRule()
  }

  "RulesTxtDeploymentService" should "provide the (common) rules.txt and a decompound-rules.txt for PRELIVE" in {
    val deploymentDescriptor = service.generateRulesTxtContentWithFilenames(core1Id, "PRELIVE", logDebug = false)

    deploymentDescriptor.solrIndexId shouldBe core1Id
    deploymentDescriptor.regularRules.content should include ("aerosmith") // simply cross check content
    deploymentDescriptor.regularRules.sourceFileName shouldBe "/changed-common-rules-temp-path/search-management-ui_rules-txt.tmp"
    deploymentDescriptor.regularRules.destinationFileName shouldBe "/deployment-path-prelive/common-rules.txt"
    deploymentDescriptor.replaceRules shouldBe None

    val decompoundRules = deploymentDescriptor.decompoundRules.get
    decompoundRules.content should include ("damen")
    decompoundRules.sourceFileName shouldBe "/changed-common-rules-temp-path/search-management-ui_rules-txt.tmp-2" // auto generated (by spec)
    decompoundRules.destinationFileName shouldBe "/deployment-path-prelive/decompound-rules.txt"
  }

  "RulesTxtDeploymentService" should "provide the (common) rules.txt and a decompound-rules.txt for LIVE" in {
    val deploymentDescriptor = service.generateRulesTxtContentWithFilenames(core1Id, "LIVE", logDebug = false)

    deploymentDescriptor.solrIndexId shouldBe core1Id
    deploymentDescriptor.regularRules.content should include ("aerosmith") // simply cross check content
    deploymentDescriptor.regularRules.sourceFileName shouldBe "/changed-common-rules-temp-path/search-management-ui_rules-txt.tmp"
    deploymentDescriptor.regularRules.destinationFileName shouldBe "/deployment-path-live/common-rules.txt"
    deploymentDescriptor.replaceRules shouldBe None

    val decompoundRules = deploymentDescriptor.decompoundRules.get
    decompoundRules.content should include ("damen")
    decompoundRules.sourceFileName shouldBe "/changed-common-rules-temp-path/search-management-ui_rules-txt.tmp-2" // auto generated (by spec)
    decompoundRules.destinationFileName shouldBe "/deployment-path-live/decompound-rules.txt"
  }

}

class RulesReplaceAndDecompoundTxtDeploymentConfigVariantSpec extends FlatSpec with Matchers with CommonRulesTxtDeploymentServiceConfigVariantsSpecBase {

  override protected lazy val activateSpelling = true

  override protected lazy val additionalAppConfig = Seq(
    // (common) rules.txt config
    "smui2solr.SRC_TMP_FILE" -> "/changed-common-rules-temp-path/search-management-ui_rules-txt.tmp",
    "smui2solr.DST_CP_FILE_TO" -> "/deployment-path-live/common-rules.txt",
    "toggle.rule-deployment.pre-live.present" -> true,
    "smui2solr.deploy-prelive-fn-rules-txt" -> "/deployment-path-prelive/common-rules.txt",
    // replace-rules.txt config
    "smui2solr.replace-rules-tmp-file" -> "/changed-replace-rules-temp-path/search-management-ui_replace-rules-txt.tmp",
    "smui2solr.replace-rules-dst-cp-file-to" -> "/deployment-path-live/replace-rules.txt",
    "smui2solr.deploy-prelive-fn-replace-txt" -> "/deployment-path-prelive/replace-rules.txt",
    // decompound-rules.txt config
    "toggle.rule-deployment.split-decompound-rules-txt" -> true,
    "toggle.rule-deployment.split-decompound-rules-txt-DST_CP_FILE_TO" -> "/deployment-path-live/decompound-rules.txt",
    "smui2solr.deploy-prelive-fn-decompound-txt" -> "/deployment-path-prelive/decompound-rules.txt"
  )

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    createDecompoundRule()
  }

  "RulesTxtDeploymentService" should "provide the (common) rules.txt, replace-rules.txt and a decompound-rules.txt for PRELIVE" in {
    val deploymentDescriptor = service.generateRulesTxtContentWithFilenames(core1Id, "PRELIVE", logDebug = false)

    deploymentDescriptor.solrIndexId shouldBe core1Id
    deploymentDescriptor.regularRules.content should include ("aerosmith") // simply cross check content
    deploymentDescriptor.regularRules.sourceFileName shouldBe "/changed-common-rules-temp-path/search-management-ui_rules-txt.tmp"
    deploymentDescriptor.regularRules.destinationFileName shouldBe "/deployment-path-prelive/common-rules.txt"

    val replaceRules = deploymentDescriptor.replaceRules.get
    replaceRules.content should include ("freezer") // simply cross check content
    replaceRules.sourceFileName shouldBe "/changed-replace-rules-temp-path/search-management-ui_replace-rules-txt.tmp"
    replaceRules.destinationFileName shouldBe "/deployment-path-prelive/replace-rules.txt"

    val decompoundRules = deploymentDescriptor.decompoundRules.get
    decompoundRules.content should include ("damen")
    decompoundRules.sourceFileName shouldBe "/changed-common-rules-temp-path/search-management-ui_rules-txt.tmp-2" // auto generated (by spec)
    decompoundRules.destinationFileName shouldBe "/deployment-path-prelive/decompound-rules.txt"
  }

  "RulesTxtDeploymentService" should "provide the (common) rules.txt, replace-rules.txt and a decompound-rules.txt for LIVE" in {
    val deploymentDescriptor = service.generateRulesTxtContentWithFilenames(core1Id, "LIVE", logDebug = false)

    deploymentDescriptor.solrIndexId shouldBe core1Id
    deploymentDescriptor.regularRules.content should include ("aerosmith") // simply cross check content
    deploymentDescriptor.regularRules.sourceFileName shouldBe "/changed-common-rules-temp-path/search-management-ui_rules-txt.tmp"
    deploymentDescriptor.regularRules.destinationFileName shouldBe "/deployment-path-live/common-rules.txt"

    val replaceRules = deploymentDescriptor.replaceRules.get
    replaceRules.content should include ("freezer") // simply cross check content
    replaceRules.sourceFileName shouldBe "/changed-replace-rules-temp-path/search-management-ui_replace-rules-txt.tmp"
    replaceRules.destinationFileName shouldBe "/deployment-path-live/replace-rules.txt"

    val decompoundRules = deploymentDescriptor.decompoundRules.get
    decompoundRules.content should include ("damen")
    decompoundRules.sourceFileName shouldBe "/changed-common-rules-temp-path/search-management-ui_rules-txt.tmp-2" // auto generated (by spec)
    decompoundRules.destinationFileName shouldBe "/deployment-path-live/decompound-rules.txt"
  }

}

/**
  * Interface with deployment script (for regular and "GIT" target alike)
  */

class RulesTxtDeploymentRegularTargetSpec extends FlatSpec with Matchers with CommonRulesTxtDeploymentServiceConfigVariantsSpecBase {

  override protected lazy val activateSpelling = true

  override protected lazy val additionalAppConfig = Seq(
    // (common) rules.txt config
    "smui2solr.SRC_TMP_FILE" -> "/changed-common-rules-temp-path/search-management-ui_rules-txt.tmp",
    "smui2solr.DST_CP_FILE_TO" -> "/deployment-path-live/common-rules.txt",
    "toggle.rule-deployment.pre-live.present" -> true,
    "smui2solr.deploy-prelive-fn-rules-txt" -> "/deployment-path-prelive/common-rules.txt",
    // replace-rules.txt config
    "smui2solr.replace-rules-tmp-file" -> "/changed-replace-rules-temp-path/search-management-ui_replace-rules-txt.tmp",
    "smui2solr.replace-rules-dst-cp-file-to" -> "/deployment-path-live/replace-rules.txt",
    "smui2solr.deploy-prelive-fn-replace-txt" -> "/deployment-path-prelive/replace-rules.txt",
    // decompound-rules.txt config
    "toggle.rule-deployment.split-decompound-rules-txt" -> true,
    "toggle.rule-deployment.split-decompound-rules-txt-DST_CP_FILE_TO" -> "/deployment-path-live/decompound-rules.txt",
    "smui2solr.deploy-prelive-fn-decompound-txt" -> "/deployment-path-prelive/decompound-rules.txt",
    // Solr host config complete
    "smui2solr.SOLR_HOST" -> "live.solr.instance:8983",
    "smui2solr.deploy-prelive-solr-host" -> "prelive.solr.instance:8983",
    // test script
    "toggle.rule-deployment.custom-script" -> true,
    "toggle.rule-deployment.custom-script-SMUI2SOLR-SH_PATH" -> "test/resources/smui2test.sh"
  )

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    createDecompoundRule()
  }

  "interfaceSmui2SolrSh" should "interface the test script should return all rules.txts for PRELIVE" in {
    val deploymentDescriptor = service.generateRulesTxtContentWithFilenames(core1Id, "PRELIVE", logDebug = false)
    val res = service.executeDeploymentScript(deploymentDescriptor, "PRELIVE")
    res.success shouldBe true
    res.output shouldBe s"""$$1 = >>>/changed-common-rules-temp-path/search-management-ui_rules-txt.tmp
                           |$$2 = >>>/deployment-path-prelive/common-rules.txt
                           |$$3 = >>>prelive.solr.instance:8983
                           |$$4 = >>>core1
                           |$$5 = >>>/deployment-path-prelive/decompound-rules.txt
                           |$$6 = >>>PRELIVE
                           |$$7 = >>>/changed-replace-rules-temp-path/search-management-ui_replace-rules-txt.tmp
                           |$$8 = >>>/deployment-path-prelive/replace-rules.txt
                           |""".stripMargin
  }

  "interfaceSmui2SolrSh" should "interface the test script should return all rules.txts for LIVE" in {
    val deploymentDescriptor = service.generateRulesTxtContentWithFilenames(core1Id, "LIVE", logDebug = false)
    val res = service.executeDeploymentScript(deploymentDescriptor, "LIVE")
    res.success shouldBe true
    res.output shouldBe s"""$$1 = >>>/changed-common-rules-temp-path/search-management-ui_rules-txt.tmp
                           |$$2 = >>>/deployment-path-live/common-rules.txt
                           |$$3 = >>>live.solr.instance:8983
                           |$$4 = >>>core1
                           |$$5 = >>>/deployment-path-live/decompound-rules.txt
                           |$$6 = >>>LIVE
                           |$$7 = >>>/changed-replace-rules-temp-path/search-management-ui_replace-rules-txt.tmp
                           |$$8 = >>>/deployment-path-live/replace-rules.txt
                           |""".stripMargin
  }

}

class RulesTxtDeploymentGitTargetSpec extends FlatSpec with Matchers with CommonRulesTxtDeploymentServiceConfigVariantsSpecBase {

  override protected lazy val activateSpelling = true

  override protected lazy val additionalAppConfig = Seq(
    // switch to GIT for LIVE deployment
    "smui2solr.DST_CP_FILE_TO" -> "GIT",
    "smui.deployment.git.repo-url" -> "ssh://git@changed-git-server.tld/repos/smui_rulestxt_repo.git",
    "smui2solr.deployment.git.filename.common-rules-txt" -> "common-rules.txt",
    // (common) rules.txt config
    "smui2solr.SRC_TMP_FILE" -> "/changed-common-rules-temp-path/search-management-ui_rules-txt.tmp",
    "toggle.rule-deployment.pre-live.present" -> true,
    "smui2solr.deploy-prelive-fn-rules-txt" -> "/deployment-path-prelive/common-rules.txt",
    // replace-rules.txt config
    "smui2solr.replace-rules-tmp-file" -> "/changed-replace-rules-temp-path/search-management-ui_replace-rules-txt.tmp",
    "smui2solr.replace-rules-dst-cp-file-to" -> "/deployment-path-live/replace-rules.txt",
    "smui2solr.deploy-prelive-fn-replace-txt" -> "/deployment-path-prelive/replace-rules.txt",
    // decompound-rules.txt config
    "toggle.rule-deployment.split-decompound-rules-txt" -> true,
    "toggle.rule-deployment.split-decompound-rules-txt-DST_CP_FILE_TO" -> "/deployment-path-live/decompound-rules.txt",
    "smui2solr.deploy-prelive-fn-decompound-txt" -> "/deployment-path-prelive/decompound-rules.txt",
    // Solr host config complete
    "smui2solr.SOLR_HOST" -> "live.solr.instance:8983",
    "smui2solr.deploy-prelive-solr-host" -> "prelive.solr.instance:8983"
  )

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    createDecompoundRule()
  }

  // TODO think about make the interface to smui2git.sh also interchangable (like with smui2solr.sh) to inject a echoing test script
  // TODO think about bootstrapping a local git server (docker) within test, to test the whole roundtrip

  "interfaceSmui2GitSh" should "interface the test script should return all rules.txts for LIVE" in {

    val deploymentDescriptor = service.generateRulesTxtContentWithFilenames(core1Id, "LIVE", logDebug = false)
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

    val deploymentDescriptor = service.generateRulesTxtContentWithFilenames(core1Id, "PRELIVE", logDebug = false)
    val res = service.executeDeploymentScript(deploymentDescriptor, "PRELIVE")

    // TODO script result itself failed - this is cheesy, but cristal clear, as we don't have a local git server running or a test script instead

    res.output should include ("/deployment-path-prelive/common-rules.txt")
    res.output should include ("/deployment-path-prelive/replace-rules.txt")
    res.output should include ("/deployment-path-prelive/decompound-rules.txt")

    // TODO As of v3.11.7 there is no option:
    // TODO ... to deploy to different git hosts / repos / branches (it all makes sense)
    // TODO ... to deploy further rules.txts (like replace-rules.txt, decompound-rules.txt)

  }


}
