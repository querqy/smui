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
import java.time.LocalDateTime

import scala.concurrent.{ExecutionContext, Future}
import controllers.auth.AuthActionFactory
import models._
import models.config.SmuiVersion
import models.input.{InputTagId, ListItem, SearchInputId, SearchInputWithRules}
import models.querqy.QuerqyRulesTxtGenerator
import models.spellings.{CanonicalSpellingId, CanonicalSpellingValidator, CanonicalSpellingWithAlternatives}
import services.RulesTxtDeploymentService

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

  // TODO check, if method is still in use or got substituted by listAll()?
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
    logger.debug("In ApiController :: updateRulesTxtForSolrIndexAndTargetPlatform")

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

  /**
    * Deployment info (raw or formatted)
    */

  case class DeploymentInfo(msg: Option[String])
  implicit val logDeploymentInfoWrites = Json.writes[DeploymentInfo]

  def getLatestDeploymentResult(solrIndexId: String, targetSystem: String): Action[AnyContent] = authActionFactory.getAuthenticatedAction(Action).async { request: Request[AnyContent] =>
    Future {
      logger.debug("In ApiController :: getLatestDeploymentResult")
      logger.debug(s"... solrIndexId = $solrIndexId")
      logger.debug(s"... targetSystem = $targetSystem")

      // TODO make part of routes as optional parameter? GET spec for the call is a bit scattered right now ...
      val rawReqPrm: Option[String] = request.getQueryString("raw")
      val isRawRequested: Boolean = rawReqPrm match {
        case Some(s) => s.equals("true")
        case None => false
      }

      logger.debug(s"... isRawRequested = $isRawRequested")

      val deplLogDetail = searchManagementRepository.lastDeploymentLogDetail(solrIndexId, targetSystem)
      def getRawVerboseDeplMsg() = {
        if(isRawRequested) {
          // raw date output
          deplLogDetail match {
            case Some(deploymentLogDetail) => {
              DeploymentInfo(Some(s"${deploymentLogDetail.lastUpdate}"))
            }
            case None => DeploymentInfo(None)
          }
        } else {
          // verbose output (default)
          val msg = deplLogDetail match {
            case Some(deploymentLogDetail) => {
              val formatLastUpdate = deploymentLogDetail.lastUpdate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
              s"Last publish on $targetSystem ${formatLastUpdate} OK"
            }
            case None => s"No deployment event for $targetSystem"
          }
          DeploymentInfo(Some(msg))
        }
      }

      Ok(Json.toJson(getRawVerboseDeplMsg()))
    }
  }

  /**
    * Config info
    */

  case class SmuiVersionInfo(
    latestMarketStandard: Option[String],
    current: Option[String],
    infoType: String,
    msgHtml: String
  )

  object SmuiVersionInfoType extends Enumeration {
    val INFO = Value("INFO")
    val WARN = Value("WARN")
    val ERROR = Value("ERROR")
  }

  implicit val smuiVersionInfoWrites = Json.writes[SmuiVersionInfo]

  // TODO consider outsourcing this "business logic" into the (config) model
  def getLatestVersionInfo() = authActionFactory.getAuthenticatedAction(Action).async {
    Future {
      // get latest version from dockerhub
      val latestFromDockerHub = SmuiVersion.latestVersionFromDockerHub()
      val current = SmuiVersion.parse(models.buildInfo.BuildInfo.version)

      val versionInfo = (if (latestFromDockerHub.isEmpty || current.isEmpty) {
        logger.error(s":: cannot determine version diff between latestFromDockerHub and current ($latestFromDockerHub, $current)")

        def renderVersionOption(o: Option[SmuiVersion]) = o match {
          case None => None
          case Some(version) => Some(s"$version")
        }

        SmuiVersionInfo(
          renderVersionOption(latestFromDockerHub),
          renderVersionOption(current),
          SmuiVersionInfoType.ERROR.toString,
          "<div>Unable to determine version diff between market standard (on DockerHub) and local instance installation (see logs).<div>"
        )

      } else {

        logger.info(s":: latest version from DockerHub = ${latestFromDockerHub.get}")

        val (infoType, msgHtml) = (if(latestFromDockerHub.get.greaterThan(current.get)) {
          (
            SmuiVersionInfoType.WARN.toString,
            // note: logical HTML structure within modal dialog begins with <h5>
            "<h5>Info</h5>" +
              // TODO get maintainer from build.sbt
              "<div>Your locally installed <strong>SMUI instance is outdated</strong>. Please consider an update. If you have issues, contact the maintainer (<a href=\"mailto:paulbartusch@gmx.de\">paulbartusch@gmx.de</a>) or file an issue to the project: <a href=\"https://github.com/querqy/smui/issues\" target=\"_new\">https://github.com/querqy/smui/issues</a><div>"
              // TODO parse querqy.org/docs/smui/release-notes/ and teaser new features (optional) - might look like:
              // "<hr>" +
              // "<h5>What's new</h5>"
              // "<ul>LIST_OF_RELEASE_NOTES</ul>" +
              // "<div>See <a href=\"https://querqy.org/docs/smui/release-notes/\" target=\"_new\">https://querqy.org/docs/smui/release-notes/</a></div>"
          )
        } else (
            SmuiVersionInfoType.INFO.toString,
            // TODO only case, that does not deliver HTML - semantically not nice, but feasible
            "SMUI is up-to-date!"
          )
        )

        SmuiVersionInfo(
          Some(s"${latestFromDockerHub.get}"),
          Some(s"${current.get}"),
          infoType,
          msgHtml
        )
      })

      Ok(Json.toJson(versionInfo))
    }
  }

  /**
    * Activity log
    */

  def getActivityLog(inputId: String) = authActionFactory.getAuthenticatedAction(Action).async {
    Future {
      val activityLog = searchManagementRepository.getInputRuleActivityLog(inputId)
      Ok(Json.toJson(activityLog))
    }
  }

  /**
    * Reports (for Activity log as well)
    */

  def getRulesReport(solrIndexId: String) = authActionFactory.getAuthenticatedAction(Action).async {
    Future {
      val report = searchManagementRepository.getRulesReport(SolrIndexId(solrIndexId))
      Ok(Json.toJson(report))
    }
  }

  def getActivityReport(solrIndexId: String) = authActionFactory.getAuthenticatedAction(Action).async { request: Request[AnyContent] => {
      Future {
        val rawDateFrom: Option[String] = request.getQueryString("dateFrom")
        val rawDateTo: Option[String] = request.getQueryString("dateTo")

        // TODO switch to debug
        logger.debug("In ApiController :: getActivityReport")
        logger.debug(s":: rawDateFrom = $rawDateFrom")
        logger.debug(s":: rawDateTo = $rawDateTo")

        // TODO ensure aligned date pattern between frontend and backend
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        // TODO make error nicer, in case either From or To parameter did not exist
        // fyi: hours/minutes/seconds needs to be added
        // (see https://stackoverflow.com/questions/22463062/how-to-parse-format-dates-with-localdatetime-java-8)
        val dateFrom = LocalDateTime.parse(s"${rawDateFrom.get} 00:00:00", formatter)
        val dateTo = LocalDateTime.parse(s"${rawDateTo.get} 23:59:59", formatter)

        logger.debug(s":: dateFrom = $dateFrom")
        logger.debug(s":: dateTo = $dateTo")

        val report = searchManagementRepository.getActivityReport(SolrIndexId(solrIndexId), dateFrom, dateTo)
        Ok(Json.toJson(report))
      }
    }
  }

}
