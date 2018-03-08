package controllers

import java.io.StringReader
import javax.inject.Inject

import models._
import play.api.data.Forms._
import play.api.data._
import play.api.i18n._
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.Play

import sys.process._

import scala.concurrent.{ExecutionContext, Future}
import models.SearchManagementModel._
// TODO evaluate encapsulating querqy validation to a own "models"-class or into models.SearchManagementRepository
import querqy.rewrite.commonrules.SimpleCommonRulesParser
import querqy.parser.WhiteSpaceQuerqyParserFactory

// TODO Make ApiController pure REST- / JSON-Controller to ensure all implicit Framework responses (e.g. 400, 500) conformity
class ApiController @Inject()(searchManagementRepository: SearchManagementRepository,
                              querqyRulesTxtGenerator: QuerqyRulesTxtGenerator,
                              cc: MessagesControllerComponents)(implicit executionContext: ExecutionContext)
  extends MessagesAbstractController(cc) {

  private val logger = play.api.Logger;

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

  def listAllSolrIndeces = Action.async {
    Future {
      Ok( Json.toJson(searchManagementRepository.listAllSolrIndeces) );
    }
  }

  def listAllSearchInputs(solrIndexId: Long) = Action.async {
    Future {
      // TODO add error handling (database connection, other exceptions)
      Ok( Json.toJson(searchManagementRepository.listAllSearchInputsInclDirectedSynonyms(solrIndexId)) );

      /*
      TODO remove test data output

      Ok( Json.toJson(List[SearchInput](
        new SearchInput(Some(1), "arbeitsministerium", List[SynonymRule](
          new SynonymRule(Some(1), 0, "Bundesministerium für Arbeit und Soziales"),
          new SynonymRule(Some(2), 0, "BMAS"),
          new SynonymRule(Some(3), 1, "arbeit ministerium")
        ), List[UpDownRule](), List[FilterRule](), List[DeleteRule]()),
        new SearchInput(Some(2), "Betriebsverfassungsgesetz", List[SynonymRule](), List[UpDownRule](), List[FilterRule](), List[DeleteRule]()),
        new SearchInput(Some(3), "FlexÜ", List[SynonymRule](), List[UpDownRule](), List[FilterRule](), List[DeleteRule]()),
        new SearchInput(Some(4), "manteltarifvertrag", List[SynonymRule](), List[UpDownRule](), List[FilterRule](), List[DeleteRule]()),
      )) )
      */
    }
  }

  def getDetailedSearchInput(searchInputId: Long) = Action.async {
    Future {
      // TODO add error handling (database connection, other exceptions)
      Ok( Json.toJson(searchManagementRepository.getDetailedSearchInput(searchInputId)) )

      /*
      TODO remove test data output

      Ok(Json.toJson(new SearchInput(Some(1), "arbeitsministerium", List[SynonymRule](
        new SynonymRule(Some(1), 0, "Bundesministerium für Arbeit und Soziales"),
        new SynonymRule(Some(2), 0, "BMAS"),
        new SynonymRule(Some(3), 1, "arbeit ministerium")
      ), List[UpDownRule](), List[FilterRule](), List[DeleteRule]())))
      */
    }
  }

  def addNewSearchInput(solrIndexId: Long) = Action.async { request: Request[AnyContent] =>
    Future {
      val body: AnyContent = request.body
      val jsonBody: Option[JsValue] = body.asJson

      // Expecting json body
      jsonBody.map { json =>
        val searchInputTerm = (json \ "term").as[String]
        val maybeSearchInputId = searchManagementRepository.addNewSearchInput(solrIndexId, searchInputTerm)

        Ok( Json.toJson(new ApiResult(API_RESULT_OK, "Adding Search Input '" + searchInputTerm + "' successful.", maybeSearchInputId)) )
      }.getOrElse {
        BadRequest( Json.toJson(new ApiResult(API_RESULT_FAIL, "Adding new Search Input failed. Unexpected body data.", None)) )
      }
    }
  }

  private def validateSearchInputToErrMsg(searchInput: SearchInput): Option[String] = {

    // TODO validation ends with first broken rule, it should collect all errors to a line.
    // TODO decide, if input having no rule at all is legit ... (e.g. newly created). Will currently being filtered.

    // validate against SMUI rules (TODO outsource in separated method)

    // if input contains *-Wildcard, all synonyms must be directed
    // TODO discuss if (1) contains or (2) endsWith is the right interpretation
    if(searchInput.term.trim().contains("*")) {
      if(searchInput.synonymRules.filter(r => r.synonymType == 0).size > 0) {
        logger.error("Parsing Search Input: Wildcard *-using input ('" + searchInput.term + "') has undirected synonym rule");
        return Some("Wildcard *-using input ('\" + searchInput.term + \"') has undirected synonym rule");
      }
    }

    // undirected synonyms must not contain *-Wildcard
    if(searchInput.synonymRules.filter(r => r.synonymType == 0 && r.term.trim.contains("*")).size > 0) {
      logger.error("Parsing Search Input: Wildcard *-using undirected synonym for Input ('" + searchInput.term + "')");
      return Some("Parsing Search Input: Wildcard *-using undirected synonym for Input ('" + searchInput.term + "')");
    }

    // validate against querqy parser (TODO outsource in separated method)

    val singleSearchInputRule = querqyRulesTxtGenerator
      .renderSearchInputRulesForTerm(searchInput.term, searchInput);
    try {
      logger.debug("Parsing Search Input singleSearchInputRule = >>>" + singleSearchInputRule + "<<<");

      val simpleCommonRulesParser: SimpleCommonRulesParser = new SimpleCommonRulesParser(
        new StringReader(singleSearchInputRule),
        new WhiteSpaceQuerqyParserFactory(),
        true
      );
      simpleCommonRulesParser.parse();

      logger.debug("Parsing Search Input ok! simpleCommonRulesParser = " + simpleCommonRulesParser.toString());
      return None;
    } catch {
      case e: Exception => {
        logger.error("Parsing Search Input ended in Exception e.message = " + e.getMessage());
        return Some(e.getMessage());
      }
    }
  }

  def updateSearchInput(searchInputId: Long) = Action.async { request: Request[AnyContent] =>
    Future {

      val body: AnyContent = request.body
      val jsonBody: Option[JsValue] = body.asJson

      // Expecting json body
      jsonBody.map { json =>
        val searchInput = json.as[SearchInput]

        // TODO transport validation result via API
        validateSearchInputToErrMsg(searchInput);

        // TODO handle potential conflict between searchInputId and JSON-passed searchInput.id
        searchManagementRepository.updateSearchInput(searchInput);
        // TODO consider Update returning the updated SearchInput(...) instead of an ApiResult(...)
        Ok( Json.toJson(new ApiResult(API_RESULT_OK, "Updating Search Input successful.", Some(searchInputId))) );
      }.getOrElse {
        BadRequest( Json.toJson(new ApiResult(API_RESULT_FAIL, "Adding new Search Input failed. Unexpected body data.", None)) )
      }
    }
  }

  def deleteSearchInput(searchInputId: Long) = Action.async {
    Future {
      searchManagementRepository.deleteSearchInput(searchInputId);
      Ok( Json.toJson(new ApiResult(API_RESULT_OK, "Deleting Search Input successful", None)) );
    }
  }

  def updateRulesTxtForSolrIndex(solrIndexId: Long) = Action.async {
    Future {
      // Generate rules.txt first
      val strRulesTxt = querqyRulesTxtGenerator.render(solrIndexId);

      // TODO Replace through a proper Play Framework temp file implementation (take care of hard coded /tmp path then)
      val TEMP_FILENAME = "/tmp/search-management-ui_rules-txt.tmp";
      val tmpFile = new java.io.File(TEMP_FILENAME);
      tmpFile.createNewFile();
      val fw = new java.io.FileWriter(tmpFile);
      try {
        fw.write(strRulesTxt);
      }
      catch {
        case iox: java.io.IOException => logger.error( "IOException while writing /tmp file: " + iox.getStackTrace );
        case _: Throwable => logger.error("Got an unexpected error while writing /tmp file");
      }
      finally {
        fw.close();
      }

      logger.debug( ">>>" + strRulesTxt + "<<<" );

      val script = Play.current.path.getAbsolutePath() + "/conf/smui2solr.sh " +
        TEMP_FILENAME + " " +
        searchManagementRepository.getSolrIndexName(solrIndexId);
      val result = script !;
      logger.debug( "Script execution result: " + result );
      if (result == 0) {
        Ok( Json.toJson(new ApiResult(API_RESULT_OK, "Updating Search Management Config for Solr Index successful.", None)) );
      } else {
        BadRequest( Json.toJson(new ApiResult(API_RESULT_FAIL, "Updating Solr Index failed. Unexpected result in script execution.", None)) )
      }
    }
  }

  def listAllSuggestedSolrFields(solrIndexId: Long) = Action.async {
    Future {
      // TODO add error handling (database connection, other exceptions)
      Ok( Json.toJson(searchManagementRepository.listAllSuggestedSolrFields(solrIndexId)) );
    }
  }

}
