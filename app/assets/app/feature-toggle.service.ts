import { Injectable } from '@angular/core';
import { Headers, Http, Response } from '@angular/http';

import 'rxjs/add/operator/toPromise';

const FEATURE_TOGGLE_UI_CONCEPT_UPDOWN_RULES_COMBINED = 'toggle.ui-concept.updown-rules.combined';
const FEATURE_TOGGLE_UI_CONCEPT_ALL_RULES_WITH_SOLR_FIELDS = 'toggle.ui-concept.all-rules.with-solr-fields';

// TODO refactor into proper angular/export dependency (DI)
declare var FEATURE_TOGGLE_LIST: any;

@Injectable()
export class FeatureToggleService {

  constructor(private http: Http) {
  }

  getSync(toggleName: string): any {

    console.log('In FeatureToggleService :: getSync');
    // console.log('... toggleName = ' + JSON.stringify(toggleName));
    const retFt = FEATURE_TOGGLE_LIST.filter(function(ft) {
      return (ft.toggleName === toggleName);
    });
    // console.log('... retFt = ' + JSON.stringify(retFt));
    if (retFt.length === 1) {
      console.log('... retFt[0].toggleValue = ' + JSON.stringify(retFt[0].toggleValue));
      return retFt[0].toggleValue;
    } else {
      // TODO werfen oder bei return null belassen?
      // throw new Error("Feature Toggle >>>" + toggleName + "<<< not defined.");
      return null;
    }
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

}
