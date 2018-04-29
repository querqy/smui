import { Component, OnInit, Input } from '@angular/core';
// import { ActivatedRoute, Params } from '@angular/router';

import { ViewChild } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import { Subject } from 'rxjs/Subject';
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/merge';
import 'rxjs/add/operator/filter';
import 'rxjs/add/operator/debounceTime';
import 'rxjs/add/operator/distinctUntilChanged';
import { NgbTypeahead } from '@ng-bootstrap/ng-bootstrap';

import { AppComponent } from './app.component';
import { SearchInputListComponent } from './search-input-list.component';

import * as smm from './search-management.model';
import { SearchManagementService } from './search-management.service';
import { FeatureToggleService } from './feature-toggle.service';

const DEFAULT_IDX_UP_DOWN_DROPDOWN_DEFINITION_MAPPING = 4;

@Component({
  selector: 'smui-search-input-detail',
  templateUrl: './search-input-detail.component.html',
  styleUrls: ['./search-input-detail.component.css'],
  providers: [FeatureToggleService]
})
export class SearchInputDetailComponent implements OnInit {

  // TODO consider using an more abstract component-communication model (e.g. message-service, events, etc.)
  @Input() listComponent: SearchInputListComponent;
  @Input() parentComponent: AppComponent;

  detailSearchInput: smm.SearchInput = null;
  private initDetailSearchInputHashForDirtyState: string = null;
  private suggestedSolrFieldNames = null;
  private currentSolrIndexId = -1; // TODO maybe take parentComponent's currentSolrIndexId instead of local copy

  // TODO open typeahead popup on focus -- focus$ = new Subject<string>();
  searchSuggestedSolrFieldNames = (text$: Observable<string>) =>
    text$
      .debounceTime(200).distinctUntilChanged()
      .map(term => (
        this.suggestedSolrFieldNames === null ? [] : (
          term === ''
            ? this.suggestedSolrFieldNames
            : this.suggestedSolrFieldNames
              .filter(s => s.toLowerCase().indexOf(term.toLowerCase()) > -1)).slice(0, 10)
        )
      );

  constructor(
    private searchManagementService: SearchManagementService,
    public featureToggleService: FeatureToggleService /*, TODO use or remove "route" DI
    private route: ActivatedRoute */) {
  }

  ngOnInit() {
    console.log('In SearchInputDetailComponent :: ngOnInit');
    console.log(':: parentComponent = ' + this.parentComponent);
  }

  public loadSuggestedSolrFieldsForSolrIndexWithId(solrIndexId: number) {
    console.log('In SearchInputDetailComponent :: loadSuggestedSolrFieldsForSolrIndexWithId');
    console.log(':: solrIndexId = ' + JSON.stringify(solrIndexId));
    this.currentSolrIndexId = solrIndexId;

    this.searchManagementService
      .listAllSuggestedSolrFields(this.currentSolrIndexId)
      .then(retSuggestedSolrFieldNames => {
        console.log(':: then :: retSuggestedSolrFieldNames = ' + JSON.stringify(retSuggestedSolrFieldNames));
        this.suggestedSolrFieldNames = retSuggestedSolrFieldNames
          .reduce((r, s) => r.concat(s.name, '-' + s.name), [])
        console.log(':: this.suggestedSolrFieldNames = ' + JSON.stringify(this.suggestedSolrFieldNames));
      })
      .catch(error => this.handleError(error));
  }

  handleError(error: any) {
    console.log('In SearchInputDetailComponent :: handleError');
    console.log(':: error = ' + error);
    this.parentComponent
      .showErrorMsg('An error occurred.'); // TODO Do a more detaillied error description
  }

  // TODO consider evaluate a more elegant solution to dispatch upDownDropdownDefinitionMappings from smm to the template
  public upDownDropdownDefinitionMappings() {
    return smm.upDownDropdownDefinitionMappings;
  }

  // Helper to find the closest matching mapping to an existing persisted UP/DOWN rule
  private findIdxClosestUpDownDropdownDefinitionMapping(upDownRule) {
    // TODO heaps of implicit knowledge about the model is necessary here. Consider refactoring (maybe model or even backend).
    // Find minimum distance to comparision array. TODO Consider a more elegant functional solution.
    const arrayComparison = [500, 100 , 50, 10, 5];
    let idxMinimum = 0;
    let lastMinimumValue = Math.abs(arrayComparison[0] - upDownRule.boostMalusValue);
    for (let i = 1; i < 5; i++) {
      const nextMinimumValue = Math.abs(arrayComparison[i] - upDownRule.boostMalusValue);
      if (nextMinimumValue < lastMinimumValue) {
        lastMinimumValue = nextMinimumValue;
        idxMinimum = i;
      }
    }
    switch (upDownRule.upDownType) {
      case 0: // UP
        return idxMinimum;
      case 1: // DOWN
        return 9 - idxMinimum;
      default: // undefined
        return DEFAULT_IDX_UP_DOWN_DROPDOWN_DEFINITION_MAPPING;
    }
  }

  private extractSuggestedSolrFieldName(objList: Array<any>) {
    for (let i = 0; i < objList.length; i++) {
      // TODO "term" and "suggestedSolrFieldName" attributes are implicitly assumed. Change approach (maybe put into model or even backend)
      objList[i].suggestedSolrFieldName = '';
      if (objList[i].term !== null) {
        const term = objList[i].term.trim();
        if (term.length > 0) {
          const regexSolrSyntax = new RegExp('^\\*(.*?):');
          const found = term.match(regexSolrSyntax);
          if ((found !== null) && (found.length > 1)) {
            objList[i].suggestedSolrFieldName = found[1].trim();
            objList[i].term = objList[i].term.substr(found[0].length, objList[i].term.length - found[0].length);
          }
        }
      }
    }
  }

  private integrateSuggestedSolrFieldName(objList: Array<any>) {
    for (let i = 0; i < objList.length; i++) {
      if (objList[i].suggestedSolrFieldName) {
        if (objList[i].suggestedSolrFieldName.trim().length > 0) {
          objList[i].term = '* ' + objList[i].suggestedSolrFieldName + ':' + objList[i].term;
          // TODO not very elegant incl. "delete" operator ... Refactor!
          delete objList[i].suggestedSolrFieldName;
        }
      }
    }
  }

  public showDetailsForSearchInputWithId(searchInputId: number) {
    console.log('In SearchInputDetailComponent :: showDetailsForSearchInputWithId :: searchInputId = ' + searchInputId);

    if (searchInputId === null) {
      this.detailSearchInput = null;
    } else {
      this.searchManagementService
        .getDetailedSearchInput(searchInputId)
        .then(retSearchInput => {

          this.detailSearchInput = retSearchInput;

          // take care of extracted Solr syntax
          if (this.featureToggleService.getSyncToggleUiConceptAllRulesWithSolrFields()) {
            this.extractSuggestedSolrFieldName(this.detailSearchInput.upDownRules);
            this.extractSuggestedSolrFieldName(this.detailSearchInput.filterRules);
          }

          // take care of UP/DOWN mappings
          if (this.featureToggleService.getSyncToggleUiConceptUpDownRulesCombined()) {
            if ((this.detailSearchInput.upDownRules !== null) && (this.detailSearchInput.upDownRules.length > 0)) {
              // convert to simple combined UP/DOWN dropdown definition mappings
              // TODO consider a more elegant functional solution
              for (let idxUpDownRules = 0; idxUpDownRules < this.detailSearchInput.upDownRules.length; idxUpDownRules++) {
                this.detailSearchInput.upDownRules[idxUpDownRules]
                  .upDownDropdownDefinitionMapping = this.findIdxClosestUpDownDropdownDefinitionMapping(
                    this.detailSearchInput.upDownRules[idxUpDownRules]);
              }
            }
          }

          this.initDetailSearchInputHashForDirtyState = JSON.stringify(this.detailSearchInput); // TODO hash string value
        })
        .catch(error => this.handleError(error));
    }
  }

  public isDirty(): boolean {
    console.log('In SearchInputDetailComponent :: isDirty');

    if (this.detailSearchInput === null) {
      return false;
    } else {
      return JSON.stringify(this.detailSearchInput) !== this.initDetailSearchInputHashForDirtyState;
    }
  }

  public addNewSynonymRule() {
    console.log('In SearchInputDetailComponent :: addNewSynonym');

    const emptySynonymRule: smm.SynonymRule = {
      id: null,
      synonymType: 0,
      term: ''
    };
    this.detailSearchInput
      .synonymRules.push(emptySynonymRule);
  }

  public deleteSynonymRule(index: number) {
    console.log('In SearchInputDetailComponent :: deleteSynonymRule :: index = ' + index);

    this.detailSearchInput.synonymRules.splice(index, 1);
  }

  public addNewUpDownRule() {
    console.log('In SearchInputDetailComponent :: addNewUpDownRule');

    const emptyUpDownRule: smm.UpDownRule = {
      id: null,
      term: ''
    };
    if (this.featureToggleService.getSyncToggleUiConceptUpDownRulesCombined()) {
      // NOTE: the attribute "upDownDropdownDefinitionMapping" is frontend-only and not supposed to be part of REST transfer
      emptyUpDownRule.upDownDropdownDefinitionMapping = DEFAULT_IDX_UP_DOWN_DROPDOWN_DEFINITION_MAPPING;
    } else {
      emptyUpDownRule.upDownType = 0;
      emptyUpDownRule.boostMalusValue = 0;
    }
    this.detailSearchInput
      .upDownRules.push(emptyUpDownRule);
  }

  public deleteUpDownRule(index: number) {
    console.log('In SearchInputDetailComponent :: deleteUpDownRule :: index = ' + index);

    this.detailSearchInput.upDownRules.splice(index, 1);
  }

  public addNewFilterRule() {
    console.log('In SearchInputDetailComponent :: addNewFilterRule');

    const emptyFilterRule: smm.FilterRule = {
      id: null,
      term: ''
    };
    if (this.featureToggleService.getSyncToggleUiConceptAllRulesWithSolrFields()) {
      emptyFilterRule.suggestedSolrFieldName = '';
    }
    this.detailSearchInput
      .filterRules.push(emptyFilterRule);
  }

  public deleteFilterRule(index: number) {
    console.log('In SearchInputDetailComponent :: deleteFilterRule :: index = ' + index);

    this.detailSearchInput.filterRules.splice(index, 1);
  }

  public addNewDeleteRule() {
    console.log('In SearchInputDetailComponent :: addNewDeleteRule');

    const emptyDeleteRule: smm.DeleteRule = {
      id: null,
      term: ''
    };
    this.detailSearchInput
      .deleteRules.push(emptyDeleteRule);
  }

  public deleteDeleteRule(index: number) {
    console.log('In SearchInputDetailComponent :: deleteDeleteRule :: index = ' + index);

    this.detailSearchInput.deleteRules.splice(index, 1);
  }

  public saveSearchInputDetails() {
    console.log('In SearchInputDetailComponent :: saveSearchInputDetails');

    // TODO routine directly operating on this.detailSearchInput frontend model. Therefore it flickers. Refactor!

    // take care of extracted Solr syntax
    // WARNING: this must be done first (before UP/DOWN mappings) as below routine potentially removes "suggestedSolrFieldName" attribute
    if (this.featureToggleService.getSyncToggleUiConceptAllRulesWithSolrFields()) {
      this.integrateSuggestedSolrFieldName(this.detailSearchInput.upDownRules);
      this.integrateSuggestedSolrFieldName(this.detailSearchInput.filterRules);
    }

    // take care of UP/DOWN mappings
    console.log(':: this.detailSearchInput.upDownRules = ' + this.detailSearchInput.upDownRules);
    if (this.featureToggleService.getSyncToggleUiConceptUpDownRulesCombined()) {
      if ((this.detailSearchInput.upDownRules !== null) && (this.detailSearchInput.upDownRules.length > 0)) {
        // convert from simple combined UP/DOWN dropdown definition mappings to detailed upDownType and bonus/malus value
        this.detailSearchInput.upDownRules = this.detailSearchInput.upDownRules.map(upDownRule => {
          return {
            id: upDownRule.id,
            term: upDownRule.term,
            upDownType: smm.upDownDropdownDefinitionMappings[upDownRule.upDownDropdownDefinitionMapping].upDownType,
            boostMalusValue: smm.upDownDropdownDefinitionMappings[upDownRule.upDownDropdownDefinitionMapping].boostMalusValue
          }
        });
      }
    }

    // and persist against REST backend
    this.searchManagementService
      .updateSearchInput(this.detailSearchInput)
      .then(res => {
        console.log('In SearchInputDetailComponent :: saveSearchInputDetails :: then :: res = ' + JSON.stringify(res));

        // reload detailSearchInput detail's model as well for updates on order of rules
        this.showDetailsForSearchInputWithId(this.detailSearchInput.id);

        // reload list for maybe updates on directed synonyms
        this.listComponent
          .reloadSearchInputListAfterDetailUpdate();

        console.log(':: parentComponent = ' + this.parentComponent);
        this.parentComponent
          .showSuccessMsg('Saving Details successful.');
      })
      .catch(error => this.handleError(error));
  }
}
