import { Injectable } from '@angular/core';
import {FeatureToggle, SolrIndex} from '../models';
import {HttpClient} from '@angular/common/http';

const FEATURE_TOGGLE_UI_CONCEPT_UPDOWN_RULES_COMBINED = 'toggle.ui-concept.updown-rules.combined';
const FEATURE_TOGGLE_UI_CONCEPT_ALL_RULES_WITH_SOLR_FIELDS = 'toggle.ui-concept.all-rules.with-solr-fields';
const FEATURE_TOGGLE_RULE_DEPLOYMENT_PRE_LIVE_PRESENT = 'toggle.rule-deployment.pre-live.present';
const FEATURE_TOGGLE_UI_LIST_LIMIT_ITEMS_TO = 'toggle.ui-list.limit-items-to';
const FEATURE_ACTIVATE_SPELLING = 'toggle.activate-spelling';
const FEATURE_ACTIVATE_EVENTHISTORY = 'toggle.activate-eventhistory';
const FEATURE_CUSTOM_UP_DOWN_MAPPINGS = 'toggle.ui-concept.custom.up-down-dropdown-mappings';
const FEATURE_TOGGLE_DEPLOYMENT_LABEL = "toggle.rule-deployment-label";
const FEATURE_TOGGLE_DEPLOYMENT_PRELIVE_LABEL = "toggle.deploy-prelive-fn-label";
const FEATURE_TOGGLE_REPORT_RULE_USAGE_STATISTICS = "toggle.report.rule-usage-statistics"


@Injectable({
  providedIn: 'root'
})
export class FeatureToggleService {
  private readonly baseUrl = 'api/v1';
  private readonly featureToggleApiPath: string = 'featureToggles';
  private featureToggles: FeatureToggle[] = [];

  constructor(private http: HttpClient) {}

  getFeatureToggles(): Promise<void> {
    return this.http
      .get<FeatureToggle[]>(`${this.baseUrl}/${this.featureToggleApiPath}`)
      .toPromise()
      .then(featureToggles => {
        this.featureToggles = featureToggles;
      });
  }

  getSync(toggleName: string): any {
    // console.log('In FeatureToggleService :: getSync');
    // console.log('... toggleName = ' + JSON.stringify(toggleName));
    const retFt = this.featureToggles.filter(ft => (ft.toggleName === toggleName));
    // console.log('... retFt = ' + JSON.stringify(retFt));
    if (retFt.length === 1) {
      // console.log('... retFt[0].toggleValue = ' + JSON.stringify(retFt[0].toggleValue));
      return retFt[0].toggleValue;
    } else {
      // TODO werfen oder bei return null belassen?
      // throw new Error("Feature Toggle >>>" + toggleName + "<<< not defined.");
      return null;
    }
  }

  isRuleTaggingActive(): boolean {
    return this.getSync('toggle.rule-tagging');
  }

  // TODO rethink if interfacing like this is generic enough

  getSyncToggleUiConceptUpDownRulesCombined(): any {
    return this
      .getSync(FEATURE_TOGGLE_UI_CONCEPT_UPDOWN_RULES_COMBINED);
  }

  getSyncToggleUiConceptAllRulesWithSolrFields(): any {
    return this
      .getSync(FEATURE_TOGGLE_UI_CONCEPT_ALL_RULES_WITH_SOLR_FIELDS);
  }

  getSyncToggleRuleDeploymentPreLivePresent(): any {
    return this
      .getSync(FEATURE_TOGGLE_RULE_DEPLOYMENT_PRE_LIVE_PRESENT);
  }

  getSyncToggleUiListLimitItemsTo(): any {
    return this
      .getSync(FEATURE_TOGGLE_UI_LIST_LIMIT_ITEMS_TO);
  }

  getSyncToggleActivateSpelling(): any {
    return this.getSync(FEATURE_ACTIVATE_SPELLING);
  }

  getSyncToggleActivateEventHistory(): any {
    return this.getSync(FEATURE_ACTIVATE_EVENTHISTORY);
  }

  getSyncToggleCustomUpDownDropdownMappings(): any {
    try {
      return JSON.parse(this.getSync(FEATURE_CUSTOM_UP_DOWN_MAPPINGS));
    } catch(e) {
      console.error(e);
      return {};
    }
  }

  getSyncToggleDeploymentLabel(stage: string): any {
    const s =
      (stage == 'PRELIVE') ?
        FEATURE_TOGGLE_DEPLOYMENT_PRELIVE_LABEL :
        FEATURE_TOGGLE_DEPLOYMENT_LABEL;
    return this.getSync(s);
  }

  getSyncToggleRuleUsageStatistics(): any {
    return this.getSync(FEATURE_TOGGLE_REPORT_RULE_USAGE_STATISTICS);
  }

}
