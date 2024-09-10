package controllers

import java.io.{OutputStream, PipedInputStream, PipedOutputStream}
import org.apache.pekko.stream.scaladsl.{Source, StreamConverters}
import org.apache.pekko.util.ByteString

import javax.inject.Inject
import play.api.Logging
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.json.Reads._

import java.nio.file.Paths
import java.time.format.DateTimeFormatter
import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}
import models.FeatureToggleModel.FeatureToggleService
import models._
import models.config.SmuiVersion
import models.config.TargetEnvironment._
import models.input.{InputTagId, InputValidator, ListItem, SearchInputId, SearchInputWithRules}
import models.querqy.QuerqyRulesTxtGenerator
import models.reports.RulesUsageReport
import models.rules.{DeleteRule, FilterRule, RedirectRule, SynonymRule, UpDownRule}
import models.spellings.{CanonicalSpellingId, CanonicalSpellingValidator, CanonicalSpellingWithAlternatives}
import org.pac4j.core.profile.UserProfile
import org.pac4j.play.context.PlayFrameworkParameters
import org.pac4j.play.scala.{Security, SecurityComponents}
import play.api.libs.Files
import scala.jdk.CollectionConverters.ListHasAsScala
import services.{RulesTxtDeploymentService, RulesTxtImportService, RulesUsageService}


// TODO Make ApiController pure REST- / JSON-Controller to ensure all implicit Framework responses (e.g. 400, 500) conformity
class ApiController @Inject()(val controllerComponents: SecurityComponents,
                              featureToggleService: FeatureToggleService,
                              searchManagementRepository: SearchManagementRepository,
                              querqyRulesTxtGenerator: QuerqyRulesTxtGenerator,
                              rulesTxtDeploymentService: RulesTxtDeploymentService,
                              rulesTxtImportService: RulesTxtImportService,
                              targetEnvironmentConfigService: TargetEnvironmentConfigService,
                              rulesUsageService: RulesUsageService)
                             (implicit executionContext: ExecutionContext)
  extends Security[UserProfile] with play.api.i18n.I18nSupport with Logging {

  val API_RESULT_OK = "OK"
  val API_RESULT_FAIL = "KO"

  case class ApiResult(result: String, message: String, returnId: Option[Id])

  implicit val apiResultWrites: OWrites[ApiResult] = Json.writes[ApiResult]

  def getFeatureToggles: Action[AnyContent] = Action {
    Ok(Json.toJson(featureToggleService.getJsFrontendToggleList))
  }

  def listAllSolrIndeces: Action[AnyContent] = Action {
    Ok(Json.toJson(searchManagementRepository.listAllSolrIndexes))
  }

  def addNewSolrIndex: Action[AnyContent] = Action { request: Request[AnyContent] =>
    val body: AnyContent = request.body
    val jsonBody: Option[JsValue] = body.asJson

    // Expecting json body
    jsonBody.map { json =>
      val searchIndexName = (json \ "name").as[String]
      val searchIndexDescription = (json \ "description").as[String]
      val solrIndexId = searchManagementRepository.addNewSolrIndex(
        SolrIndex(name = searchIndexName, description = searchIndexDescription)
      )

      Ok(Json.toJson(ApiResult(API_RESULT_OK, "Successfully added Deployment Channel '" + searchIndexName + "'.", Some(solrIndexId))))
    }.getOrElse {
      BadRequest(Json.toJson(ApiResult(API_RESULT_FAIL, "Adding new Deployment Channel failed. Unexpected body data.", None)))
    }
  }

  def getSolrIndex(solrIndexId: String): Action[AnyContent] = Action.async {
    Future {
      Ok(Json.toJson(searchManagementRepository.getSolrIndex(SolrIndexId(solrIndexId))))
    }
  }

  def deleteSolrIndex(solrIndexId: String): Action[AnyContent] = Action.async {
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

  def downloadAllRulesTxtFiles: Action[AnyContent] = Action {
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
  def listAllSearchInputs(solrIndexId: String): Action[AnyContent] = Action {
    // TODO add error handling (database connection, other exceptions)
    Ok(Json.toJson(searchManagementRepository.listAllSearchInputsInclDirectedSynonyms(SolrIndexId(solrIndexId))))
  }

  def listAllInputTags(): Action[AnyContent] = Action {
    Ok(Json.toJson(searchManagementRepository.listAllInputTags()))
  }

  def getDetailedSearchInput(searchInputId: String): Action[AnyContent] = Action {
    // TODO add error handling (database connection, other exceptions)
    Ok(Json.toJson(searchManagementRepository.getDetailedSearchInput(SearchInputId(searchInputId))))
  }


  def addNewSearchInput(solrIndexId: String): Action[AnyContent] = Action.async { request: Request[AnyContent] =>
    Future {
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



  def updateSearchInput(searchInputId: String): Action[AnyContent] = Action { request: Request[AnyContent] =>
    val body: AnyContent = request.body
    val jsonBody: Option[JsValue] = body.asJson
    val userInfo: Option[String] = lookupUserInfo(request)

    // Expecting json body
    jsonBody.map { json =>
      val searchInput = json.as[SearchInputWithRules]

      InputValidator.validateInputTerm(searchInput.term) match {
        case Nil => {
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
          val msgs = s"Failed to update Search Input with new term ${searchInput.term}: " + errors.mkString("\n")
          logger.error(msgs)
          BadRequest(Json.toJson(ApiResult(API_RESULT_FAIL, msgs, None)))
        }
      }

    }.getOrElse {
      BadRequest(Json.toJson(ApiResult(API_RESULT_FAIL, "Adding new Search Input failed. Unexpected body data.", None)))
    }
  }

  def deleteSearchInput(searchInputId: String): Action[AnyContent] = Action.async { request: Request[AnyContent] =>
    Future {
      val userInfo: Option[String] = lookupUserInfo(request)
      searchManagementRepository.deleteSearchInput(searchInputId, userInfo)
      Ok(Json.toJson(ApiResult(API_RESULT_OK, "Deleting Search Input successful", None)))
    }
  }

  def copySearchInput(searchInputId: String, solrIndexId: String): Action[AnyContent] = Action.async { request: Request[AnyContent] =>
    Future {
      val userInfo: Option[String] = lookupUserInfo(request)

      try {
        val sourceSearchInputWithRules = searchManagementRepository.getDetailedSearchInput(SearchInputId(searchInputId))
        // TODO Make transactional to rollback in case of error. BUT it's not crucial since there is just a copy created and the user can recover easily using the UI in case it could not finish
        val copySearchInputId = searchManagementRepository.addNewSearchInput(
          SolrIndexId(solrIndexId),
          sourceSearchInputWithRules.get.term,
          sourceSearchInputWithRules.get.tags.map(x => x.id),
          userInfo)

        val copySearchInputWithRules =
          SearchInputWithRules(copySearchInputId,
            term = sourceSearchInputWithRules.get.term,
            synonymRules = sourceSearchInputWithRules.get.synonymRules.map(x => SynonymRule.createWithNewIdFrom(x)),
            upDownRules = sourceSearchInputWithRules.get.upDownRules.map(x => UpDownRule.createWithNewIdFrom(x)),
            filterRules = sourceSearchInputWithRules.get.filterRules.map(x => FilterRule.createWithNewIdFrom(x)),
            deleteRules = sourceSearchInputWithRules.get.deleteRules.map(x => DeleteRule.createWithNewIdFrom(x)),
            redirectRules = sourceSearchInputWithRules.get.redirectRules.map(x => RedirectRule.createWithNewIdFrom(x)),
            tags = sourceSearchInputWithRules.get.tags,
            isActive = sourceSearchInputWithRules.get.isActive,
            comment = sourceSearchInputWithRules.get.comment);

        searchManagementRepository.updateSearchInput(copySearchInputWithRules, userInfo)

        Ok(Json.toJson(ApiResult(API_RESULT_OK, "Copying Search Input successful", Some(copySearchInputId))))

      } catch {
        case e: Exception => BadRequest(
          Json.toJson(ApiResult(API_RESULT_FAIL, s"Copying Search Input failed: ${e.getMessage}", None))
        )
        case _: Throwable => BadRequest(
          Json.toJson(ApiResult(API_RESULT_FAIL, s"Copying Search Input failed due to an unknown error", None))
        )
      }
    }
  }

  def listAll(solrIndexId: String) : Action[AnyContent] = Action {
    val searchInputs = searchManagementRepository.listAllSearchInputsInclDirectedSynonyms(SolrIndexId(solrIndexId))
    val spellings = searchManagementRepository.listAllSpellingsWithAlternatives(SolrIndexId(solrIndexId))
    Ok(Json.toJson(ListItem.create(searchInputs, spellings, rulesUsageService.getRulesUsageStatistics)))
  }

  def addNewSpelling(solrIndexId: String): Action[AnyContent] = Action.async { request: Request[AnyContent] =>
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

  def getDetailedSpelling(canonicalSpellingId: String): Action[AnyContent] = Action.async {
    Future {
      val spellingWithAlternatives = searchManagementRepository.getDetailedSpelling(canonicalSpellingId)
      Ok(Json.toJson(spellingWithAlternatives))
    }
  }

  def updateSpelling(solrIndexId: String, canonicalSpellingId: String): Action[AnyContent] = Action { request: Request[AnyContent] =>
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
  def deleteSpelling(canonicalSpellingId: String): Action[AnyContent] = Action.async { request: Request[AnyContent] =>
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
  def updateRulesTxtForSolrIndexAndTargetPlatform(solrIndexId: String, targetSystem: String): Action[AnyContent] = Action {
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

  def listAllSuggestedSolrFields(solrIndexId: String): Action[AnyContent] = Action.async {
    Future {
      // TODO add error handling (database connection, other exceptions)
      Ok(Json.toJson(searchManagementRepository.listAllSuggestedSolrFields(solrIndexId)))
    }
  }

  def addNewSuggestedSolrField(solrIndexId: String): Action[AnyContent] = Action.async { request: Request[AnyContent] =>
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
  def deleteSuggestedSolrField(solrIndexId: String, suggestedFieldId: String): Action[AnyContent] = Action.async { request: Request[AnyContent] =>
    Future {
      searchManagementRepository.deleteSuggestedSolrField(SuggestedSolrFieldId(suggestedFieldId))
      Ok(Json.toJson(ApiResult(API_RESULT_OK, "Deleting Suggested Field successful", None)))
    }
  }

  // TODO consider making method .asynch
  def importFromRulesTxt(solrIndexId: String): Action[MultipartFormData[Files.TemporaryFile]] = Action(parse.multipartFormData) { request =>
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
        val filePayload = bufferedSource.getLines().mkString("\n")
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

  /**
   * Deployment info (raw or formatted)
   */

  case class DeploymentInfo(msg: Option[String])

  implicit val logDeploymentInfoWrites: OWrites[DeploymentInfo] = Json.writes[DeploymentInfo]

  @deprecated("The old style of retrieving a deployment log summary as plain text will be removed", "SMUI version > 3.15.1")
  def getLatestDeploymentResultV1(solrIndexId: String, targetSystem: String): Action[AnyContent] = Action.async { request: Request[AnyContent] =>
    Future {
      logger.debug("In ApiController :: getLatestDeploymentResultV1")
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

  case class DeploymentDetailedInfo(targetSystem: String, formattedDateTime: String, result: Int)

  implicit val logDeploymentDetailedInfoWrites: OWrites[DeploymentDetailedInfo] = Json.writes[DeploymentDetailedInfo]

  def getLatestDeploymentResult(solrIndexId: String): Action[AnyContent] = Action.async { request: Request[AnyContent] =>
    Future {
      logger.debug("In ApiController :: getLatestDeploymentResult")
      logger.debug(s"... solrIndexId = $solrIndexId")

      def readDeploymentDetailedInfo(forTargetSystem: String) = {
        searchManagementRepository.lastDeploymentLogDetail(solrIndexId, forTargetSystem) match {
          case Some(deplLogDetailRaw) => DeploymentDetailedInfo(
                forTargetSystem,
                deplLogDetailRaw.lastUpdate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                deplLogDetailRaw.result
          )
          case None => DeploymentDetailedInfo(
              forTargetSystem,
              "<not yet deployed>",
              0
          )
        }
      }

      val deplLogDetailList = if (featureToggleService.isSmuiRuleDeploymentPrelivePresent)
          List(
            readDeploymentDetailedInfo("LIVE"),
            readDeploymentDetailedInfo("PRELIVE")
          )
        else
          List(
            readDeploymentDetailedInfo("LIVE")
          )
      
      Ok(Json.toJson( deplLogDetailList ))
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

  implicit val smuiVersionInfoWrites: OWrites[SmuiVersionInfo] = Json.writes[SmuiVersionInfo]

  // TODO consider outsourcing this "business logic" into the (config) model
  def getLatestVersionInfo() = Action.async {
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
              "<div>Your locally installed <strong>SMUI instance is outdated</strong>. Please consider an update. If you have issues, contact the maintainer (<a href=\"mailto:hello@productful.io\">hello@productful.io</a>) or file an issue to the project: <a href=\"https://github.com/querqy/smui/issues\" target=\"_new\">https://github.com/querqy/smui/issues</a><div>"
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

  def getTargetEnvironment() = Action.async {
    Future {

      val targetEnvEnf = targetEnvironmentConfigService.read

      logger.info("In ApiController :: getTargetEnvironment :: targetEnvironmentConfigService = " + targetEnvEnf.toString)

      Ok(
        Json.toJson(
          targetEnvEnf
        )
      )
    }
  }

  /**
   * Activity log
   */

  def getActivityLog(inputId: String) = Action.async {
    Future {
      val activityLog = searchManagementRepository.getInputRuleActivityLog(inputId)
      Ok(Json.toJson(activityLog))
    }
  }

  /**
   * Reports (for Activity log as well)
   */

  def getRulesReport(solrIndexId: String) = Action.async {
    Future {
      val report = searchManagementRepository.getRulesReport(SolrIndexId(solrIndexId))
      Ok(Json.toJson(report))
    }
  }

  def getActivityReport(solrIndexId: String) = Action.async { request: Request[AnyContent] => {
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

  def getRulesUsageReport(solrIndexId: String): Action[AnyContent] = Action {
    rulesUsageService.getRulesUsageStatistics.map { ruleUsageStatistics =>
      val allSearchInputs = searchManagementRepository.listAllSearchInputsInclDirectedSynonyms(SolrIndexId(solrIndexId))
      val report = RulesUsageReport.create(allSearchInputs, ruleUsageStatistics)
      Ok(Json.toJson(report))
    }.getOrElse(
      NoContent
    )
  }

  private def lookupUserInfo(request: Request[AnyContent]) = {
    val maybeUserId = getProfiles(request).headOption.map(_.getId)
    logger.debug(s"Current user: $maybeUserId")
    maybeUserId
  }

  private def getProfiles(request: RequestHeader): List[UserProfile] = {
    val parameters = new PlayFrameworkParameters(request)
    val webContext = controllerComponents.config.getWebContextFactory.newContext(parameters)
    val sessionStore = controllerComponents.config.getSessionStoreFactory.newSessionStore(parameters)
    val profileManager = controllerComponents.config.getProfileManagerFactory.apply(webContext, sessionStore)
    profileManager.getProfiles.asScala.toList
  }

}
