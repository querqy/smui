package models

import javax.inject.Inject
import play.api.Configuration
import play.twirl.api.utils.StringEscapeUtils

package object FeatureToggleModel {

  trait JsFeatureToggleValue {
    def render(): String
  }

  class JsBoolFeatureToggleValue(bState: Boolean) extends JsFeatureToggleValue {
    override def render(): String = {
      bState.toString
    }
  }

  class JsStringFeatureToggleValue(value: String) extends JsFeatureToggleValue {
    override def render(): String = s""""${StringEscapeUtils.escapeEcmaScript(value)}""""
  }

  case class JsFeatureToggle(toggleName: String, toggleValue: JsFeatureToggleValue)

  @javax.inject.Singleton
  class FeatureToggleService @Inject()(appConfig: Configuration) {

    private val FEATURE_TOGGLE_UI_CONCEPT_UPDOWN_RULES_COMBINED = "toggle.ui-concept.updown-rules.combined"
    private val FEATURE_TOGGLE_UI_CONCEPT_ALL_RULES_WITH_SOLR_FIELDS = "toggle.ui-concept.all-rules.with-solr-fields"
    private val FEATURE_TOGGLE_UI_LIST_LIMIT_ITEMS_TO = "toggle.ui-list.limit-items-to"
    private val FEATURE_TOGGLE_RULE_DEPLOYMENT_LOG_RULE_ID = "toggle.rule-deployment.log-rule-id"
    private val FEATURE_TOGGLE_RULE_DEPLOYMENT_SPLIT_DECOMPOUND_RULES_TXT = "toggle.rule-deployment.split-decompound-rules-txt"
    private val FEATURE_TOGGLE_RULE_DEPLOYMENT_SPLIT_DECOMPOUND_RULES_TXT_DST_CP_FILE_TO = "toggle.rule-deployment.split-decompound-rules-txt-DST_CP_FILE_TO"
    private val FEATURE_TOGGLE_RULE_DEPLOYMENT_PRE_LIVE_PRESENT = "toggle.rule-deployment.pre-live.present"
    private val FEATURE_TOGGLE_RULE_DEPLOYMENT_CUSTOM_SCRIPT = "toggle.rule-deployment.custom-script"
    private val FEATURE_TOGGLE_RULE_DEPLOYMENT_CUSTOM_SCRIPT_SMUI2SOLR_SH_PATH = "toggle.rule-deployment.custom-script-SMUI2SOLR-SH_PATH"
    private val FEATURE_TOGGLE_HEADLINE = "toggle.headline"
    private val ACTIVATE_RULE_TAGGING = "toggle.rule-tagging"
    private val PREDEFINED_TAGS_FILE = "toggle.predefined-tags-file"
    private val SMUI_AUTH_SIMPLE_LOGOUT = "smui.auth.ui-concept.simple-logout-button-target-url"
    private val SMUI_VERSION = "smui.version"
    private val FEATURE_TOGGLE_ACTIVATE_SPELLING = "toggle.activate-spelling"

    def getJsFrontendToogleList: List[JsFeatureToggle] = {
      def jsBoolFeatureToggle(toggleKey: String, bDefault: Boolean): JsFeatureToggle = {
        JsFeatureToggle(
          toggleKey,
          new JsBoolFeatureToggleValue(appConfig.getOptional[Boolean](toggleKey).getOrElse(bDefault))
        )
      }
      List(
        jsBoolFeatureToggle(FEATURE_TOGGLE_UI_CONCEPT_UPDOWN_RULES_COMBINED, true),
        jsBoolFeatureToggle(FEATURE_TOGGLE_UI_CONCEPT_ALL_RULES_WITH_SOLR_FIELDS, true),
        jsBoolFeatureToggle(FEATURE_TOGGLE_RULE_DEPLOYMENT_PRE_LIVE_PRESENT, false),
        jsBoolFeatureToggle(ACTIVATE_RULE_TAGGING, false),
        JsFeatureToggle(FEATURE_TOGGLE_HEADLINE, new JsStringFeatureToggleValue(
          appConfig.getOptional[String](FEATURE_TOGGLE_HEADLINE).getOrElse("Search Management UI"))),
        JsFeatureToggle(SMUI_AUTH_SIMPLE_LOGOUT, new JsStringFeatureToggleValue(
          appConfig.getOptional[String](SMUI_AUTH_SIMPLE_LOGOUT).getOrElse(""))),
        JsFeatureToggle(SMUI_VERSION, new JsStringFeatureToggleValue(models.buildInfo.BuildInfo.version)),
        JsFeatureToggle(FEATURE_TOGGLE_UI_LIST_LIMIT_ITEMS_TO, new JsStringFeatureToggleValue(
          appConfig.getOptional[String](FEATURE_TOGGLE_UI_LIST_LIMIT_ITEMS_TO).getOrElse("-1"))),
        jsBoolFeatureToggle(FEATURE_TOGGLE_ACTIVATE_SPELLING, false)
      )
    }

    def getToggleRuleDeploymentLogRuleId: Boolean = {
      appConfig.getOptional[Boolean](FEATURE_TOGGLE_RULE_DEPLOYMENT_LOG_RULE_ID).getOrElse(false)
    }

    def getToggleRuleDeploymentSplitDecompoundRulesTxt: Boolean = {
      appConfig.getOptional[Boolean](FEATURE_TOGGLE_RULE_DEPLOYMENT_SPLIT_DECOMPOUND_RULES_TXT).getOrElse(false)
    }

    def getToggleRuleDeploymentSplitDecompoundRulesTxtDstCpFileTo: String = {
      appConfig.getOptional[String](FEATURE_TOGGLE_RULE_DEPLOYMENT_SPLIT_DECOMPOUND_RULES_TXT_DST_CP_FILE_TO).getOrElse("")
    }

    def getToggleRuleDeploymentCustomScript: Boolean = {
      appConfig.getOptional[Boolean](FEATURE_TOGGLE_RULE_DEPLOYMENT_CUSTOM_SCRIPT).getOrElse(false)
    }

    def getToggleRuleDeploymentCustomScriptSmui2solrShPath: String = {
      appConfig.getOptional[String](FEATURE_TOGGLE_RULE_DEPLOYMENT_CUSTOM_SCRIPT_SMUI2SOLR_SH_PATH).getOrElse("")
    }

    def isRuleTaggingActive: Boolean = {
      appConfig.get[Boolean](ACTIVATE_RULE_TAGGING)
    }

    def predefinedTagsFileName: Option[String] = {
      appConfig.getOptional[String](PREDEFINED_TAGS_FILE).filter(_.nonEmpty)
    }

    def getToggleActivateSpelling: Boolean = {
      appConfig.getOptional[Boolean](FEATURE_TOGGLE_ACTIVATE_SPELLING).getOrElse(false)
    }
  }

}
