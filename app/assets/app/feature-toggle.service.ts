import { Injectable } from '@angular/core';
import { Headers, Http, Response } from '@angular/http';

import 'rxjs/add/operator/toPromise';

const FEATURE_TOGGLE_UI_CONCEPT_UPDOWN_RULES_COMBINED = 'toggle.ui-concept.updown-rules.combined';
const FEATURE_TOGGLE_UI_CONCEPT_ALL_RULES_WITH_SOLR_FIELDS = 'toggle.ui-concept.all-rules.with-solr-fields';

@Injectable()
export class FeatureToggleService {

  constructor(private http: Http) {
  }

  getSync(toggleName: string): any {
    switch (toggleName) {
      case FEATURE_TOGGLE_UI_CONCEPT_UPDOWN_RULES_COMBINED:
        // TODO rework static hard-coded definitions of Feature Toggles (to something configureable coming from the backend)
        return {
          bState: true
        }
      case FEATURE_TOGGLE_UI_CONCEPT_ALL_RULES_WITH_SOLR_FIELDS:
        return {
          bState: true
        }
      default:
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
