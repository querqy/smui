package services

import models.FeatureToggleModel.FeatureToggleService
import models.input.InputTag
import models.querqy.{QuerqyReplaceRulesGenerator, QuerqyRulesTxtGenerator}
import models.{DeploymentScriptResult, SearchManagementRepository, SolrIndexId}
import play.api.{Configuration, Environment, Logging}

import java.io.OutputStream
import java.util.zip.{ZipEntry, ZipOutputStream}
import javax.inject.Inject
import scala.collection.mutable.ListBuffer
import scala.sys.process._

// TODO consider moving this to a service (instead of models) package
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

  // for backward compatibility: returns first element from generateRulesTxtContentWithFilenamesList(...)
  def generateRulesTxtContentWithFilenames(solrIndexId: SolrIndexId, targetSystem: String, logDebug: Boolean = true): RulesTxtsForSolrIndex = {
    generateRulesTxtContentWithFilenamesList(solrIndexId, targetSystem, logDebug).head
  }

  // generate list of files per intputTag value (including the common set without the tag property)
  def generateRulesTxtContentWithFilenamesList(solrIndexId: SolrIndexId, targetSystem: String, logDebug: Boolean = true): List[RulesTxtsForSolrIndex] = {
    val filterInputTagProperty = appConfig.get[String]("smui2solr.deployment.per.inputtag.property")
    // build rules list based on the filter
    val rulesTxtsForSolrIndexes: ListBuffer[RulesTxtsForSolrIndex] = new ListBuffer[RulesTxtsForSolrIndex]
    // 1st: common rules without the inputTag assigned
    rulesTxtsForSolrIndexes += generateRulesTxtContentWithFilenamesPerInputTag(solrIndexId, targetSystem, true, filterInputTagProperty, null)
    // 2nd: rules per inputTag values
    if (!filterInputTagProperty.isEmpty) {
      for (currentInputTag <- searchManagementRepository.listAllInputTagValuesForInputTagProperty(solrIndexId, filterInputTagProperty)) {
        rulesTxtsForSolrIndexes += generateRulesTxtContentWithFilenamesPerInputTag(solrIndexId, targetSystem, true, filterInputTagProperty, currentInputTag)
      }
    }
    rulesTxtsForSolrIndexes.toList
  }

  /**
    * Generates a list of source to destination filenames containing the rules.txt(s) according to current application settings.
    *
    * @param solrIndexId Solr Index Id to generate the output for.
    */
  // TODO evaluate, if logDebug should be used to prevent verbose logging of the whole generated rules.txt (for zip download especially)
  def generateRulesTxtContentWithFilenamesPerInputTag(solrIndexId: SolrIndexId, targetSystem: String, logDebug: Boolean = true, filterInputTagProperty: String, currentInputTag: InputTag): RulesTxtsForSolrIndex = {
    val inputTagValue:String = if (currentInputTag != null)
      currentInputTag.value
    else
      ""
    // SMUI config for (regular) LIVE deployment
    val SRC_TMP_FILE = appendTagToFileName(appConfig.get[String]("smui2solr.SRC_TMP_FILE"), inputTagValue)
    val DST_CP_FILE_TO = appendTagToFileName(appConfig.get[String]("smui2solr.DST_CP_FILE_TO"), inputTagValue)
    val DO_SPLIT_DECOMPOUND_RULES_TXT = featureToggleService.getToggleRuleDeploymentSplitDecompoundRulesTxt
    val DECOMPOUND_RULES_TXT_DST_CP_FILE_TO = featureToggleService.getToggleRuleDeploymentSplitDecompoundRulesTxtDstCpFileTo
    // (additional) SMUI config for PRELIVE deployment
    val SMUI_DEPLOY_PRELIVE_FN_RULES_TXT = appendTagToFileName(appConfig.get[String]("smui2solr.deploy-prelive-fn-rules-txt"), inputTagValue)
    val SMUI_DEPLOY_PRELIVE_FN_DECOMPOUND_TXT = appendTagToFileName(appConfig.get[String]("smui2solr.deploy-prelive-fn-decompound-txt"),inputTagValue)

    // Replace rules (spelling)
    val EXPORT_REPLACE_RULES = featureToggleService.getToggleActivateSpelling
    val REPLACE_RULES_SRC_TMP_FILE = appConfig.get[String]("smui2solr.replace-rules-tmp-file")
    val REPLACE_RULES_DST_CP_FILE_TO = appConfig.get[String]("smui2solr.replace-rules-dst-cp-file-to")
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
      SMUI_DEPLOY_PRELIVE_FN_RULES_TXT
    else // targetSystem == "LIVE"
      DST_CP_FILE_TO

    val replaceRulesDstCpFileTo =
      if (targetSystem == "PRELIVE") SMUI_DEPLOY_PRELIVE_FN_REPLACE_TXT
      else REPLACE_RULES_DST_CP_FILE_TO

    val sourceTempFile = SRC_TMP_FILE

    val replaceRules = {
      // exclude iterations for inputtag value != null as replace rules have no inputtags
      if (EXPORT_REPLACE_RULES && currentInputTag == null) {
        val allCanonicalSpellings = searchManagementRepository.listAllSpellingsWithAlternatives(solrIndexId)
        Some(RulesTxtWithFileNames(
          QuerqyReplaceRulesGenerator.renderAllCanonicalSpellingsToReplaceRules(allCanonicalSpellings),
          REPLACE_RULES_SRC_TMP_FILE,
          replaceRulesDstCpFileTo
        ))
      } else None
    }

    if (!DO_SPLIT_DECOMPOUND_RULES_TXT) {
      RulesTxtsForSolrIndex(solrIndexId,
        RulesTxtWithFileNames(querqyRulesTxtGenerator.renderSingleRulesTxt(solrIndexId, filterInputTagProperty, currentInputTag), sourceTempFile, dstCpFileTo),
        None,
        replaceRules
      )
    } else {
      val decompoundDstCpFileTo = if (targetSystem == "PRELIVE")
        SMUI_DEPLOY_PRELIVE_FN_DECOMPOUND_TXT
      else // targetSystem == "LIVE"
        DECOMPOUND_RULES_TXT_DST_CP_FILE_TO
      RulesTxtsForSolrIndex(solrIndexId,
        RulesTxtWithFileNames(querqyRulesTxtGenerator.renderSeparatedRulesTxts(solrIndexId, renderCompoundsRulesTxt = false, filterInputTagProperty, currentInputTag), sourceTempFile, dstCpFileTo),
        Some(RulesTxtWithFileNames(querqyRulesTxtGenerator.renderSeparatedRulesTxts(solrIndexId, renderCompoundsRulesTxt = true, filterInputTagProperty, currentInputTag),
          sourceTempFile + "-2", decompoundDstCpFileTo)
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

    def interfaceSmui2SolrSh(scriptPath: String, srcTmpFile: String, dstCpFileTo: String, solrHost: String,
                             solrCoreName: String, decompoundDstCpFileTo: String, targetSystem: String,
                             replaceRulesSrcTmpFile: String, replaceRulesDstCpFileTo: String
                            ): DeploymentScriptResult = {

      logger.info(
        s""":: interfaceSmui2SolrSh
           |:: scriptPath = $scriptPath
           |:: srcTmpFile = $srcTmpFile
           |:: dstCpFileTo = $dstCpFileTo
           |:: solrHost = $solrHost
           |:: solrCoreName = $solrCoreName
           |:: decompoundDstCpFileTo = $decompoundDstCpFileTo
           |:: targetSystem = $targetSystem
           |:: replaceRulesSrcTmpFile = $replaceRulesSrcTmpFile
           |:: replaceRulesDstCpFileTo = $replaceRulesDstCpFileTo
      """.stripMargin)

      val scriptCall =
      // define call for regular smui2solr (default or custom script) and add parameters to the script (in expected order, see smui2solr.sh)
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
          targetSystem + " " +
          // REPLACE_RULES_SRC_TMP_FILE=$7
          replaceRulesSrcTmpFile + " " +
          // REPLACE_RULES_DST_CP_FILE_TO=$8
          replaceRulesDstCpFileTo

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
      if (!firstResult.success) {
        // still accept, if no change happened
        if (firstResult.output.trim.endsWith("nothing to commit, working tree clean")) {
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

    val replaceRulesSrcTmpFile = rulesTxts.replaceRules.map(_.sourceFileName).getOrElse("NONE")
    val replaceRulesDstCpFileTo = rulesTxts.replaceRules.map(_.destinationFileName).getOrElse("NONE")

    // execute script
    // TODO currently only git deployment for LIVE instance available
    val deployToGit = targetSystem.equals("LIVE") && appConfig.get[String]("smui2solr.DST_CP_FILE_TO").equals("GIT")
    val result = (if (!deployToGit) {
      logger.info(s":: executeDeploymentScript :: regular script configured calling interfaceSmui2SolrSh(scriptPath = '$scriptPath')")
      interfaceSmui2SolrSh(
        scriptPath,
        srcTmpFile,
        dstCpFileTo,
        solrHost,
        solrCoreName,
        decompoundDstCpFileTo,
        targetSystem,
        replaceRulesSrcTmpFile,
        replaceRulesDstCpFileTo
      )
    } else {
      logger.info(":: executeDeploymentScript :: GIT configured calling interfaceSmui2GitSh")
      // TODO support further rule files (decompound / replace) and maybe solrCoreName and/or targetSystem for git branch?
      interfaceSmui2GitSh(
        environment.rootPath.getAbsolutePath + "/conf/smui2git.sh",
        srcTmpFile,
        featureToggleService.getSmuiDeploymentGitRepoUrl,
        featureToggleService.getSmuiDeploymentGitFilenameCommonRulesTxt,
      )
    })
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
        val rulesList = generateRulesTxtContentWithFilenamesList(index.id, "LIVE", logDebug = false)
        for (rules <- rulesList) {
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
      }
    } finally {
      logger.debug("Wrote all rules.txt files to a zip stream")
      zipStream.close()
      out.close()
    }
  }

  /**
    * Returns the file name with an optional tag inserted
    * When the file name has a 3 or 4 char extension,
    * the tag is inserted before the extension.
    */
  def appendTagToFileName(fileName: String, tag: String) : String = {
    if (tag.isEmpty)
      return fileName
    val regex = "^(.*)(\\.([A-Za-z0-9]{3,4}))$".r
    val tagLabel = "_".+(tag)
    val firstMatch =  regex.findFirstMatchIn(fileName)
    if (firstMatch.isDefined)
      firstMatch.get.group(1).+(tagLabel).+(firstMatch.get.group(2))
    else
      fileName.+(tagLabel)
  }

}
