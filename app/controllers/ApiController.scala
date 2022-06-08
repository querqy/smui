//CJM 10
package controllers

import akka.stream.scaladsl.{Source, StreamConverters}
import akka.util.ByteString
import controllers.auth.{AuthActionFactory, UserRequest}
import models.FeatureToggleModel.FeatureToggleService
import models._
import models.config.SmuiVersion
import models.input._
import models.querqy.QuerqyRulesTxtGenerator
import models.spellings.{CanonicalSpellingId, CanonicalSpellingValidator, CanonicalSpellingWithAlternatives}
import models.validatedimport.ValidatedImportData
import org.checkerframework.checker.units.qual.A
import play.api.Logging
import play.api.libs.Files
import play.api.libs.Files.TemporaryFile.temporaryFileToPath
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.mvc._
import services.{RulesTxtDeploymentService, RulesTxtImportService}

import java.io.{OutputStream, PipedInputStream, PipedOutputStream}
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


// TODO Make ApiController pure REST- / JSON-Controller to ensure all implicit Framework responses (e.g. 400, 500) conformity
class ApiController @Inject()(authActionFactory: AuthActionFactory,
                              featureToggleService: FeatureToggleService,
                              searchManagementRepository: SearchManagementRepository,
                              querqyRulesTxtGenerator: QuerqyRulesTxtGenerator,
                              cc: MessagesControllerComponents,
                              rulesTxtDeploymentService: RulesTxtDeploymentService,
                              rulesTxtImportService: RulesTxtImportService)(implicit executionContext: ExecutionContext)
  extends MessagesAbstractController(cc) with Logging {

  val API_RESULT_OK = "OK"
  val API_RESULT_FAIL = "KO"

  case class ApiResult(result: String, message: String, returnId: Option[Id])

  implicit val apiResultWrites = Json.writes[ApiResult]

  def getFeatureToggles = authActionFactory.getAuthenticatedAction(Action) {
    Ok(Json.toJson(featureToggleService.getJsFrontendToggleList))
  }

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

      try {
        var solrIndexId = searchManagementRepository.addNewSolrIndex(
          SolrIndex(name = searchIndexName, description = searchIndexDescription)
        );
        logger.debug("solrIndexId:" + solrIndexId);
        Ok(Json.toJson(ApiResult(API_RESULT_OK, "Successfully added Deployment Channel '" + searchIndexName + "'.", Some(solrIndexId))))
      } catch {
        case e: Exception => {
          logger.debug("The searchIndexDescription (Search Engine Collection Name) given was likely a duplicate.");
          BadRequest(Json.toJson(ApiResult(API_RESULT_FAIL, "Could not add Rules Collection. Only one Rules Collection per Search Engine Collection is allowed.", None)))
        };
      }

    }.getOrElse {
      BadRequest(Json.toJson(ApiResult(API_RESULT_FAIL, "Adding new Deployment Channel failed. Unexpected body data.", None)))
    }
  }

  def getSolrIndex(solrIndexId: String) = authActionFactory.getAuthenticatedAction(Action).async {
    Future {
      Ok(Json.toJson(searchManagementRepository.getSolrIndex(SolrIndexId(solrIndexId))))
    }
  }

  def deleteSolrIndex(solrIndexId: String) = authActionFactory.getAuthenticatedAction(Action).async {
    Future {
      // TODO handle exception, give API_RESULT_FAIL
      try {
        searchManagementRepository.deleteSolrIndex(solrIndexId)
        Ok(Json.toJson(ApiResult(API_RESULT_OK, "Deleting Solr Index successful", None)))
      } catch {
        case e: Exception => BadRequest(
            Json.toJson(
              ApiResult(API_RESULT_FAIL, s"Deleting Solr Index failed: ${e.getMessage}", None)
            )
          )
        case _: Throwable => BadRequest(
          Json.toJson(
            ApiResult(API_RESULT_FAIL, s"Deleting Solr Index failed due to an unknown error", None)
          )
        )
      }
    }
  }

  //CJM 8
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
      logger.debug("addNewSearchInput")
      val userInfo: Option[String] = lookupUserInfo(request)

      val body: AnyContent = request.body
      val jsonBody: Option[JsValue] = body.asJson

      // Expecting json body
      jsonBody.map { json =>
        val searchInputTerm = (json \ "term").as[String]
        val tags = (json \ "tags").as[Seq[String]].map(InputTagId(_))

        InputValidator.validateInputTerm(searchInputTerm) match {
          case Nil => {
            val searchInputId = searchManagementRepository.addNewSearchInput(SolrIndexId(solrIndexId), searchInputTerm, tags, userInfo)
            Ok(Json.toJson(ApiResult(API_RESULT_OK, "Adding Search Input '" + searchInputTerm + "' successful.", Some(searchInputId))))
          }
          case errors => {
            val msgs = s"Failed to add new Search Input ${searchInputTerm}: " + errors.mkString("\n")
            logger.error(msgs)
            BadRequest(Json.toJson(ApiResult(API_RESULT_FAIL, msgs, None)))
          }
        }
      }.getOrElse {
        BadRequest(Json.toJson(ApiResult(API_RESULT_FAIL, "Adding new Search Input failed. Unexpected body data.", None)))
      }
    }
  }



  def updateSearchInput(searchInputId: String) = authActionFactory.getAuthenticatedAction(Action) { request: Request[AnyContent] =>
    logger.debug("updateSearchInput:1")
    var body: AnyContent = request.body
    //var jsonString: String = "{\"id\":\"ab116147-498a-427e-9481-9565739aa706\",\"term\":\"synonym1\",\"synonymRules\":[{\"id\":\"e7ab9b28-e6af-48dc-af7f-224b00381e62\",\"synonymType\":1,\"term\":\"synonym2\",\"isActive\":true,\"status\":1,\"lastUpdate\":\"2005\"}],\"upDownRules\":[],\"filterRules\":[],\"deleteRules\":[],\"redirectRules\":[],\"tags\":[],\"isActive\":true,\"comment\":\"synonym3\"}"
    //val jsVal: JsValue = Json.parse(jsonString)
    //body = AnyContentAsJson(jsVal)
    val jsonBody: Option[JsValue] = body.asJson
    val userInfo: Option[String] = lookupUserInfo(request)
    logger.debug("updateSearchInput:2")
    logger.debug("updateSearchInput:body")
    logger.debug(body.toString)
    logger.debug("updateSearchInput:jsonBody")
    logger.debug(jsonBody.toString)
    // Expecting json body
    jsonBody.map { json =>
      logger.debug("updateSearchInput:3")
      val searchInput = json.as[SearchInputWithRules]
      logger.debug("updateSearchInput:4")
      InputValidator.validateInputTerm(searchInput.term) match {
        case Nil => {
          logger.debug("updateSearchInput:5")
          // proceed updating input with rules
          querqyRulesTxtGenerator.validateSearchInputToErrMsg(searchInput) match {
            case Some(strErrMsg: String) =>
              logger.error("updateSearchInput failed on validation of searchInput with id " + searchInputId + " - validation returned the following error output: <<<" + strErrMsg + ">>>")
              BadRequest(Json.toJson(ApiResult(API_RESULT_FAIL, strErrMsg, None)))
            case None => {
              // TODO handle potential conflict between searchInputId and JSON-passed searchInput.id
              searchManagementRepository.updateSearchInput(searchInput, userInfo)
              // TODO consider Update returning the updated SearchInput(...) instead of an ApiResult(...)
              Ok(Json.toJson(ApiResult(API_RESULT_OK, "Updating Search Input successful.", Some(SearchInputId(searchInputId)))))
            }
          }
        }
        case errors => {
          logger.debug("updateSearchInput:6")
          val msgs = s"Failed to update Search Input with new term ${searchInput.term}: " + errors.mkString("\n")
          logger.error(msgs)
          BadRequest(Json.toJson(ApiResult(API_RESULT_FAIL, msgs, None)))
        }
      }

    }.getOrElse {
      logger.debug("updateSearchInput:7")
      BadRequest(Json.toJson(ApiResult(API_RESULT_FAIL, "Adding new Search Input failed. Unexpected body data.", None)))
    }
  }

  def deleteSearchInput(searchInputId: String) = authActionFactory.getAuthenticatedAction(Action).async { request: Request[AnyContent] =>
    Future {
      val userInfo: Option[String] = lookupUserInfo(request)
      searchManagementRepository.deleteSearchInput(searchInputId, userInfo)
      Ok(Json.toJson(ApiResult(API_RESULT_OK, "Deleting Search Input successful", None)))
    }
  }

  def listAll(solrIndexId: String) = authActionFactory.getAuthenticatedAction(Action) {
    val searchInputs = searchManagementRepository.listAllSearchInputsInclDirectedSynonyms(SolrIndexId(solrIndexId))
    val spellings = searchManagementRepository.listAllSpellingsWithAlternatives(SolrIndexId(solrIndexId))
    Ok(Json.toJson(ListItem.create(searchInputs, spellings)))
  }

//  def listAll2(solrIndexId: String) = authActionFactory.getAuthenticatedAction(Action) {
//    //val searchInputs = searchManagementRepository.listAllSearchInputsInclDirectedSynonyms(SolrIndexId(solrIndexId))
//    //val searchInputs = searchManagementRepository.listAllSearchInputsInclDirectedSynonyms(SolrIndexId(solrIndexId))
//    //val spellings = searchManagementRepository.listAllSpellingsWithAlternatives(SolrIndexId(solrIndexId))
//    Future {
//      this.getSolrIndex(solrIndexId)
//      Ok(Json.toJson(ApiResult(API_RESULT_OK, "Solr Index successful", None)))
//
//    }
//    //Ok(s);
//    //Ok(Json.toJson(x))
//  }

  def addNewSpelling(solrIndexId: String) = authActionFactory.getAuthenticatedAction(Action).async { request: Request[AnyContent] =>
    Future {
      val userInfo: Option[String] = lookupUserInfo(request)
      val body: AnyContent = request.body
      val jsonBody: Option[JsValue] = body.asJson

      val optTerm = jsonBody.flatMap(json => (json \"term").asOpt[String])
      optTerm.map { term =>
        CanonicalSpellingValidator.validateNoEmptySpelling(term) match {
          case None => {
            val canonicalSpelling = searchManagementRepository.addNewCanonicalSpelling(SolrIndexId(solrIndexId), term, userInfo)
            Ok(Json.toJson(ApiResult(API_RESULT_OK, "Successfully added Canonical Spelling '" + term + "'.", Some(canonicalSpelling.id))))
          }
          case Some(error) => {
            BadRequest(Json.toJson(ApiResult(API_RESULT_FAIL, error, None)))
          }
        }
      }.getOrElse {
        BadRequest(Json.toJson(ApiResult(API_RESULT_FAIL, "Adding new Canonical Spelling failed. Unexpected body data.", None)))
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
    val userInfo: Option[String] = lookupUserInfo(request)
    val body: AnyContent = request.body
    val jsonBody: Option[JsValue] = body.asJson

    // Expecting json body
    jsonBody.map { json =>
      val spellingWithAlternatives = json.as[CanonicalSpellingWithAlternatives]

      val otherSpellings = searchManagementRepository.listAllSpellingsWithAlternatives(SolrIndexId(solrIndexId)).filter(_.id != spellingWithAlternatives.id)
      CanonicalSpellingValidator.validateCanonicalSpellingsAndAlternatives(spellingWithAlternatives, otherSpellings) match {
        case Nil =>
          searchManagementRepository.updateSpelling(spellingWithAlternatives, userInfo)
          Ok(Json.toJson(ApiResult(API_RESULT_OK, "Successfully updated Canonical Spelling.", Some(CanonicalSpellingId(canonicalSpellingId)))))
        case errors =>
          val msgs = s"Failed to update Canonical Spelling ${spellingWithAlternatives.term}: " + errors.mkString("\n")
          logger.error(msgs)
          BadRequest(Json.toJson(ApiResult(API_RESULT_FAIL, msgs, None)))
      }
    }.getOrElse {
      BadRequest(Json.toJson(ApiResult(API_RESULT_FAIL, "Updating Canonical Spelling failed. Unexpected body data.", None)))
    }
  }
  def deleteSpelling(canonicalSpellingId: String) = authActionFactory.getAuthenticatedAction(Action).async { request: Request[AnyContent] =>
    Future {
      val userInfo: Option[String] = lookupUserInfo(request)
      searchManagementRepository.deleteSpelling(canonicalSpellingId, userInfo)
      Ok(Json.toJson(ApiResult(API_RESULT_OK, "Deleting Canonical Spelling with alternatives successful.", None)))
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
              ApiResult(API_RESULT_FAIL, s"Updating Search Management Config for Solr Index failed.\nScript output:\n${result.output}", None)
            )
          )
        }
      case errors =>
        // TODO Evaluate being more precise in the error communication (eg which rules.txt failed?, where? / which line?, why?, etc.)
        BadRequest(
          Json.toJson(
            ApiResult(API_RESULT_FAIL, s"Updating Search Management Config for Solr Index failed. Validation errors in rules.txt:\n${errors.mkString("\n")}", None)
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

  def addNewSuggestedSolrField(solrIndexId: String) = authActionFactory.getAuthenticatedAction(Action).async { request: Request[AnyContent] =>
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

  // I am requiring the solrIndexId because it is more RESTful, but it turns out we don't need it.
  // Maybe validation some day?
  def deleteSuggestedSolrField(solrIndexId: String, suggestedFieldId: String) = authActionFactory.getAuthenticatedAction(Action).async { request: Request[AnyContent] =>
    Future {
      searchManagementRepository.deleteSuggestedSolrField(SuggestedSolrFieldId(suggestedFieldId))
      Ok(Json.toJson(ApiResult(API_RESULT_OK, "Deleting Suggested Field successful", None)))
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
        val bufferedSource = scala.io.Source.fromFile(tmp_file_path)
        val filePayload = bufferedSource.getLines.mkString("\n")
        try {
          val importStatistics = rulesTxtImportService.importFromFilePayload(filePayload, SolrIndexId(solrIndexId))
          val apiResultMsg = "Import from rules.txt file successful with following statistics:\n" +
            "^-- count rules.txt inputs = " + importStatistics._1 + "\n" +
            "^-- count rules.txt lines skipped = " + importStatistics._2 + "\n" +
            "^-- count rules.txt unknown convert = " + importStatistics._3 + "\n" +
            "^-- count consolidated inputs (after rev engineering undirected synonyms) = " + importStatistics._4 + "\n" +
            "^-- count total rules after consolidation = " + importStatistics._5 + "\n"

          Ok(Json.toJson(ApiResult(API_RESULT_OK, apiResultMsg, None)))
        } catch {
          case e: Exception => {
            Ok(Json.toJson(ApiResult(API_RESULT_FAIL, e.getMessage(), None)))
          }
        } finally {
          bufferedSource.close()
        }

      }
      .getOrElse {
        Ok(Json.toJson(ApiResult(API_RESULT_FAIL, "File rules_txt missing in request body.", None)))
      }
  }
  private def lookupUserInfo(request: Request[AnyContent]) = {
    val userInfo: Option[String] = request match {
      case _: UserRequest[A] => Option(request.asInstanceOf[UserRequest[A]].username)
      case _ => None
    }
    userInfo
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
        if (isRawRequested) {
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

      logger.info(s":: SMUI version of this instance: ($current)")

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

        val (infoType, msgHtml) = (if (latestFromDockerHub.get.greaterThan(current.get)) {
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

  def getDatabaseJsonWithId(id: String): Action[AnyContent] = authActionFactory.getAuthenticatedAction(Action).async {
    Future {
      logger.debug("In ApiController:getDatabaseJsonWithId and got id: " + id)
      Ok(Json.toJson(searchManagementRepository.getDatabaseJsonWithId(id)))
    }
  }

  def uploadImport: Action[MultipartFormData[Files.TemporaryFile]] = authActionFactory.getAuthenticatedAction(Action).async(parse.multipartFormData) { implicit request =>
    Future {
      val tryDatabaseStuff: Boolean = true
      logger.debug("In ApiController:uploadImport")
      if (request.body.files.size == 1) {

        val fileName: String = request.body.files.head.filename
        logger.debug(fileName)
        import java.nio.file.Files
        val content = Files.readString(temporaryFileToPath(request.body.files.head.ref))
        logger.debug(content)
        val validatedImport: ValidatedImportData = new ValidatedImportData(fileName, content)
        if (tryDatabaseStuff) {
          searchManagementRepository.doImport(validatedImport)
        }
        Ok(Json.toJson(ApiResult(API_RESULT_OK, "Got file.", None)))
      } else {
        BadRequest("Only one upload file is allowed. Input must be valid")
      }
    }
  }

//  def putty: Action[AnyContent] = authActionFactory.getAuthenticatedAction(Action).async {
//    Future {
//      logger.debug("ApiController.putty():1")
//      searchManagementRepository.putty
//      logger.debug("ApiController.putty():2")
//      Ok(Json.toJson(ApiResult(API_RESULT_OK, "That worked.", None)))
//    }
//  }

  def putty: Action[AnyContent] = authActionFactory.getAuthenticatedAction(Action).async {
    Future {
      logger.debug("In ApiController:putty")
      val content = "[{\"tableName\":\"solr_index\",\"columns\":[\"id\",\"name\",\"description\",\"last_update\"],\"rows\":[[\"b0eecea6-efa7-4575-9bb4-acba1aab146b\",\"b0eecea6-efa7-4575-9bb4-acba1aab146b\",\"test\",\"2022-05-31T11:14:38\"]]},{\"tableName\":\"search_input\",\"columns\":[\"id\",\"term\",\"solr_index_id\",\"last_update\",\"status\",\"comment\"],\"rows\":[[\"16c30efd-3139-4916-bfb6-57463af18250\",\"test\",\"b0eecea6-efa7-4575-9bb4-acba1aab146b\",\"2022-05-31T17:25:25\",1,\"updown comment\"],[\"5418428c-0d4c-4464-a2a6-084f264be360\",\"s\",\"b0eecea6-efa7-4575-9bb4-acba1aab146b\",\"2022-05-31T15:29:59\",1,\"syn com\"],[\"70823642-e7c6-4857-9d6c-a54b3c382f0d\",\"test\",\"b0eecea6-efa7-4575-9bb4-acba1aab146b\",\"2022-05-31T14:43:54\",1,\"\"],[\"89c10061-26d9-4b5f-9e99-92696cc5da74\",\"test two three\",\"b0eecea6-efa7-4575-9bb4-acba1aab146b\",\"2022-05-31T13:43:50\",1,\"a comment\"],[\"9fb7f8b4-5544-4df0-9d08-d485a0145dbe\",\"redirect\",\"b0eecea6-efa7-4575-9bb4-acba1aab146b\",\"2022-05-31T17:20:36\",1,\"redirect comment\"],[\"ccc48739-f192-44b1-b552-995eed4a0a51\",\"all\",\"b0eecea6-efa7-4575-9bb4-acba1aab146b\",\"2022-05-31T14:41:55\",1,\"\"],[\"dd1bd496-90ed-43ad-9895-e67a4f67adeb\",\"test1\",\"b0eecea6-efa7-4575-9bb4-acba1aab146b\",\"2022-05-31T13:54:27\",1,\"\"],[\"e8064dd4-0e76-4e0b-963a-06ea8cae65e2\",\"t345\",\"b0eecea6-efa7-4575-9bb4-acba1aab146b\",\"2022-05-31T14:56:51\",1,\"c\"]]},{\"tableName\":\"redirect_rule\",\"columns\":[\"id\",\"target\",\"search_input_id\",\"last_update\",\"status\"],\"rows\":[[\"89e5833a-64b4-4a97-924b-a18b66694437\",\"https://www.google.com\",\"9fb7f8b4-5544-4df0-9d08-d485a0145dbe\",\"2022-05-31T17:20:36\",1]]},{\"tableName\":\"synonym_rule\",\"columns\":[\"id\",\"synonymType\",\"term\",\"search_input_id\",\"last_update\",\"status\"],\"rows\":[[\"70d3eb55-ad67-4890-a157-130ea72637c1\",0,\"y\",\"5418428c-0d4c-4464-a2a6-084f264be360\",\"2022-05-31T15:29:59\",1]]},{\"tableName\":\"up_down_rule\",\"columns\":[\"id\",\"up_down_type\",\"boost_malus_type\",\"term\",\"search_input_id\",\"last_update\",\"status\"],\"rows\":[[\"a26f49ad-28ba-40e3-a968-ada168d948c7\",0,5,\"* a:test\",1,\"16c30efd-3139-4916-bfb6-57463af18250\",\"2022-05-31T17:25:25\"]]},{\"tableName\":\"delete_rule\",\"columns\":[\"id\",\"term\",\"search_input_id\",\"last_update\",\"status\"],\"rows\":[[\"36d7a7d2-4133-4bcc-b4b3-19ec6d0404d1\",\"two\",\"89c10061-26d9-4b5f-9e99-92696cc5da74\",\"2022-05-31T13:43:50\",1]]},{\"tableName\":\"filter_rule\",\"columns\":[\"id\",\"term\",\"search_input_id\",\"last_update\",\"status\"],\"rows\":[[\"8ed6c4bd-ac69-4a94-898c-fabb13a7fc47\",\"* test:* a:b\",\"e8064dd4-0e76-4e0b-963a-06ea8cae65e2\",\"2022-05-31T14:56:51\",1]]},{\"tableName\":\"suggested_solr_field\",\"columns\":[\"id\",\"name\",\"solr_index_id\",\"last_update\"],\"rows\":[[\"4ce83b3a-7263-4873-b4f2-a66a9321fdbb\",\"test\",\"b0eecea6-efa7-4575-9bb4-acba1aab146b\",\"2022-05-31T17:37:43\"]]},{\"tableName\":\"input_tag\",\"columns\":[\"id\",\"solr_index_id\",\"property\",\"tag_value\",\"exported\",\"predefined\",\"last_update\"],\"rows\":[[\"wh\",\"some solr_index_id\",\"some property\",\"some tag_value\",2345,123,\"2022-05-31T18:23:47\"]]},{\"tableName\":\"tag_2_input\",\"columns\":[\"id\",\"searchInputId\",\"last_update\"],\"rows\":[[\"hi\",\"3\",\"2022-05-31T18:22:14\"]]},{\"tableName\":\"canonical_spelling\",\"columns\":[\"id\",\"solr_index_id\",\"term\",\"status\",\"comment\",\"last_update\"],\"rows\":[[\"id8\",\"id9\",\"a_term_can_spell\",0,\"can_spell_comment\",\"2022-05-31T18:44:15\"]]},{\"tableName\":\"alternative_spelling\",\"columns\":[\"id\",\"canonical_spelling_id\",\"term\",\"status\",\"last_update\"],\"rows\":[[\"id10\",\"id11\",\"alt_spell_term\",0,\"2022-05-31T18:44:15\"]]}]"
      val validatedImport: ValidatedImportData = new ValidatedImportData("unknown.txt", content)
      searchManagementRepository.doImport(validatedImport)
      Ok(Json.toJson(ApiResult(API_RESULT_OK, "OK.", None)))
    }
  }

}
