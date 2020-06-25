package models
import java.io.OutputStream
import java.util.zip.{ZipEntry, ZipOutputStream}

import javax.inject.Inject
import models.FeatureToggleModel.FeatureToggleService
import play.api.{Configuration, Environment, Logging}

import sys.process._

@javax.inject.Singleton
class RulesTxtDeploymentService @Inject() (querqyRulesTxtGenerator: QuerqyRulesTxtGenerator,
                                           appConfig: Configuration,
                                           featureToggleService: FeatureToggleService,
                                           searchManagementRepository: SearchManagementRepository,
                                           environment: Environment) extends Logging {

  case class RulesTxtsForSolrIndex(solrIndexId: SolrIndexId,
                                   regularRules: RulesTxtWithFileNames,
                                   decompoundRules: Option[RulesTxtWithFileNames],
                                   replaceRules: Option[RulesTxtWithFileNames]) {

    def regularAndDecompoundFiles: List[RulesTxtWithFileNames] = List(regularRules) ++ decompoundRules

    def allFiles: List[RulesTxtWithFileNames] = regularAndDecompoundFiles ++ replaceRules

  }

  case class RulesTxtWithFileNames(content: String,
                                   sourceFileName: String,
                                   destinationFileName: String)

  /**
    * Generates a list of source to destination filenames containing the rules.txt(s) according to current application settings.
    *
    * @param solrIndexId Solr Index Id to generate the output for.
    */
  // TODO evaluate, if logDebug should be used to prevent verbose logging of the whole generated rules.txt (for zip download especially)
  def generateRulesTxtContentWithFilenames(solrIndexId: SolrIndexId, targetSystem: String, logDebug: Boolean = true): RulesTxtsForSolrIndex = {

    // SMUI config for (regular) LIVE deployment
    val SRC_TMP_FILE = appConfig.get[String]("smui2solr.SRC_TMP_FILE")
    val DST_CP_FILE_TO = appConfig.get[String]("smui2solr.DST_CP_FILE_TO")
    val DO_SPLIT_DECOMPOUND_RULES_TXT = featureToggleService.getToggleRuleDeploymentSplitDecompoundRulesTxt
    val DECOMPOUND_RULES_TXT_DST_CP_FILE_TO = featureToggleService.getToggleRuleDeploymentSplitDecompoundRulesTxtDstCpFileTo
    // (additional) SMUI config for PRELIVE deployment
    val SMUI_DEPLOY_PRELIVE_FN_RULES_TXT = appConfig.get[String]("smui2solr.deploy-prelive-fn-rules-txt")
    val SMUI_DEPLOY_PRELIVE_FN_DECOMPOUND_TXT = appConfig.get[String]("smui2solr.deploy-prelive-fn-decompound-txt")

    // Replace rules (spelling)
    val EXPORT_REPLACE_RULES = featureToggleService.getToggleReplaceRuleDeployment
    val REPLACE_RULES_SRC_TMP_FILE = appConfig.get[String]("smui2solr.REPLACE_RULES_TMP_FILE")
    val REPLACE_RULES_DST_CP_FILE_TO = appConfig.get[String]("smui2solr.REPLACE_RULES_DST_CP_FILE_TO")
    val SMUI_DEPLOY_PRELIVE_FN_REPLACE_TXT = appConfig.get[String]("smui2solr.deploy-prelive-fn-replace-txt")

    if (logDebug) {
      logger.debug(
        s""":: generateRulesTxtContentWithFilenames config
           |:: SRC_TMP_FILE = $SRC_TMP_FILE
           |:: DST_CP_FILE_TO = $DST_CP_FILE_TO
           |:: DO_SPLIT_DECOMPOUND_RULES_TXT = $DO_SPLIT_DECOMPOUND_RULES_TXT
           |:: DECOMPOUND_RULES_TXT_DST_CP_FILE_TO = $DECOMPOUND_RULES_TXT_DST_CP_FILE_TO
           |:: SMUI_DEPLOY_PRELIVE_FN_RULES_TXT = $SMUI_DEPLOY_PRELIVE_FN_RULES_TXT
           |:: SMUI_DEPLOY_PRELIVE_FN_DECOMPOUND_TXT = $SMUI_DEPLOY_PRELIVE_FN_DECOMPOUND_TXT
           |:: EXPORT_REPLACE_RULES = $EXPORT_REPLACE_RULES
           |:: REPLACE_RULES_SRC_TMP_FILE = $REPLACE_RULES_SRC_TMP_FILE
           |:: REPLACE_RULES_DST_CP_FILE_TO = $REPLACE_RULES_DST_CP_FILE_TO
           |:: SMUI_DEPLOY_PRELIVE_FN_REPLACE_TXT = $SMUI_DEPLOY_PRELIVE_FN_REPLACE_TXT
      """.stripMargin)
    }

    // generate one rules.txt by default or two separated, if decompound instructions are supposed to be split

    // TODO test correct generation in different scenarios (one vs. two rules.txts, etc.)
    val dstCpFileTo = if (targetSystem == "PRELIVE")
      SMUI_DEPLOY_PRELIVE_FN_REPLACE_TXT
    else // targetSystem == "LIVE"
      DST_CP_FILE_TO

    val replaceRulesDstCpFileTo =
      if (targetSystem == "PRELIVE") SMUI_DEPLOY_PRELIVE_FN_RULES_TXT
      else REPLACE_RULES_DST_CP_FILE_TO

    val replaceRules =
      if (EXPORT_REPLACE_RULES) {
        Some(RulesTxtWithFileNames(
          querqyRulesTxtGenerator.renderReplaceRules(solrIndexId), REPLACE_RULES_SRC_TMP_FILE, replaceRulesDstCpFileTo
        ))
      } else None

    if (!DO_SPLIT_DECOMPOUND_RULES_TXT) {
      RulesTxtsForSolrIndex(solrIndexId,
        RulesTxtWithFileNames(querqyRulesTxtGenerator.renderSingleRulesTxt(solrIndexId), SRC_TMP_FILE, dstCpFileTo),
        None,
        replaceRules
      )
    } else {
      val decompoundDstCpFileTo = if (targetSystem == "PRELIVE")
        SMUI_DEPLOY_PRELIVE_FN_DECOMPOUND_TXT
      else // targetSystem == "LIVE"
        DECOMPOUND_RULES_TXT_DST_CP_FILE_TO
      RulesTxtsForSolrIndex(solrIndexId,
        RulesTxtWithFileNames(querqyRulesTxtGenerator.renderSeparatedRulesTxts(solrIndexId, renderCompoundsRulesTxt = false), SRC_TMP_FILE, dstCpFileTo),
        Some(RulesTxtWithFileNames(querqyRulesTxtGenerator.renderSeparatedRulesTxts(solrIndexId, renderCompoundsRulesTxt = true),
          SRC_TMP_FILE + "-2", decompoundDstCpFileTo)
        ),
        replaceRules
      )
    }
  }

  /**
    * Returns errors for the given rules files.
    * There are no errors if the list is empty.
    */
  def validateCompleteRulesTxts(rulesTxts: RulesTxtsForSolrIndex, logDebug: Boolean = true): List[String] = {
    val rulesValidation = rulesTxts.regularAndDecompoundFiles.flatMap { rulesFile =>
      if (logDebug) {
        logger.debug(":: validateCompleteRulesTxts for src = " + rulesFile.sourceFileName + " dst = " + rulesFile.destinationFileName)
        logger.debug(":: rulesTxt = <<<" + rulesFile.content + ">>>")
      }
      val validationResult = querqyRulesTxtGenerator.validateQuerqyRulesTxtToErrMsg(rulesFile.content)
      validationResult.foreach { strErrMsg =>
        logger.warn(":: validation failed with message = " + strErrMsg)
      }
      validationResult
    }

    val replaceRulesValidation = rulesTxts.replaceRules.flatMap { replaceRules =>
      querqyRulesTxtGenerator.validateQuerqyReplaceRulesTxtToErrMsg(replaceRules.content)
    }.toSeq

    rulesValidation ++ replaceRulesValidation
  }

  def executeDeploymentScript(rulesTxts: RulesTxtsForSolrIndex, targetSystem: String): DeploymentScriptResult = {

    // interface to smui2solr.sh
    def interfaceDeploymentScript(scriptPath: String, srcTmpFile: String, dstCpFileTo: String, solrHost: String, solrCoreName: String, decompoundDstCpFileTo: String, targetSystem: String): DeploymentScriptResult = {
      // TODO perform file copying and solr core reload directly in the application (without any shell dependency)
      logger.info(
        s""":: executeDeploymentScript config
           |:: scriptPath = $scriptPath
           |:: srcTmpFile = $srcTmpFile
           |:: dstCpFileTo = $dstCpFileTo
           |:: solrHost = $solrHost
           |:: solrCoreName = $solrCoreName
           |:: decompoundDstCpFileTo = $decompoundDstCpFileTo
           |:: targetSystem = $targetSystem
      """.stripMargin)

      val output = new StringBuilder()
      val processLogger = ProcessLogger(line => output.append(line + "\n"))
      // define call and add parameters to the script (in expected order, see smui2solr.sh)
      val scriptCall =
        scriptPath + " " +
        // SRC_TMP_FILE=$1
        srcTmpFile + " " +
        // DST_CP_FILE_TO=$2
        dstCpFileTo + " " +
        // SOLR_HOST=$3
        solrHost + " " +
        // SOLR_CORE_NAME=$4
        solrCoreName + " " +
        // DECOMPOUND_DST_CP_FILE_TO=$5
        decompoundDstCpFileTo + " " +
        // TARGET_SYSTEM=$6
        targetSystem
      // call
      val exitCode = scriptCall.!(processLogger)
      DeploymentScriptResult(exitCode, output.toString())
    }

    // determine script
    val DO_CUSTOM_SCRIPT_SMUI2SOLR_SH = featureToggleService.getToggleRuleDeploymentCustomScript
    val CUSTOM_SCRIPT_SMUI2SOLR_SH_PATH = featureToggleService.getToggleRuleDeploymentCustomScriptSmui2solrShPath
    val scriptPath = if (DO_CUSTOM_SCRIPT_SMUI2SOLR_SH)
      CUSTOM_SCRIPT_SMUI2SOLR_SH_PATH
    else
      environment.rootPath.getAbsolutePath + "/conf/smui2solr.sh"

    val srcTmpFile = rulesTxts.regularRules.sourceFileName
    val dstCpFileTo = rulesTxts.regularRules.destinationFileName
    val decompoundDstCpFileTo = if (rulesTxts.decompoundRules.isDefined)
      rulesTxts.decompoundRules.get.destinationFileName
    else
      "NONE"

    // host for (optional) core reload
    val SMUI_DEPLOY_PRELIVE_SOLR_HOST = appConfig.get[String]("smui2solr.deploy-prelive-solr-host")
    val SMUI_DEPLOY_LIVE_SOLR_HOST = appConfig.get[String]("smui2solr.SOLR_HOST")
    val solrHost = if (targetSystem == "PRELIVE")
      if (SMUI_DEPLOY_PRELIVE_SOLR_HOST.isEmpty)
        "NONE"
      else
        SMUI_DEPLOY_PRELIVE_SOLR_HOST
    else // targetSystem == "LIVE"
      if (SMUI_DEPLOY_LIVE_SOLR_HOST.isEmpty)
        "NONE"
      else
        SMUI_DEPLOY_LIVE_SOLR_HOST
    // core name from repo (optional, for core reload as well)
    val solrCoreName = searchManagementRepository.getSolrIndexName(rulesTxts.solrIndexId)

    // execute script
    val result = interfaceDeploymentScript(
      scriptPath,
      srcTmpFile,
      dstCpFileTo,
      solrHost,
      solrCoreName,
      decompoundDstCpFileTo,
      targetSystem
    )
    if (result.success) {
      logger.info(s"Rules.txt deployment successful:\n${result.output}")
    } else {
      logger.warn(s"Rules.txt deployment failed with exit code ${result.exitCode}:\n${result.output}")
    }
    result
  }

  def writeRulesTxtTempFiles(rulesTxts: RulesTxtsForSolrIndex): Unit = {

    // helper to write rules.txt output to to temp file
    def writeRulesTxtToTempFile(strRulesTxt: String, tmpFilePath: String): Unit = {
      val tmpFile = new java.io.File(tmpFilePath)
      tmpFile.createNewFile()
      val fw = new java.io.FileWriter(tmpFile)
      try {
        fw.write(strRulesTxt)
      }
      catch {
        case iox: java.io.IOException => logger.error("IOException while writing /tmp file: " + iox.getStackTrace)
        case _: Throwable => logger.error("Got an unexpected error while writing /tmp file")
      }
      finally {
        fw.close()
      }
    }

    // write the temp file(s)
    rulesTxts.allFiles.foreach { file =>
      writeRulesTxtToTempFile(file.content, file.sourceFileName)
    }
  }

  def writeAllRulesTxtFilesAsZipFileToStream(out: OutputStream): Unit = {
    val zipStream = new ZipOutputStream(out)
    try {
      for (index <- searchManagementRepository.listAllSolrIndexes) {
        // TODO make targetSystem configurable from ApiController.downloadAllRulesTxtFiles ... go with "LIVE" from now (as there exist no different revisions of the search management content)!
        val rules = generateRulesTxtContentWithFilenames(index.id, "LIVE", logDebug = false)
        zipStream.putNextEntry(new ZipEntry(s"rules_${index.name}.txt"))
        zipStream.write(rules.regularRules.content.getBytes("UTF-8"))
        zipStream.closeEntry()

        for (decompoundRules <- rules.decompoundRules) {
          zipStream.putNextEntry(new ZipEntry(s"rules-decompounding_${index.name}.txt"))
          zipStream.write(decompoundRules.content.getBytes("UTF-8"))
          zipStream.closeEntry()
        }

        for (replaceRules <- rules.replaceRules) {
          zipStream.putNextEntry(new ZipEntry(s"replace-rules_${index.name}.txt"))
          zipStream.write(replaceRules.content.getBytes("UTF-8"))
          zipStream.closeEntry()
        }
      }
    } finally {
      logger.debug("Wrote all rules.txt files to a zip stream")
      zipStream.close()
      out.close()
    }
  }

}
