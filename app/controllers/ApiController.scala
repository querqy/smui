package controllers

import java.io.{OutputStream, PipedInputStream, PipedOutputStream}

import akka.stream.scaladsl.{Source, StreamConverters}
import akka.util.ByteString
import javax.inject.Inject
import controllers.auth.AuthActionFactory
import models._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.json.Reads._

import scala.concurrent.{ExecutionContext, Future}
import models.SearchManagementModel._
import play.api.http.HttpEntity

// TODO Make ApiController pure REST- / JSON-Controller to ensure all implicit Framework responses (e.g. 400, 500) conformity
class ApiController @Inject()(searchManagementRepository: SearchManagementRepository,
                              querqyRulesTxtGenerator: QuerqyRulesTxtGenerator,
                              cc: MessagesControllerComponents,
                              authActionFactory: AuthActionFactory,
                              rulesTxtDeploymentService: RulesTxtDeploymentService)(implicit executionContext: ExecutionContext)
  extends MessagesAbstractController(cc) {

  private val logger = play.api.Logger

  val API_RESULT_OK = "OK"
  val API_RESULT_FAIL = "KO"

  case class ApiResult(result: String, message: String, returnId: Option[String])

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

  def listAllSolrIndeces = authActionFactory.getAuthenticatedAction(Action) {
    Ok(Json.toJson(searchManagementRepository.listAllSolrIndeces))
  }

  def addNewSolrIndex = authActionFactory.getAuthenticatedAction(Action) { request: Request[AnyContent] =>
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

  def downloadAllRulesTxtFiles = authActionFactory.getAuthenticatedAction(Action) { req =>
    Ok.chunked(
      createStreamResultInBackground(
        rulesTxtDeploymentService.writeAllRulesTxtFilesAsZipFileToStream)).as("application/zip")
  }

  private def createStreamResultInBackground(createStream: OutputStream => Unit): Source[ByteString, _] = {
    val in = new PipedInputStream()
    val out = new PipedOutputStream(in)
    new Thread(() => createStream(out)).start()
    StreamConverters.fromInputStream(() => in)
  }

  def listAllSearchInputs(solrIndexId: String) = authActionFactory.getAuthenticatedAction(Action).async {
    Future {
      // TODO add error handling (database connection, other exceptions)
      Ok(Json.toJson(searchManagementRepository.listAllSearchInputsInclDirectedSynonyms(solrIndexId)))
    }
  }

  def getDetailedSearchInput(searchInputId: String) = authActionFactory.getAuthenticatedAction(Action).async {
    Future {
      // TODO add error handling (database connection, other exceptions)
      Ok(Json.toJson(searchManagementRepository.getDetailedSearchInput(searchInputId)))
    }
  }

  def addNewSearchInput(solrIndexId: String) = authActionFactory.getAuthenticatedAction(Action).async { request: Request[AnyContent] =>
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

  def updateSearchInput(searchInputId: String) = authActionFactory.getAuthenticatedAction(Action) { request: Request[AnyContent] =>
    val body: AnyContent = request.body
    val jsonBody: Option[JsValue] = body.asJson

    // Expecting json body
    jsonBody.map { json =>
      val searchInput = json.as[SearchInput]

      querqyRulesTxtGenerator.validateSearchInputToErrMsg(searchInput) match {
        case Some(strErrMsg: String) =>
          // TODO transport validation result via API and communicate it to the user. Evaluate not saving the searchInput in this case.
          logger.error("updateSearchInput failed on validation of searchInput with id " + searchInputId + " - validation returned the following error output: <<<" + strErrMsg + ">>>")
        case None =>
      }

      // TODO handle potential conflict between searchInputId and JSON-passed searchInput.id
      searchManagementRepository.updateSearchInput(searchInput)
      // TODO consider Update returning the updated SearchInput(...) instead of an ApiResult(...)
      Ok(Json.toJson(ApiResult(API_RESULT_OK, "Updating Search Input successful.", Some(searchInputId))))
    }.getOrElse {
      BadRequest(Json.toJson(ApiResult(API_RESULT_FAIL, "Adding new Search Input failed. Unexpected body data.", None)))
    }
  }

  def deleteSearchInput(searchInputId: String) = authActionFactory.getAuthenticatedAction(Action).async {
    Future {
      searchManagementRepository.deleteSearchInput(searchInputId)
      Ok(Json.toJson(ApiResult(API_RESULT_OK, "Deleting Search Input successful", None)))
    }
  }

  /**
    * Performs an update of the rules.txt (or separate rules.txt files) to the configured Solr instance
    * while using the smui2solr.sh or a custom script.
    *
    * @param solrIndexId  Id of the Solr Index in the database
    * @param targetSystem "PRELIVE" vs. "LIVE" ... for reference @see evolutions/default/1.sql
    * @return Ok or BadRequest, if something failed.
    */
  def updateRulesTxtForSolrIndexAndTargetPlatform(solrIndexId: String, targetSystem: String): Action[AnyContent] = authActionFactory.getAuthenticatedAction(Action) {
    logger.debug("In ApiController :: updateRulesTxtForSolrIndex")

    // generate rules.txt(s)
    val rulesFiles = rulesTxtDeploymentService.generateRulesTxtContentWithFilenames(solrIndexId)

    // validate every generated rules.txt
    rulesTxtDeploymentService.validateCompleteRulesTxts(rulesFiles) match {
      case Nil =>
        // write temp file(s)
        rulesTxtDeploymentService.writeRulesTxtTempFiles(rulesFiles)

        // execute deployment script
        if (rulesTxtDeploymentService.executeDeploymentScript(rulesFiles, targetSystem) == 0) {
          searchManagementRepository.addNewDeploymentLogOk(solrIndexId, targetSystem)
          Ok(
            Json.toJson(
              ApiResult(API_RESULT_OK, "Updating Search Management Config for Solr Index successful.", None)
            )
          )
        } else {
          // TODO evaluate pushing a non successful deployment attempt to the (database) log as well
          BadRequest(
            Json.toJson(
              ApiResult(API_RESULT_FAIL, "Updating Solr Index failed. Unexpected result in script execution.", None)
            )
          )
        }
      case errors =>
        // TODO Evaluate being more precise in the error communication (eg which rules.txt failed?, where? / which line?, why?, etc.)
        BadRequest(
          Json.toJson(
            ApiResult(API_RESULT_FAIL, "Updating Solr Index failed. Validation error in rules.txt.", None)
          )
        )
    }
  }

  def listAllSuggestedSolrFields(solrIndexId: String): Action[AnyContent] = authActionFactory.getAuthenticatedAction(Action).async {
    Future {
      // TODO add error handling (database connection, other exceptions)
      Ok(Json.toJson(searchManagementRepository.listAllSuggestedSolrFields(solrIndexId)))
    }
  }

  def addNewSuggestedSolrField(solrIndexId: String)= authActionFactory.getAuthenticatedAction(Action).async { request: Request[AnyContent] =>
    Future {
      val body: AnyContent = request.body
      val jsonBody: Option[JsValue] = body.asJson

      // Expecting json body
      jsonBody.map { json =>
        val searchSuggestedSolrFieldName = (json \ "name").as[String]
        val maybeSuggestedSolrFieldId = searchManagementRepository.addNewSuggestedSolrField(
          solrIndexId, searchSuggestedSolrFieldName
        )

        Ok(Json.toJson(ApiResult(API_RESULT_OK, "Adding Suggested Field Name '" + searchSuggestedSolrFieldName + "' successful.", maybeSuggestedSolrFieldId)))
      }.getOrElse {
        BadRequest(Json.toJson(ApiResult(API_RESULT_FAIL, "Adding new Suggested Field Name failed. Unexpected body data.", None)))
      }
    }
  }

}
