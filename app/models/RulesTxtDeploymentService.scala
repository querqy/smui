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
                                   decompoundRules: Option[RulesTxtWithFileNames]) {

    def allRulesFiles: List[RulesTxtWithFileNames] = List(regularRules) ++ decompoundRules

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
  def generateRulesTxtContentWithFilenames(solrIndexId: SolrIndexId, logDebug: Boolean = true): RulesTxtsForSolrIndex = {

    val SRC_TMP_FILE = appConfig.get[String]("smui2solr.SRC_TMP_FILE")
    val DST_CP_FILE_TO = appConfig.get[String]("smui2solr.DST_CP_FILE_TO")
    val DO_SPLIT_DECOMPOUND_RULES_TXT = featureToggleService.getToggleRuleDeploymentSplitDecompoundRulesTxt
    val DECOMPOUND_RULES_TXT_DST_CP_FILE_TO = featureToggleService.getToggleRuleDeploymentSplitDecompoundRulesTxtDstCpFileTo

    if (logDebug) {
      logger.debug(
        s""":: generateRulesTxtContentWithFilenames config
           |:: SRC_TMP_FILE = $SRC_TMP_FILE
           |:: DST_CP_FILE_TO = $DST_CP_FILE_TO
           |:: DO_SPLIT_DECOMPOUND_RULES_TXT = $DO_SPLIT_DECOMPOUND_RULES_TXT
           |:: DECOMPOUND_RULES_TXT_DST_CP_FILE_TO = $DECOMPOUND_RULES_TXT_DST_CP_FILE_TO
      """.stripMargin)
    }

    // generate one rules.txt by default or two separated, if decompound instructions are supposed to be split

    // TODO test correct generation in different scenarios (one vs. two rules.txts, etc.)
    if (!DO_SPLIT_DECOMPOUND_RULES_TXT) {
      RulesTxtsForSolrIndex(solrIndexId,
        RulesTxtWithFileNames(querqyRulesTxtGenerator.renderSingleRulesTxt(solrIndexId), SRC_TMP_FILE, DST_CP_FILE_TO), None)
    } else {
      RulesTxtsForSolrIndex(solrIndexId,
        RulesTxtWithFileNames(querqyRulesTxtGenerator.renderSeparatedRulesTxts(solrIndexId, renderCompoundsRulesTxt = false), SRC_TMP_FILE, DST_CP_FILE_TO),
        Some(RulesTxtWithFileNames(querqyRulesTxtGenerator.renderSeparatedRulesTxts(solrIndexId, renderCompoundsRulesTxt = true),
          SRC_TMP_FILE + "-2", DECOMPOUND_RULES_TXT_DST_CP_FILE_TO)))
    }
  }

  /**
    * Returns errors for the given rules files.
    * There are no errors if the list is empty.
    */
  def validateCompleteRulesTxts(rulesTxts: RulesTxtsForSolrIndex, logDebug: Boolean = true): List[String] = {
    rulesTxts.allRulesFiles.flatMap { rulesFile =>
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
  }

  def executeDeploymentScript(rulesTxts: RulesTxtsForSolrIndex, targetSystem: String): Int = {

    val SOLR_HOST = appConfig.get[String]("smui2solr.SOLR_HOST")
    val SOLR_CORE_NAME = searchManagementRepository.getSolrIndexName(rulesTxts.solrIndexId)
    val DO_CUSTOM_SCRIPT_SMUI2SOLR_SH = featureToggleService.getToggleRuleDeploymentCustomScript
    val CUSTOM_SCRIPT_SMUI2SOLR_SH_PATH = featureToggleService.getToggleRuleDeploymentCustomScriptSmui2solrShPath

    logger.info(
      s""":: executeDeploymentScript config
         |:: targetSystem = $targetSystem
         |:: SOLR_HOST = $SOLR_HOST
         |:: SOLR_CORE_NAME = $SOLR_CORE_NAME
         |:: DO_CUSTOM_SCRIPT_SMUI2SOLR_SH = $DO_CUSTOM_SCRIPT_SMUI2SOLR_SH
         |:: CUSTOM_SCRIPT_SMUI2SOLR_SH_PATH = $CUSTOM_SCRIPT_SMUI2SOLR_SH_PATH
    """.stripMargin)

    val scriptPath = if (DO_CUSTOM_SCRIPT_SMUI2SOLR_SH) CUSTOM_SCRIPT_SMUI2SOLR_SH_PATH else environment.rootPath.getAbsolutePath + "/conf/smui2solr.sh"
    val scriptCall =
        scriptPath + " " +
        // add parameters to the script (in expected order, see smui2solr.sh)
        rulesTxts.regularRules.sourceFileName + " " + // smui2solr.sh param $1 - SRC_TMP_FILE
        rulesTxts.regularRules.destinationFileName + " " + // smui2solr.sh param $2 - DST_CP_FILE_TO
        SOLR_HOST + " " + // smui2solr.sh param $3 - SOLR_HOST
        SOLR_CORE_NAME + " " + // smui2solr.sh param $4 - SOLR_CORE_NAME
        (if (rulesTxts.decompoundRules.isDefined) rulesTxts.decompoundRules.get.destinationFileName else "NONE") + " " + // smui2solr.sh param $5 - DECOMPOUND_DST_CP_FILE_TO
        targetSystem // smui2solr.sh param $6 - TARGET_SYSTEM
    val result = scriptCall.! // TODO perform file copying and solr core reload directly in the application (without any shell dependency)
    logger.info(":: executeDeploymentScript :: Script execution result: " + result)
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
    rulesTxts.allRulesFiles.foreach { file =>
      writeRulesTxtToTempFile(file.content, file.sourceFileName)
    }
  }

  def writeAllRulesTxtFilesAsZipFileToStream(out: OutputStream): Unit = {
    val zipStream = new ZipOutputStream(out)
    try {
      for (index <- searchManagementRepository.listAllSolrIndexes) {
        val rules = generateRulesTxtContentWithFilenames(index.id, logDebug = false)
        zipStream.putNextEntry(new ZipEntry(s"rules_${index.name}.txt"))
        zipStream.write(rules.regularRules.content.getBytes("UTF-8"))
        zipStream.closeEntry()

        for (decompoundRules <- rules.decompoundRules) {
          zipStream.putNextEntry(new ZipEntry(s"rules-decompounding_${index.name}.txt"))
          zipStream.write(decompoundRules.content.getBytes("UTF-8"))
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
