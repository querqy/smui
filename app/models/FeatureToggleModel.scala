package models

import javax.inject.Inject

import play.api.Configuration

package object FeatureToggleModel {

  trait JsFeatureToggleValue {
    def render(): String
  }

  class JsBoolFeatureToggleValue(bState: Boolean) extends JsFeatureToggleValue {
    override def render(): String = {
      bState.toString
    }
  }

  case class JsFeatureToggle(toggleName: String, toggleValue: JsFeatureToggleValue)

  @javax.inject.Singleton
  class FeatureToggleService @Inject()(appConfig: Configuration) {

    val FEATURE_TOGGLE_UI_CONCEPT_UPDOWN_RULES_COMBINED = "toggle.ui-concept.updown-rules.combined"
    val FEATURE_TOGGLE_UI_CONCEPT_ALL_RULES_WITH_SOLR_FIELDS = "toggle.ui-concept.all-rules.with-solr-fields"
    val FEATURE_TOGGLE_RULE_DEPLOYMENT_AUTO_DECORATE_EXPORT_HASH = "toggle.rule-deployment.auto-decorate.export-hash"
    val FEATURE_TOGGLE_RULE_DEPLOYMENT_SPLIT_DECOMPOUND_RULES_TXT = "toggle.rule-deployment.split-decompound-rules-txt"
    val FEATURE_TOGGLE_RULE_DEPLOYMENT_SPLIT_DECOMPOUND_RULES_TXT_DST_CP_FILE_TO = "toggle.rule-deployment.split-decompound-rules-txt-DST_CP_FILE_TO"
    val FEATURE_TOGGLE_RULE_DEPLOYMENT_PRE_LIVE_PRESENT = "toggle.rule-deployment.pre-live.present"
    val FEATURE_TOGGLE_RULE_DEPLOYMENT_CUSTOM_SCRIPT = "toggle.rule-deployment.custom-script"
    val FEATURE_TOGGLE_RULE_DEPLOYMENT_CUSTOM_SCRIPT_SMUI2SOLR_SH_PATH = "toggle.rule-deployment.custom-script-SMUI2SOLR-SH_PATH"

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
        jsBoolFeatureToggle(FEATURE_TOGGLE_RULE_DEPLOYMENT_PRE_LIVE_PRESENT, false)
      )
    }

    def getToggleRuleDeploymentAutoDecorateExportHash: Boolean = {
      appConfig.getOptional[Boolean](FEATURE_TOGGLE_RULE_DEPLOYMENT_AUTO_DECORATE_EXPORT_HASH).getOrElse(false)
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
  }

}
