package controllers

import javax.inject.Inject

import play.api.Configuration
import play.api.mvc._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}

import models.FeatureToggleModel._

class HomeController @Inject()(cc: MessagesControllerComponents,
                               appConfig: Configuration)(implicit executionContext: ExecutionContext)
  extends MessagesAbstractController(cc) {

  private val logger = play.api.Logger

  // TODO refactor into a separated FeatureToggleController or sth similar
  val FEATURE_TOGGLE_UI_CONCEPT_UPDOWN_RULES_COMBINED = "toggle.ui-concept.updown-rules.combined";
  val FEATURE_TOGGLE_UI_CONCEPT_ALL_RULES_WITH_SOLR_FIELDS = "toggle.ui-concept.all-rules.with-solr-fields";

  def index(urlPath: String) = Action.async {
    Future {
      logger.debug("index-Controller called delivering views.html.home()")

      logger.debug("... reading config")
      val TOGGLE_UI_CONCEPT_UPDOWN_RULES_COMBINED_VAL: Boolean = appConfig.getBoolean(FEATURE_TOGGLE_UI_CONCEPT_UPDOWN_RULES_COMBINED).getOrElse(true);
      val TOGGLE_UI_CONCEPT_ALL_RULES_WITH_SOLR_FIELDS_VAL: Boolean = appConfig.getBoolean(FEATURE_TOGGLE_UI_CONCEPT_ALL_RULES_WITH_SOLR_FIELDS).getOrElse(true);
      logger.debug("... TOGGLE_UI_CONCEPT_UPDOWN_RULES_COMBINED_VAL = " + TOGGLE_UI_CONCEPT_UPDOWN_RULES_COMBINED_VAL);
      logger.debug("... TOGGLE_UI_CONCEPT_ALL_RULES_WITH_SOLR_FIELDS_VAL = " + TOGGLE_UI_CONCEPT_ALL_RULES_WITH_SOLR_FIELDS_VAL);
      // TODO refactor into recursive list adding function initializing a immutable list
      val featureToggleList: ArrayBuffer[FeatureToggle] = ArrayBuffer[FeatureToggle]();
      featureToggleList += FeatureToggle(
        FEATURE_TOGGLE_UI_CONCEPT_UPDOWN_RULES_COMBINED,
        new BoolFeatureToggleValue(TOGGLE_UI_CONCEPT_UPDOWN_RULES_COMBINED_VAL) );
      featureToggleList += FeatureToggle(
        FEATURE_TOGGLE_UI_CONCEPT_ALL_RULES_WITH_SOLR_FIELDS,
        new BoolFeatureToggleValue(TOGGLE_UI_CONCEPT_ALL_RULES_WITH_SOLR_FIELDS_VAL) );
      logger.debug("... config ready!")
      Ok( views.html.home(featureToggleList.toList) )
    }(executionContext) // TODO withSecurity ... because of play.filters.headers.contentSecurityPolicy (and resolve general setup in application.conf)
  }

}
