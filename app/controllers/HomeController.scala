package controllers

import javax.inject.Inject

import play.api.Configuration
import play.api.mvc._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}

import models.FeatureToggleModel._

class HomeController @Inject()(cc: MessagesControllerComponents,
                               appConfig: Configuration,
                               featureToggleList: FeatureToggleList)(implicit executionContext: ExecutionContext)
  extends MessagesAbstractController(cc) {

  private val logger = play.api.Logger

  // TODO refactor into a separated FeatureToggleController or sth similar
  // TODO initialize independent from home controller and the fact, that first call usually goes to /

  def index(urlPath: String) = Action.async {
    Future {
      logger.debug("index-Controller called delivering views.html.home()")

      logger.debug("... reading config")
      // shift default values to central class as well
      val TOGGLE_UI_CONCEPT_UPDOWN_RULES_COMBINED_VAL: Boolean = appConfig.getBoolean(FEATURE_TOGGLE_UI_CONCEPT_UPDOWN_RULES_COMBINED).getOrElse(true);
      val TOGGLE_UI_CONCEPT_ALL_RULES_WITH_SOLR_FIELDS_VAL: Boolean = appConfig.getBoolean(FEATURE_TOGGLE_UI_CONCEPT_ALL_RULES_WITH_SOLR_FIELDS).getOrElse(true);
      val FEATURE_TOGGLE_RULE_DEPLOYMENT_PRE_LIVE_PRESENT_VAL: Boolean = appConfig.getBoolean(FEATURE_TOGGLE_RULE_DEPLOYMENT_PRE_LIVE_PRESENT).getOrElse(false);
      val FEATURE_TOGGLE_RULE_DEPLOYMENT_AUTO_DECORATE_EXPORT_HASH_VAL: Boolean = appConfig.getBoolean(FEATURE_TOGGLE_RULE_DEPLOYMENT_AUTO_DECORATE_EXPORT_HASH).getOrElse(false);
      val FEATURE_TOGGLE_RULE_DEPLOYMENT_SPLIT_DECOMPOUND_RULES_TXT_VAL: Boolean = appConfig.getBoolean(FEATURE_TOGGLE_RULE_DEPLOYMENT_SPLIT_DECOMPOUND_RULES_TXT).getOrElse(false);
      val FEATURE_TOGGLE_RULE_DEPLOYMENT_SPLIT_DECOMPOUND_RULES_TXT_DST_CP_FILE_TO_VAL: String = appConfig.getString(FEATURE_TOGGLE_RULE_DEPLOYMENT_SPLIT_DECOMPOUND_RULES_TXT_DST_CP_FILE_TO).getOrElse("");

      logger.debug("... TOGGLE_UI_CONCEPT_UPDOWN_RULES_COMBINED_VAL = " + TOGGLE_UI_CONCEPT_UPDOWN_RULES_COMBINED_VAL);
      logger.debug("... TOGGLE_UI_CONCEPT_ALL_RULES_WITH_SOLR_FIELDS_VAL = " + TOGGLE_UI_CONCEPT_ALL_RULES_WITH_SOLR_FIELDS_VAL);
      logger.debug("... FEATURE_TOGGLE_RULE_DEPLOYMENT_PRE_LIVE_PRESENT_VAL = " + FEATURE_TOGGLE_RULE_DEPLOYMENT_PRE_LIVE_PRESENT_VAL);
      logger.debug("... FEATURE_TOGGLE_RULE_DEPLOYMENT_AUTO_DECORATE_EXPORT_HASH_VAL = " + FEATURE_TOGGLE_RULE_DEPLOYMENT_AUTO_DECORATE_EXPORT_HASH_VAL);
      logger.debug("... FEATURE_TOGGLE_RULE_DEPLOYMENT_SPLIT_DECOMPOUND_RULES_TXT_VAL = " + FEATURE_TOGGLE_RULE_DEPLOYMENT_SPLIT_DECOMPOUND_RULES_TXT_VAL);
      logger.debug("... FEATURE_TOGGLE_RULE_DEPLOYMENT_SPLIT_DECOMPOUND_RULES_TXT_DST_CP_FILE_TO_VAL = " + FEATURE_TOGGLE_RULE_DEPLOYMENT_SPLIT_DECOMPOUND_RULES_TXT_DST_CP_FILE_TO_VAL);

      // TODO refactor into recursive list adding function initializing a immutable list
      featureToggleList.list = ArrayBuffer[FeatureToggle]();
      featureToggleList.list += FeatureToggle(
        FEATURE_TOGGLE_UI_CONCEPT_UPDOWN_RULES_COMBINED,
        new BoolFeatureToggleValue(TOGGLE_UI_CONCEPT_UPDOWN_RULES_COMBINED_VAL) );
      featureToggleList.list += FeatureToggle(
        FEATURE_TOGGLE_UI_CONCEPT_ALL_RULES_WITH_SOLR_FIELDS,
        new BoolFeatureToggleValue(TOGGLE_UI_CONCEPT_ALL_RULES_WITH_SOLR_FIELDS_VAL) );
      featureToggleList.list += FeatureToggle(
        FEATURE_TOGGLE_RULE_DEPLOYMENT_PRE_LIVE_PRESENT,
        new BoolFeatureToggleValue(FEATURE_TOGGLE_RULE_DEPLOYMENT_PRE_LIVE_PRESENT_VAL) );
      featureToggleList.list += FeatureToggle(
        FEATURE_TOGGLE_RULE_DEPLOYMENT_AUTO_DECORATE_EXPORT_HASH,
        new BoolFeatureToggleValue(FEATURE_TOGGLE_RULE_DEPLOYMENT_AUTO_DECORATE_EXPORT_HASH_VAL) );
      featureToggleList.list += FeatureToggle(
        FEATURE_TOGGLE_RULE_DEPLOYMENT_SPLIT_DECOMPOUND_RULES_TXT,
        new BoolFeatureToggleValue(FEATURE_TOGGLE_RULE_DEPLOYMENT_SPLIT_DECOMPOUND_RULES_TXT_VAL) );
      featureToggleList.list += FeatureToggle(
        FEATURE_TOGGLE_RULE_DEPLOYMENT_SPLIT_DECOMPOUND_RULES_TXT_DST_CP_FILE_TO,
        new ProtectedStringFeatureToggleValue(FEATURE_TOGGLE_RULE_DEPLOYMENT_SPLIT_DECOMPOUND_RULES_TXT_DST_CP_FILE_TO_VAL) );

      logger.debug("... config ready!")
      Ok( views.html.home(featureToggleList.list.toList) )
    }(executionContext) // TODO withSecurity ... because of play.filters.headers.contentSecurityPolicy (and resolve general setup in application.conf)
  }

}
