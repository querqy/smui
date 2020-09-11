package models

import javax.inject.Inject
import play.api.libs.json.{Json, OFormat}
import play.api.{Configuration, Logging}
import play.twirl.api.utils.StringEscapeUtils

import scala.util.Try

import models.rules.UpDownRule

// TODO refactor FeatureToggleModel (and FeatureToggleService) to config package (for being in sync with Spec structure)
package object FeatureToggleModel extends Logging {

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

  class JsObjFeatureToggleValue(rawObjAsString: String) extends JsFeatureToggleValue {
    override def render(): String = s"$rawObjAsString"
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
    private val SMUI_DEFAULT_DISPLAY_USERNAME = "toggle.display-username.default"
    private val SMUI_ACTIVATE_EVENTHISTORY = "toggle.activate-eventhistory"
    private val FEATURE_CUSTOM_UP_DOWN_MAPPINGS = "toggle.ui-concept.custom.up-down-dropdown-mappings"
    private val SMUI_DEPLOYMENT_GIT_REPO_URL = "smui.deployment.git.repo-url"
    private val SMUI_DEPLOYMENT_GIT_FN_COMMON_RULES_TXT = "smui2solr.deployment.git.filename.common-rules-txt"

    /**
      * helper for custom UP/DOWN mappings
      */

    case class UpDownDropdownMapping(
      displayName: String,
      upDownType: Int,
      boostMalusValue: Int
    )

    implicit val jsonFormatUpDownDropdownMapping: OFormat[UpDownDropdownMapping] = Json.format[UpDownDropdownMapping]

    // TODO consider using the UpDownDropdownMapping model
    private val DEFAULT_UP_DOWN_MAPPINGS = "[{\"displayName\":\"UP(+++++)\",\"upDownType\":0,\"boostMalusValue\":500},{\"displayName\":\"UP(++++)\",\"upDownType\":0,\"boostMalusValue\":100},{\"displayName\":\"UP(+++)\",\"upDownType\":0,\"boostMalusValue\":50},{\"displayName\":\"UP(++)\",\"upDownType\":0,\"boostMalusValue\":10},{\"displayName\":\"UP(+)\",\"upDownType\":0,\"boostMalusValue\": 5},{\"displayName\":\"DOWN(-)\",\"upDownType\":1,\"boostMalusValue\": 5},{\"displayName\":\"DOWN(--)\",\"upDownType\":1,\"boostMalusValue\": 10},{\"displayName\":\"DOWN(---)\",\"upDownType\":1,\"boostMalusValue\": 50},{\"displayName\":\"DOWN(----)\",\"upDownType\":1,\"boostMalusValue\": 100},{\"displayName\":\"DOWN(-----)\",\"upDownType\":1,\"boostMalusValue\": 500}]"

    def isCustomUpDownMappingsValid(rawCustomUpDownMappings: String) = {
      // parse JSON structure
      logger.debug(s":: In isCustomUpDownMappingsValid :: rawCustomUpDownMappings = $rawCustomUpDownMappings")
      Try(
        Json.parse(rawCustomUpDownMappings).validate[Seq[UpDownDropdownMapping]].asOpt.get
      ).toOption match {
        case None => false
        case Some(customUpDownMappings: Seq[UpDownDropdownMapping]) => {
          // check plausibility
          // ... mappings are not empty
          if (customUpDownMappings.isEmpty) {
            logger.error(s":: plausibility error: custom mappings are empty (customUpDownMappings = $customUpDownMappings)")
            false
          } else {
            // ... only UP/DOWN values exist and the description is not empty
            customUpDownMappings.forall(m => {
              (if (!List(UpDownRule.TYPE_UP, UpDownRule.TYPE_DOWN).contains(m.upDownType)) {
                logger.error(s":: plausibility error: custom mappings contain unexpected upDownType (m = $m)")
                false
              } else {
                true
              }) &&
              (!m.displayName.isEmpty)
            }) &&
            // ... minimum one UP/DOWN for both types
            (if (customUpDownMappings.exists(m => m.upDownType.equals(UpDownRule.TYPE_UP)) &&
              customUpDownMappings.exists(m => m.upDownType.equals(UpDownRule.TYPE_DOWN))) {
              true
            } else {
              logger.error(s":: plausibility error: UP and DOWN type must be defined (customUpDownMappings = $customUpDownMappings)")
              false
            })
          }
        }
      }
    }

    /**
      * Interface
      */

    def getJsFrontendToggleList: List[JsFeatureToggle] = {

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
        jsBoolFeatureToggle(FEATURE_TOGGLE_ACTIVATE_SPELLING, false),
        jsBoolFeatureToggle(SMUI_ACTIVATE_EVENTHISTORY, false),
        JsFeatureToggle(
          FEATURE_CUSTOM_UP_DOWN_MAPPINGS,
          // TODO consider rendering the UpDownDropdownMapping model by a proper JsJsonFeatureToggle type (instead of raw JsObjFeatureToggleValue)
          new JsObjFeatureToggleValue(
            appConfig.getOptional[String](FEATURE_CUSTOM_UP_DOWN_MAPPINGS) match {
              case None => DEFAULT_UP_DOWN_MAPPINGS
              case Some(rawCustomUpDownMappings: String) => {
                logger.debug(s"FeatureToggleModel :: got rawCustomUpDownMappings = $rawCustomUpDownMappings")
                // validate customUpDownMappings
                if (isCustomUpDownMappingsValid(rawCustomUpDownMappings)) {
                  rawCustomUpDownMappings
                } else {
                  // in case of negative validation, log an error and return default
                  logger.error(s"FeatureToggleModel :: getJsFrontendToggleList :: invalid rawCustomUpDownMappings found, using defaults (>>>$rawCustomUpDownMappings)")
                  DEFAULT_UP_DOWN_MAPPINGS
                }
              }
            }
          )
        )
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

    def getToggleDefaultDisplayUsername: String = {
      appConfig.getOptional[String](SMUI_DEFAULT_DISPLAY_USERNAME).getOrElse("Anonymous Search Manager")
    }

    def getToggleActivateEventHistory: Boolean = {
      appConfig.getOptional[Boolean](SMUI_ACTIVATE_EVENTHISTORY).getOrElse(false)
    }

    def getSmuiDeploymentGitRepoUrl: String = {
      appConfig.getOptional[String](SMUI_DEPLOYMENT_GIT_REPO_URL).getOrElse("localhost/smui_test_repo")
    }

    def getSmuiDeploymentGitFilenameCommonRulesTxt: String = {
      appConfig.getOptional[String](SMUI_DEPLOYMENT_GIT_FN_COMMON_RULES_TXT).getOrElse("rules.txt")
    }

  }

}
