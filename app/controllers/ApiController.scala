package controllers

import java.io.{OutputStream, PipedInputStream, PipedOutputStream}

import akka.stream.scaladsl.{Source, StreamConverters}
import akka.util.ByteString
import javax.inject.Inject
import play.api.Logging
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.json.Reads._
import java.nio.file.Paths
import java.time.format.DateTimeFormatter

import scala.concurrent.{ExecutionContext, Future}
import controllers.auth.AuthActionFactory
import models._
import models.querqy.QuerqyRulesTxtGenerator
import models.spellings.{CanonicalSpellingId, CanonicalSpellingValidator, CanonicalSpellingWithAlternatives}

// TODO Make ApiController pure REST- / JSON-Controller to ensure all implicit Framework responses (e.g. 400, 500) conformity
class ApiController @Inject()(searchManagementRepository: SearchManagementRepository,
                              querqyRulesTxtGenerator: QuerqyRulesTxtGenerator,
                              cc: MessagesControllerComponents,
                              authActionFactory: AuthActionFactory,
                              rulesTxtDeploymentService: RulesTxtDeploymentService)(implicit executionContext: ExecutionContext)
  extends MessagesAbstractController(cc) with Logging {

  val API_RESULT_OK = "OK"
  val API_RESULT_FAIL = "KO"

  case class ApiResult(result: String, message: String, returnId: Option[Id])
  implicit val apiResultWrites = Json.writes[ApiResult]


  def listAllSolrIndeces = authActionFactory.getAuthenticatedAction(Action) {
    Ok(Json.toJson(searchManagementRepository.listAllSolrIndexes))
  }

  def addNewSolrIndex = authActionFactory.getAuthenticatedAction(Action) { request: Request[AnyContent] =>
    val body: AnyContent = request.body
    val jsonBody: Option[JsValue] = body.asJson

    // Expecting json body
    jsonBody.map { json =>
      val searchIndexName = (json \ "name").as[String]
      val searchIndexDescription = (json \ "description").as[String]
      val solrIndexId = searchManagementRepository.addNewSolrIndex(
        SolrIndex(name = searchIndexName, description = searchIndexDescription)
      )

      Ok(Json.toJson(ApiResult(API_RESULT_OK, "Adding Search Input '" + searchIndexName + "' successful.", Some(solrIndexId))))
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

  def listAllSearchInputs(solrIndexId: String) = authActionFactory.getAuthenticatedAction(Action) {
    // TODO add error handling (database connection, other exceptions)
    Ok(Json.toJson(searchManagementRepository.listAllSearchInputsInclDirectedSynonyms(SolrIndexId(solrIndexId))))
  }

  def listAllInputTags(): Action[AnyContent] = authActionFactory.getAuthenticatedAction(Action) {
    Ok(Json.toJson(searchManagementRepository.listAllInputTags()))
  }

  def getDetailedSearchInput(searchInputId: String) = authActionFactory.getAuthenticatedAction(Action) {
    // TODO add error handling (database connection, other exceptions)
    Ok(Json.toJson(searchManagementRepository.getDetailedSearchInput(SearchInputId(searchInputId))))
  }

  def addNewSearchInput(solrIndexId: String) = authActionFactory.getAuthenticatedAction(Action).async { request: Request[AnyContent] =>
    Future {
      val body: AnyContent = request.body
      val jsonBody: Option[JsValue] = body.asJson

      // Expecting json body
      jsonBody.map { json =>
        val searchInputTerm = (json \ "term").as[String]
        val tags = (json \ "tags").as[Seq[String]].map(InputTagId(_))
        val searchInputId = searchManagementRepository.addNewSearchInput(SolrIndexId(solrIndexId), searchInputTerm, tags)

        Ok(Json.toJson(ApiResult(API_RESULT_OK, "Adding Search Input '" + searchInputTerm + "' successful.", Some(searchInputId))))
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
      val searchInput = json.as[SearchInputWithRules]

      querqyRulesTxtGenerator.validateSearchInputToErrMsg(searchInput) match {
        case Some(strErrMsg: String) =>
          logger.error("updateSearchInput failed on validation of searchInput with id " + searchInputId + " - validation returned the following error output: <<<" + strErrMsg + ">>>")
          BadRequest(Json.toJson(ApiResult(API_RESULT_FAIL, strErrMsg, None)))
        case None =>
          // TODO handle potential conflict between searchInputId and JSON-passed searchInput.id
          searchManagementRepository.updateSearchInput(searchInput)
          // TODO consider Update returning the updated SearchInput(...) instead of an ApiResult(...)
          Ok(Json.toJson(ApiResult(API_RESULT_OK, "Updating Search Input successful.", Some(SearchInputId(searchInputId)))))
      }
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

  def listAll(solrIndexId: String) = authActionFactory.getAuthenticatedAction(Action) {
    val searchInputs = searchManagementRepository.listAllSearchInputsInclDirectedSynonyms(SolrIndexId(solrIndexId))
    val spellings = searchManagementRepository.listAllSpellingsWithAlternatives(SolrIndexId(solrIndexId))
    Ok(Json.toJson(ListItem.create(searchInputs, spellings)))
  }

  def addNewSpelling(solrIndexId: String) = authActionFactory.getAuthenticatedAction(Action).async { request: Request[AnyContent] =>
    Future {
      val body: AnyContent = request.body
      val jsonBody: Option[JsValue] = body.asJson

      val optTerm = jsonBody.flatMap(json => (json \"term").asOpt[String])
      optTerm.map { term =>
        val canonicalSpelling = searchManagementRepository.addNewCanonicalSpelling(SolrIndexId(solrIndexId), term)
        Ok(Json.toJson(ApiResult(API_RESULT_OK, "Adding new canonical spelling '" + term + "' successful.", Some(canonicalSpelling.id))))
      }.getOrElse {
        BadRequest(Json.toJson(ApiResult(API_RESULT_FAIL, "Adding new canonical spelling failed. Unexpected body data.", None)))
      }
    }
  }

  def getDetailedSpelling(canonicalSpellingId: String) = authActionFactory.getAuthenticatedAction(Action).async {
    Future {
      val spellingWithAlternatives = searchManagementRepository.getDetailedSpelling(canonicalSpellingId)
      Ok(Json.toJson(spellingWithAlternatives))
    }
  }

  def updateSpelling(solrIndexId: String, canonicalSpellingId: String) = authActionFactory.getAuthenticatedAction(Action) { request: Request[AnyContent] =>
    val body: AnyContent = request.body
    val jsonBody: Option[JsValue] = body.asJson

    // Expecting json body
    jsonBody.map { json =>
      val spellingWithAlternatives = json.as[CanonicalSpellingWithAlternatives]

      val otherSpellings = searchManagementRepository.listAllSpellingsWithAlternatives(SolrIndexId(solrIndexId)).filter(_.id != spellingWithAlternatives.id)
      CanonicalSpellingValidator.validateCanonicalSpellingsAndAlternatives(spellingWithAlternatives, otherSpellings) match {
        case Nil =>
          searchManagementRepository.updateSpelling(spellingWithAlternatives)
          Ok(Json.toJson(ApiResult(API_RESULT_OK, "Updating canonical spelling successful.", Some(CanonicalSpellingId(canonicalSpellingId)))))
        case errors =>
          val msgs = s"Failed to update spelling ${spellingWithAlternatives.term}: " +  errors.mkString("\n")
          logger.error(msgs)
          BadRequest(Json.toJson(ApiResult(API_RESULT_FAIL, msgs, None)))
      }
    }.getOrElse {
      BadRequest(Json.toJson(ApiResult(API_RESULT_FAIL, "Updating canonical spelling failed. Unexpected body data.", None)))
    }
  }

  def deleteSpelling(canonicalSpellingId: String) = authActionFactory.getAuthenticatedAction(Action).async {
    Future {
      searchManagementRepository.deleteSpelling(canonicalSpellingId)
      Ok(Json.toJson(ApiResult(API_RESULT_OK, "Deleting canonical spelling with alternatives successful.", None)))
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
    val rulesFiles = rulesTxtDeploymentService.generateRulesTxtContentWithFilenames(SolrIndexId(solrIndexId), targetSystem)

    // validate every generated rules.txt
    rulesTxtDeploymentService.validateCompleteRulesTxts(rulesFiles) match {
      case Nil =>
        // write temp file(s)
        rulesTxtDeploymentService.writeRulesTxtTempFiles(rulesFiles)

        // execute deployment script
        val result = rulesTxtDeploymentService.executeDeploymentScript(rulesFiles, targetSystem)
        if (result.success) {
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
              ApiResult(API_RESULT_FAIL, s"Updating Solr Index failed.\nScript output:\n${result.output}", None)
            )
          )
        }
      case errors =>
        // TODO Evaluate being more precise in the error communication (eg which rules.txt failed?, where? / which line?, why?, etc.)
        BadRequest(
          Json.toJson(
            ApiResult(API_RESULT_FAIL, s"Updating Solr Index failed. Validation errors in rules.txt:\n${errors.mkString("\n")}", None)
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
        val field = searchManagementRepository.addNewSuggestedSolrField(
          SolrIndexId(solrIndexId), searchSuggestedSolrFieldName
        )

        Ok(Json.toJson(ApiResult(API_RESULT_OK, "Adding Suggested Field Name '" + searchSuggestedSolrFieldName + "' successful.", Some(field.id))))
      }.getOrElse {
        BadRequest(Json.toJson(ApiResult(API_RESULT_FAIL, "Adding new Suggested Field Name failed. Unexpected body data.", None)))
      }
    }
  }

  // TODO consider making method .asynch
  def importFromRulesTxt(solrIndexId: String) = authActionFactory.getAuthenticatedAction(Action)(parse.multipartFormData) { request =>
    request.body
      .file("rules_txt")
      .map { rules_txt =>
        // read POSTed file (like suggested in https://www.playframework.com/documentation/2.7.x/ScalaFileUpload)
        // only get the last part of the filename
        // otherwise someone can send a path like ../../home/foo/bar.txt to write to other files on the system
        val filename = Paths.get(rules_txt.filename).getFileName
        //val fileSize = rules_txt.fileSizes
        //val contentType = rules_txt.contentType
        val tmp_file_path = s"/tmp/$filename"
        rules_txt.ref.copyTo(Paths.get(tmp_file_path), replace = true)
        // process rules.txt file
        val filePayload = scala.io.Source.fromFile(tmp_file_path).getLines.mkString("\n")
        val importStatistics = ApiControllerHelperRulesTxt.importFromFilePayload(filePayload, SolrIndexId(solrIndexId), searchManagementRepository)
        val apiResultMsg = "Import from rules.txt file successful with following statistics:\n" +
          "^-- count rules.txt inputs = " + importStatistics._1 + "\n" +
          "^-- count rules.txt lines skipped = " + importStatistics._2 + "\n" +
          "^-- count rules.txt unknown convert = " + importStatistics._3 + "\n" +
          "^-- count consolidated inputs (after rev engineering undirected synonyms) = " + importStatistics._4 + "\n" +
          "^-- count total rules after consolidation = " + importStatistics._5
        Ok(Json.toJson(ApiResult(API_RESULT_OK, apiResultMsg, None)))
      }
      .getOrElse {
        Ok(Json.toJson(ApiResult(API_RESULT_FAIL, "File rules_txt missing in request body.", None)))
      }
  }

  case class LogDeploymentInfo(msg: String)
  implicit val logDeploymentInfoWrites = Json.writes[LogDeploymentInfo]

  def getLatestDeploymentResult(solrIndexId: String, targetSystem: String): Action[AnyContent] = authActionFactory.getAuthenticatedAction(Action).async { request: Request[AnyContent] =>
    Future {
      logger.debug("In ApiController :: updateRulesTxtForSolrIndex")
      logger.debug(s"... solrIndexId = $solrIndexId")
      logger.debug(s"... targetSystem = $targetSystem")

      val msg = searchManagementRepository.lastDeploymentLogDetail(solrIndexId, targetSystem) match {
        case Some(deploymentLogDetail) => {
          val formatLastUpdate = deploymentLogDetail.lastUpdate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
          s"Last publish on $targetSystem ${formatLastUpdate} OK"
        }
        case None => s"No deployment event for $targetSystem"
      }
      val logDeploymentInfo = new LogDeploymentInfo(msg)
      Ok(Json.toJson(logDeploymentInfo))
    }
  }

}
