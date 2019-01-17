package controllers

import javax.inject.Inject
import controllers.auth.AuthActionFactory
import models._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.{Configuration, Play}

import sys.process._
import scala.concurrent.{ExecutionContext, Future}
import models.SearchManagementModel._
import models.FeatureToggleModel._

// TODO Make ApiController pure REST- / JSON-Controller to ensure all implicit Framework responses (e.g. 400, 500) conformity
class ApiController @Inject()(searchManagementRepository: SearchManagementRepository,
                              querqyRulesTxtGenerator: QuerqyRulesTxtGenerator,
                              cc: MessagesControllerComponents,
                              appConfig: Configuration,
                              featureToggleService: FeatureToggleService,
                              authActionFactory: AuthActionFactory)(implicit executionContext: ExecutionContext)
  extends MessagesAbstractController(cc) {

  private val logger = play.api.Logger

  val API_RESULT_OK = "OK"
  val API_RESULT_FAIL = "KO"

  case class ApiResult(result: String, message: String, returnId: Option[Long])

  implicit val solrIndexWrites = Json.writes[SolrIndex]
  implicit val suggestedSolrFieldWrites = Json.writes[SuggestedSolrField]
  implicit val searchSynonymWrites = Json.writes[SynonymRule]
  implicit val upDownRuleWrites = Json.writes[UpDownRule]
  implicit val filterRuleWrites = Json.writes[FilterRule]
  implicit val deleteWrites = Json.writes[DeleteRule]
  implicit val searchInputWrites = Json.writes[SearchInput]

  // TODO for all Json.reads, that "id" = null JSON values are converted to Option.None
  implicit val searchSynonymReads = Json.reads[SynonymRule]
  implicit val upDownRuleReads = Json.reads[UpDownRule]
  implicit val filterRuleReads = Json.reads[FilterRule]
  implicit val deleteReads = Json.reads[DeleteRule]
  implicit val searchInputReads = Json.reads[SearchInput]

  implicit val apiResultWrites = Json.writes[ApiResult]

  def listAllSolrIndeces = authActionFactory.getAuthenticatedAction(Action).async {
    Future {
      Ok(Json.toJson(searchManagementRepository.listAllSolrIndeces))
    }
  }

  def addNewSolrIndex = authActionFactory.getAuthenticatedAction(Action).async { request: Request[AnyContent] =>
    Future {
      val body: AnyContent = request.body
      val jsonBody: Option[JsValue] = body.asJson

      // Expecting json body
      jsonBody.map { json =>
        val searchIndexName = (json \ "name").as[String]
        val searchIndexDescription = (json \ "description").as[String]
        val maybeSolrIndexId = searchManagementRepository.addNewSolrIndex(
          SolrIndex(None, searchIndexName, searchIndexDescription)
        )

        Ok(Json.toJson(ApiResult(API_RESULT_OK, "Adding Search Input '" + searchIndexName + "' successful.", maybeSolrIndexId)))
      }.getOrElse {
        BadRequest(Json.toJson(ApiResult(API_RESULT_FAIL, "Adding new Search Input failed. Unexpected body data.", None)))
      }
    }
  }

  def listAllSearchInputs(solrIndexId: Long) = authActionFactory.getAuthenticatedAction(Action).async {
    Future {
      // TODO add error handling (database connection, other exceptions)
      Ok(Json.toJson(searchManagementRepository.listAllSearchInputsInclDirectedSynonyms(solrIndexId)))
    }
  }

  def getDetailedSearchInput(searchInputId: Long) = authActionFactory.getAuthenticatedAction(Action).async {
    Future {
      // TODO add error handling (database connection, other exceptions)
      Ok(Json.toJson(searchManagementRepository.getDetailedSearchInput(searchInputId)))
    }
  }

  def addNewSearchInput(solrIndexId: Long) = authActionFactory.getAuthenticatedAction(Action).async { request: Request[AnyContent] =>
    Future {
      val body: AnyContent = request.body
      val jsonBody: Option[JsValue] = body.asJson

      // Expecting json body
      jsonBody.map { json =>
        val searchInputTerm = (json \ "term").as[String]
        val maybeSearchInputId = searchManagementRepository.addNewSearchInput(solrIndexId, searchInputTerm)

        Ok(Json.toJson(ApiResult(API_RESULT_OK, "Adding Search Input '" + searchInputTerm + "' successful.", maybeSearchInputId)))
      }.getOrElse {
        BadRequest(Json.toJson(ApiResult(API_RESULT_FAIL, "Adding new Search Input failed. Unexpected body data.", None)))
      }
    }
  }

  def updateSearchInput(searchInputId: Long) = authActionFactory.getAuthenticatedAction(Action).async { request: Request[AnyContent] =>
    Future {

      val body: AnyContent = request.body
      val jsonBody: Option[JsValue] = body.asJson

      // Expecting json body
      jsonBody.map { json =>
        val searchInput = json.as[SearchInput]

        querqyRulesTxtGenerator.validateSearchInputToErrMsg(searchInput) match {
          case Some(strErrMsg: String) => {
            // TODO transport validation result via API and communicate it to the user. Evaluate not saving the searchInput in this case.
            logger.error("updateSearchInput failed on validation of searchInput with id " + searchInputId + " - validation returned the following error output: <<<" + strErrMsg + ">>>")
          }
          case None => {}
        }

        // TODO handle potential conflict between searchInputId and JSON-passed searchInput.id
        searchManagementRepository.updateSearchInput(searchInput)
        // TODO consider Update returning the updated SearchInput(...) instead of an ApiResult(...)
        Ok(Json.toJson(ApiResult(API_RESULT_OK, "Updating Search Input successful.", Some(searchInputId))))
      }.getOrElse {
        BadRequest(Json.toJson(ApiResult(API_RESULT_FAIL, "Adding new Search Input failed. Unexpected body data.", None)))
      }
    }
  }

  def deleteSearchInput(searchInputId: Long) = authActionFactory.getAuthenticatedAction(Action).async {
    Future {
      searchManagementRepository.deleteSearchInput(searchInputId)
      Ok(Json.toJson(ApiResult(API_RESULT_OK, "Deleting Search Input successful", None)))
    }
  }

  /**
    * Generates a list of source to destination filenames containing the rules.txt(s) according to current application settings.
    *
    * @param solrIndexId Solr Index Id to generate the output for.
    * @return List of {{_1}} = source filename, {{_2}} destination filename, {{_3}} rules.txt content. The first entry - by contract - does
    *         always contain the regular rules.txt. The second one - optionally - contains the decompound-rules.txt.
    */
  private def generateSrcDstFilenamesToCompleteRulesTxts(solrIndexId: Long): List[(String, String, String)] = {

    val SRC_TMP_FILE = appConfig.getOptional[String]("smui2solr.SRC_TMP_FILE").getOrElse("/tmp/search-management-ui_rules-txt.tmp")
    val DST_CP_FILE_TO = appConfig.getOptional[String]("smui2solr.DST_CP_FILE_TO").getOrElse("/usr/bin/solr/defaultCore/conf/rules.txt")
    val DO_SPLIT_DECOMPOUND_RULES_TXT = featureToggleService.getToggleRuleDeploymentSplitDecompoundRulesTxt
    val DECOMPOUND_RULES_TXT_DST_CP_FILE_TO = featureToggleService.getToggleRuleDeploymentSplitDecompoundRulesTxtDstCpFileTo

    logger.debug(
      s""":: generateSrcDstFilenamesToCompleteRulesTxts config
         |:: SRC_TMP_FILE = ${SRC_TMP_FILE}
         |:: DST_CP_FILE_TO = ${DST_CP_FILE_TO}
         |:: DO_SPLIT_DECOMPOUND_RULES_TXT = ${DO_SPLIT_DECOMPOUND_RULES_TXT}
         |:: DECOMPOUND_RULES_TXT_DST_CP_FILE_TO = ${DECOMPOUND_RULES_TXT_DST_CP_FILE_TO}
    """.stripMargin)

    // generate one rules.txt by default or two separated, if decompound instructions are supposed to be split

    // TODO test correct generation in different scenarios (one vs. two rules.txts, etc.)
    if (!DO_SPLIT_DECOMPOUND_RULES_TXT) {

      List(
        // generate one rules.txt for original temp file
        (SRC_TMP_FILE, DST_CP_FILE_TO, querqyRulesTxtGenerator.renderSingleRulesTxt(solrIndexId))
      )

    } else {

      List(
        // generate first (regular) rules.txt for original temp file
        (SRC_TMP_FILE, DST_CP_FILE_TO, querqyRulesTxtGenerator.renderSeparatedRulesTxts(solrIndexId, false)),
        // generate decompound-rules.txt for temp file (filename resulting from original temp file plus "-2" append)
        (SRC_TMP_FILE + "-2", DECOMPOUND_RULES_TXT_DST_CP_FILE_TO, querqyRulesTxtGenerator.renderSeparatedRulesTxts(solrIndexId, true))
      )

    }
  }

  private def validateCompleteRulesTxts(srcDstFilenamesToCompleteRulesTxts: List[(String, String, String)]): Option[play.api.mvc.Result] = {

    val listErrResults = srcDstFilenamesToCompleteRulesTxts.map(srcDstFilenamesToCompleteRulesTxt => {
      logger.debug(":: validateCompleteRulesTxts for src = " + srcDstFilenamesToCompleteRulesTxt._1 + " dst = " + srcDstFilenamesToCompleteRulesTxt._2)
      logger.debug(":: rulesTxt = <<<" + srcDstFilenamesToCompleteRulesTxt._3 + ">>>")
      val validationResult = querqyRulesTxtGenerator.validateQuerqyRulesTxtToErrMsg(srcDstFilenamesToCompleteRulesTxt._3)
      validationResult.map(strErrMsg =>
        logger.debug(":: validation failed with message = " + strErrMsg)
      )
      validationResult
    }).filter(validationResult =>
      validationResult.nonEmpty
    )

    if (listErrResults.nonEmpty) {
      // TODO Evaluate being more precise in the error communication (eg which rules.txt failed?, where? / which line?, why?, etc.)
      Some(BadRequest(
        Json.toJson(new ApiResult(API_RESULT_FAIL, "Updating Solr Index failed. Validation error in rules.txt.", None))
      ))
    } else {
      None
    }
  }

  private def writeRulesTxtsTempFiles(srcDstFilenamesToCompleteRulesTxts: List[(String, String, String)]) = {

    // helper to write rules.txt output to to temp file
    def writeRulesTxtToTempFile(strRulesTxt: String, tmpFilePath: String) = {
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
    srcDstFilenamesToCompleteRulesTxts.map(filenameToCompleteRulesTxt =>
      writeRulesTxtToTempFile(filenameToCompleteRulesTxt._3, filenameToCompleteRulesTxt._1)
    )
  }

  private def executeDeploymentScript(srcDstFilenamesToCompleteRulesTxts: List[(String, String, String)], solrIndexId: Long, targetSystem: String): Int = {

    val SOLR_HOST = appConfig.getOptional[String]("smui2solr.SOLR_HOST").getOrElse("localhost:8983")
    val SOLR_CORE_NAME = searchManagementRepository.getSolrIndexName(solrIndexId)
    val DO_CUSTOM_SCRIPT_SMUI2SOLR_SH = featureToggleService.getToggleRuleDeploymentCustomScript
    val CUSTOM_SCRIPT_SMUI2SOLR_SH_PATH = featureToggleService.getToggleRuleDeploymentCustomScriptSmui2solrShPath

    logger.debug(
      s""":: executeDeploymentScript config
         |:: targetSystem = ${targetSystem}
         |:: SOLR_HOST = ${SOLR_HOST}
         |:: SOLR_CORE_NAME = ${SOLR_CORE_NAME}
         |:: DO_CUSTOM_SCRIPT_SMUI2SOLR_SH = ${DO_CUSTOM_SCRIPT_SMUI2SOLR_SH}
         |:: CUSTOM_SCRIPT_SMUI2SOLR_SH_PATH = ${CUSTOM_SCRIPT_SMUI2SOLR_SH_PATH}
    """.stripMargin)

    // TODO Evaluate other ways to deal with the strong implicit dependency / necessary knowledge from the inner structure of srcDstFilenamesToCompleteRulesTxts (generated by generateSrcDstFilenamesToCompleteRulesTxts)
    // TODO ... same goes for the interface to the deployment script

    val scriptCall =
    // decide for the right script
      (if (DO_CUSTOM_SCRIPT_SMUI2SOLR_SH)
        CUSTOM_SCRIPT_SMUI2SOLR_SH_PATH
      else
        Play.current.path.getAbsolutePath() + "/conf/smui2solr.sh") + " " +
        // add parameters to the script (in expected order, see smui2solr.sh)
        srcDstFilenamesToCompleteRulesTxts.head._1 + " " + // smui2solr.sh param $1 - SRC_TMP_FILE
        srcDstFilenamesToCompleteRulesTxts.head._2 + " " + // smui2solr.sh param $2 - DST_CP_FILE_TO
        SOLR_HOST + " " + // smui2solr.sh param $3 - SOLR_HOST
        SOLR_CORE_NAME + " " + // smui2solr.sh param $4 - SOLR_CORE_NAME
        (if (srcDstFilenamesToCompleteRulesTxts.size > 1) srcDstFilenamesToCompleteRulesTxts(1)._2 else "NONE") + " " + // smui2solr.sh param $5 - DECOMPOUND_DST_CP_FILE_TO
        targetSystem // smui2solr.sh param $6 - TARGET_SYSTEM
    val result = scriptCall !; // TODO perform file copying and solr core reload directly in the application (without any shell dependency)
    logger.debug(":: executeDeploymentScript :: Script execution result: " + result)
    result
  }

  /**
    * Performs an update of the rules.txt (or separate rules.txt files) to the configured Solr instance
    * while using the smui2solr.sh or a custom script.
    *
    * @param solrIndexId  Id of the Solr Index in the database
    * @param targetSystem "PRELIVE" vs. "LIVE" ... for reference @see evolutions/default/2.sql
    * @return Ok or BadRequest, if something failed.
    */
  def updateRulesTxtForSolrIndexAndTargetPlatform(solrIndexId: Long, targetSystem: String): Action[AnyContent] = authActionFactory.getAuthenticatedAction(Action).async {
    Future {
      logger.debug("In ApiController :: updateRulesTxtForSolrIndex")

      // generate rules.txt(s)
      val srcDstFilenamesToCompleteRulesTxts = generateSrcDstFilenamesToCompleteRulesTxts(solrIndexId)

      // validate every generated rules.txt
      validateCompleteRulesTxts(srcDstFilenamesToCompleteRulesTxts) match {
        case Some(errorResult: play.api.mvc.Result) => errorResult
        case None => {
          // write temp file(s)
          writeRulesTxtsTempFiles(srcDstFilenamesToCompleteRulesTxts)

          // execute deployment script
          if (executeDeploymentScript(srcDstFilenamesToCompleteRulesTxts, solrIndexId, targetSystem) == 0) {
            searchManagementRepository.addNewDeploymentLogOk(solrIndexId, targetSystem)
            Ok(Json.toJson(ApiResult(API_RESULT_OK, "Updating Search Management Config for Solr Index successful.", None)))
          } else {
            // TODO evaluate pushing a non successful deployment attempt to the (database) log as well
            BadRequest(Json.toJson(ApiResult(API_RESULT_FAIL, "Updating Solr Index failed. Unexpected result in script execution.", None)))
          }
        }
      }
    }
  }

  def listAllSuggestedSolrFields(solrIndexId: Long): Action[AnyContent] = authActionFactory.getAuthenticatedAction(Action).async {
    Future {
      // TODO add error handling (database connection, other exceptions)
      Ok(Json.toJson(searchManagementRepository.listAllSuggestedSolrFields(solrIndexId)))
    }
  }

}
