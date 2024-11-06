package services

import java.io.OutputStream
import java.util.zip.{ZipEntry, ZipOutputStream}

import javax.inject.Inject
import models.FeatureToggleModel.FeatureToggleService
import models.querqy.{QuerqyReplaceRulesGenerator, QuerqyRulesTxtGenerator}
import models.{DeploymentScriptResult, SearchManagementRepository, SolrIndexId}
import play.api.{Configuration, Environment, Logging}

import scala.sys.process._

// TODO consider moving this to a service (instead of models) package
@javax.inject.Singleton
class RulesTxtDeploymentService @Inject() (querqyRulesTxtGenerator: QuerqyRulesTxtGenerator,
                                           appConfig: Configuration,
                                           featureToggleService: FeatureToggleService,
                                           searchManagementRepository: SearchManagementRepository,
                                           environment: Environment) extends Logging {

  case class RulesTxtsForSolrIndex(solrIndexId: SolrIndexId,
                                   regularRules: RulesTxtWithFileName,
                                   decompoundRules: Option[RulesTxtWithFileName],
                                   replaceRules: Option[RulesTxtWithFileName]) {

    def regularAndDecompoundFiles: List[RulesTxtWithFileName] = List(regularRules) ++ decompoundRules

    def allFiles: List[RulesTxtWithFileName] = regularAndDecompoundFiles ++ replaceRules

  }

  case class RulesTxtWithFileName(content: String,
                                  sourceFileName: String)

  /**
    * Generates a list of source to destination filenames containing the rules.txt(s) according to current application settings.
    *
    * @param solrIndexId Solr Index Id to generate the output for.
    */
  // TODO evaluate, if logDebug should be used to prevent verbose logging of the whole generated rules.txt (for zip download especially)
  def generateRulesTxtContentWithFilenames(solrIndexId: SolrIndexId, logDebug: Boolean = true): RulesTxtsForSolrIndex = {

    val SRC_TMP_FILE = appConfig.get[String]("smui2solr.SRC_TMP_FILE")
    val DO_SPLIT_DECOMPOUND_RULES_TXT = featureToggleService.getToggleRuleDeploymentSplitDecompoundRulesTxt
    val DO_EXPORT_REPLACE_RULES = featureToggleService.getToggleActivateSpelling
    val REPLACE_RULES_SRC_TMP_FILE = appConfig.get[String]("smui2solr.replace-rules-tmp-file")

    if (logDebug) {
      logger.debug(
        s""":: generateRulesTxtContentWithFilenames config
           |:: SRC_TMP_FILE = $SRC_TMP_FILE
           |:: DO_SPLIT_DECOMPOUND_RULES_TXT = $DO_SPLIT_DECOMPOUND_RULES_TXT
           |:: EXPORT_REPLACE_RULES = $DO_EXPORT_REPLACE_RULES
           |:: REPLACE_RULES_SRC_TMP_FILE = $REPLACE_RULES_SRC_TMP_FILE
      """.stripMargin)
    }

    // generate one rules.txt by default or two separated, if decompound instructions are supposed to be split

    val optReplaceRules =
      if (DO_EXPORT_REPLACE_RULES) {
        val allCanonicalSpellings = searchManagementRepository.listAllSpellingsWithAlternatives(solrIndexId)
        val spellingsRendered =  QuerqyReplaceRulesGenerator.renderAllCanonicalSpellingsToReplaceRules(allCanonicalSpellings)
        Some(RulesTxtWithFileName(spellingsRendered, REPLACE_RULES_SRC_TMP_FILE))
      } else None

    if (!DO_SPLIT_DECOMPOUND_RULES_TXT) {
      RulesTxtsForSolrIndex(solrIndexId,
        RulesTxtWithFileName(querqyRulesTxtGenerator.renderSingleRulesTxt(solrIndexId), SRC_TMP_FILE),
        None,
        optReplaceRules
      )
    } else {
      val rulesWithoutDecompounds = querqyRulesTxtGenerator.renderSeparatedRulesTxts(solrIndexId, renderCompoundsRulesTxt = false)
      val decompoundRules = querqyRulesTxtGenerator.renderSeparatedRulesTxts(solrIndexId, renderCompoundsRulesTxt = true)
      RulesTxtsForSolrIndex(solrIndexId,
        RulesTxtWithFileName(rulesWithoutDecompounds, SRC_TMP_FILE),
        Some(RulesTxtWithFileName(decompoundRules, SRC_TMP_FILE + "-2")),
        optReplaceRules
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
        logger.debug(":: validateCompleteRulesTxts for src = " + rulesFile.sourceFileName)
        logger.debug(":: rulesTxt = <<<" + rulesFile.content + ">>>")
      }
      val validationResult = querqyRulesTxtGenerator.validateQuerqyRulesTxtToErrMsg(rulesFile.content)
      validationResult.foreach { strErrMsg =>
        logger.warn(":: validation failed with message = " + strErrMsg)
      }
      validationResult
    }

    val replaceRulesValidation = rulesTxts.replaceRules.flatMap { replaceRules =>
      QuerqyReplaceRulesGenerator.validateQuerqyReplaceRulesTxtToErrMsg(replaceRules.content)
    }.toSeq

    rulesValidation ++ replaceRulesValidation
  }

  def executeDeploymentScript(rulesTxts: RulesTxtsForSolrIndex, targetSystem: String): DeploymentScriptResult = {

    /**
      * Interface to smui2solr.sh (or smui2git.sh)
      */
    // TODO perform file copying and solr core reload directly in the application (without any shell dependency)

    def interfaceDeploymentScript(scriptCall: String): DeploymentScriptResult = {

      val output = new StringBuilder()
      val processLogger = ProcessLogger(line => output.append(line + "\n"))

      // call
      val exitCode = scriptCall.!(processLogger)
      DeploymentScriptResult(exitCode, output.toString())

    }

    def interfaceSmui2SolrSh(scriptPath: String,
                             srcTmpFile: String,
                             replaceRulesSrcTmpFile: String,
                             targetSystem: String,
                             solrCoreName: String
                            ): DeploymentScriptResult = {

      logger.info(
        s""":: interfaceSmui2SolrSh
           |:: scriptPath = $scriptPath
           |:: srcTmpFile = $srcTmpFile
           |:: replaceRulesSrcTmpFile = $replaceRulesSrcTmpFile
           |:: solrCoreName = $solrCoreName
           |:: targetSystem = $targetSystem
      """.stripMargin)

      val scriptCall = List(
        // define call for regular smui2solr (default or custom script) and add parameters to the script (in expected order, see smui2solr.sh)
        scriptPath,
        // SRC_TMP_FILE=$1
        srcTmpFile,
        // REPLACE_RULES_SRC_TMP_FILE=$2
        replaceRulesSrcTmpFile,
        // TARGET_SYSTEM=$3
        targetSystem,
        // SOLR_CORE_NAME=$4
        solrCoreName
      ).mkString(" ")

      interfaceDeploymentScript(scriptCall)
    }

    def interfaceSmui2GitSh(scriptPath: String, srcTmpFile: String,
                            repoUrl: String, fnCommonRulesTxt: String
                           ): DeploymentScriptResult = {
      val scriptCall =
        // define call for default smui2git.sh script
        scriptPath + " " +
        // SRC_TMP_FILE=$1
        srcTmpFile + " " +
        // SMUI_GIT_REPOSITORY=$2
        repoUrl + " " +
        // SMUI_GIT_FN_COMMON_RULES_TXT=$3
        fnCommonRulesTxt

      val firstResult = interfaceDeploymentScript(scriptCall)
      if(!firstResult.success) {
        // still accept, if no change happened
        if(firstResult.output.trim.endsWith("nothing to commit, working tree clean")) {
          DeploymentScriptResult(0, firstResult.output)
        } else {
          firstResult
        }
      } else {
        firstResult
      }
    }

    // determine script
    val DO_CUSTOM_SCRIPT_SMUI2SOLR_SH = featureToggleService.getToggleRuleDeploymentCustomScript
    val CUSTOM_SCRIPT_SMUI2SOLR_SH_PATH = featureToggleService.getToggleRuleDeploymentCustomScriptSmui2solrShPath
    val scriptPath = if (DO_CUSTOM_SCRIPT_SMUI2SOLR_SH)
      CUSTOM_SCRIPT_SMUI2SOLR_SH_PATH
    else
      environment.rootPath.getAbsolutePath + "/conf/smui2solr.sh"

    val deployToGitConfigured = featureToggleService.getToggleRuleDeploymentGitEnabled

    val srcTmpFile = rulesTxts.regularRules.sourceFileName
    val solrCoreName = searchManagementRepository.getSolrIndexName(rulesTxts.solrIndexId)
    val replaceRulesSrcTmpFile = rulesTxts.replaceRules.map(_.sourceFileName).getOrElse("NONE")

    // execute script
    // TODO currently only git deployment for LIVE instance available
    val result = if (deployToGitConfigured) {
      if (targetSystem == "LIVE") {
        logger.info(":: executeDeploymentScript :: GIT configured calling interfaceSmui2GitSh")
        // TODO support further rule files (decompound / replace) and maybe solrCoreName and/or targetSystem for git branch?
        interfaceSmui2GitSh(
          environment.rootPath.getAbsolutePath + "/conf/smui2git.sh",
          srcTmpFile,
          featureToggleService.getSmuiDeploymentGitRepoUrl,
          featureToggleService.getSmuiDeploymentGitFilenameCommonRulesTxt,
        )
      } else {
        DeploymentScriptResult(exitCode = -1, output = "Git deployment only support for LIVE target")
      }
    } else {
      logger.info(s":: executeDeploymentScript :: regular script configured calling interfaceSmui2SolrSh(scriptPath = '$scriptPath')")
      interfaceSmui2SolrSh(
        scriptPath,
        srcTmpFile,
        replaceRulesSrcTmpFile,
        targetSystem,
        solrCoreName
      )
    }
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
        val rules = generateRulesTxtContentWithFilenames(index.id, logDebug = false)
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
