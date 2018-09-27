package models

import scala.collection.mutable.ArrayBuffer

package object SearchManagementModel {

  // TODO evaluate model adjustment for SolrIndex being leading construct and containts a List[SearchInput]
  case class SolrIndex(id: Option[Long] = None,
                       name: String,
                       description: String)

  case class SynonymRule(id: Option[Long] = None,
                         synonymType: Int,
                         term: String)

  case class UpDownRule(id: Option[Long] = None,
                        upDownType: Int,
                        boostMalusValue: Int,
                        term: String)

  case class FilterRule(id: Option[Long] = None,
                        term: String)

  case class DeleteRule(id: Option[Long] = None,
                        term: String)

  // TODO rearrange SearchManagementRepository algorithms purely functional, so that no mutable var-attributes necessary
  case class SearchInput(id: Option[Long] = None,
                         term: String,
                         var synonymRules: List[SynonymRule],
                         var upDownRules: List[UpDownRule],
                         var filterRules: List[FilterRule],
                         var deleteRules: List[DeleteRule])

  // TODO currently not in use ...
  // TODO Consider resolving as Option field in the Rule's itself (e.g. UpDownRule.errorMsgs List[String])
  case class SearchInputValidationResult(inputTermErrorMsg: Option[String],
                                         synonymRulesErrorMsg: List[Map[Long, String]],
                                         upDownRulesErrorMsg: List[Map[Long, String]],
                                         filterRulesErrorMsg: List[Map[Long, String]],
                                         deleteRulesErrorMsg: List[Map[Long, String]])

  case class SuggestedSolrField(id: Option[Long] = None,
                                name: String)

}

package object FeatureToggleModel {

  trait FeatureToggleValue {
    def getValue(): Any;
    def isProtected(): Boolean;
    def renderJsValue(): String;
  }

  class BoolFeatureToggleValue(bState: Boolean) extends FeatureToggleValue {
    override def getValue(): Any = bState;
    override def isProtected(): Boolean = false;
    override def renderJsValue(): String = {
      return if(bState) "true" else "false";
    }
  }

  /**
    * String Feature Toggle Value protected from being exposed to the frontend.
    *
    * @param str
    */
  class ProtectedStringFeatureToggleValue(str: String) extends FeatureToggleValue {
    override def getValue(): Any = str;
    override def isProtected(): Boolean = true;
    override def renderJsValue(): String = {
      return "-1";
    }
  }

  // TODO move isProtected() to the FeatureToggle instead of its value
  case class FeatureToggle(toggleName: String, toggleValue: FeatureToggleValue);

  val FEATURE_TOGGLE_UI_CONCEPT_UPDOWN_RULES_COMBINED = "toggle.ui-concept.updown-rules.combined";
  val FEATURE_TOGGLE_UI_CONCEPT_ALL_RULES_WITH_SOLR_FIELDS = "toggle.ui-concept.all-rules.with-solr-fields";
  val FEATURE_TOGGLE_RULE_DEPLOYMENT_AUTO_DECORATE_EXPORT_HASH = "toggle.rule-deployment.auto-decorate.export-hash";
  val FEATURE_TOGGLE_RULE_DEPLOYMENT_SPLIT_DECOMPOUND_RULES_TXT = "toggle.rule-deployment.split-decompound-rules-txt";
  val FEATURE_TOGGLE_RULE_DEPLOYMENT_SPLIT_DECOMPOUND_RULES_TXT_DST_CP_FILE_TO = "toggle.rule-deployment.split-decompound-rules-txt-DST_CP_FILE_TO";
  val FEATURE_TOGGLE_RULE_DEPLOYMENT_PRE_LIVE_PRESENT = "toggle.rule-deployment.pre-live.present";
  val FEATURE_TOGGLE_RULE_DEPLOYMENT_CUSTOM_SCRIPT = "toggle.rule-deployment.custom-script";
  val FEATURE_TOGGLE_RULE_DEPLOYMENT_CUSTOM_SCRIPT_SMUI2SOLR_SH_PATH = "toggle.rule-deployment.custom-script-SMUI2SOLR-SH_PATH";

  // list encapsulated in a class for being @Inject'ed and shared between controllers
  @javax.inject.Singleton
  class FeatureToggleList {
    var list: ArrayBuffer[FeatureToggle] = new ArrayBuffer[FeatureToggle]();

    def getToggle(findToggleName: String): Option[FeatureToggleValue] = {
      val filteredList = list.filter(_.toggleName.equals(findToggleName));
      if(filteredList.size == 0) return None;
      else return Some(filteredList.head.toggleValue);
    }
  };

}
