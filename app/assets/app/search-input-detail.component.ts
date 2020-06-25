import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/merge';
import 'rxjs/add/operator/filter';
import 'rxjs/add/operator/debounceTime';
import 'rxjs/add/operator/distinctUntilChanged';

import * as smm from './search-management.model';
import { SearchManagementService } from './search-management.service';
import { FeatureToggleService } from './feature-toggle.service';
import { SearchInputListComponent } from './search-input-list.component';
// import { ActivatedRoute, Params } from '@angular/router';

const DEFAULT_IDX_UP_DOWN_DROPDOWN_DEFINITION_MAPPING = 4;
declare var $: any; // For jquery

@Component({
  selector: 'smui-search-input-detail',
  templateUrl: './search-input-detail.component.html',
  styleUrls: ['./search-input-detail.component.css'],
  providers: [FeatureToggleService]
})
export class SearchInputDetailComponent implements OnInit {

  @Input() listComponent: SearchInputListComponent;
  @Input() selectedListItem = null;
  @Input() currentSolrIndexId = '-1';
  @Input() allInputTags: smm.InputTag[] = [];
  @Input() searchListItems: smm.ListItem[] = [];

  @Output() showErrorMsg: EventEmitter<string> = new EventEmitter();
  @Output() showSuccessMsg: EventEmitter<string> = new EventEmitter();
  @Output() refreshItemsInList: EventEmitter<string> = new EventEmitter();
  @Output() deleteItemByType: EventEmitter<any> = new EventEmitter();
  @Output() selectListItemById: EventEmitter<string> = new EventEmitter();
  @Output() createItem: EventEmitter<any> = new EventEmitter();

  private detailSearchInput: smm.SearchInput = null;
  private initDetailSearchInputHashForDirtyState: string = null;
  private suggestedSolrFieldNames = null;
  private showTags: Boolean = false;
  private availableTags: smm.InputTag[] = [];
  private saveError: string = null;
  private previousTagEventHandler = null;
  private associatedSpellings: smm.AssociatedSpelling[] = [];

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
  }

  ngOnChanges() {
    console.log('In SearchInputDetailComponent :: ngOnChanges');

    if (this.selectedListItem) {
      this.showDetailsForSearchInputWithId(this.selectedListItem.id)
    }
  }

  private availableTagsForCurrentSolrIndex() {
    return this.allInputTags.filter(tag => !tag.solrIndexId || tag.solrIndexId === this.currentSolrIndexId);
  }

  public loadSuggestedSolrFieldsForSolrIndexWithId(solrIndexId: string) {
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
    this.showErrorMsg.emit('An error occurred.'); // TODO Do a more detaillied error description
  }

  // TODO consider evaluate a more elegant solution to dispatch upDownDropdownDefinitionMappings from smm to the template
  public upDownDropdownDefinitionMappings() {
    return smm.upDownDropdownDefinitionMappings;
  }

  private initTags(tags: smm.InputTag[]) {
    this.availableTags = this.availableTagsForCurrentSolrIndex();
    this.showTags = this.featureToggleService.isRuleTaggingActive();
    
    const elem = $('.inputTags')
    if (this.previousTagEventHandler) {
      elem.off('tokenize:tokens:added tokenize:tokens:remove', this.previousTagEventHandler);
    }

    // Create tag input from multiselect input
    elem.tokenize2({ placeholder: 'Tags', dropdownMaxItems: 20, searchFromStart: false });
    // Remove previous event handlers that update the model
    //elem.off('tokenize:tokens:added	tokenize:tokens:remove')
    // Remove all previously selected tags and add all current tags
    elem.tokenize2().trigger('tokenize:clear');
    tags.forEach(tag => {
      elem.tokenize2().trigger('tokenize:tokens:add', [tag.id, tag.displayValue, true]);
    });
    const handler = () => {
      this.updateSelectedTagsInModel();
    }
    this.previousTagEventHandler = handler;
    // Register event handlers that update the model value on change
    elem.on('tokenize:tokens:added tokenize:tokens:remove', handler);
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

  public showDetailsForSearchInputWithId(searchInputId: string) {
    console.log('In SearchInputDetailComponent :: showDetailsForSearchInputWithId :: searchInputId = ' + searchInputId);

    if (searchInputId === null) {
      this.detailSearchInput = null;
      this.showTags = false;
    } else {
      this.searchManagementService
        .getDetailedSearchInput(searchInputId)
        .then(retSearchInput => {
          this.saveError = null
          this.initTags(retSearchInput.tags)
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
        .then(() => this.findSpellingsForSearchInput())
        .catch(error => this.handleError(error));
    }
  }

  private findSpellingsForSearchInput() {
    console.log('In SearchInputDetailComponent :: findSpellingsForSearchInput');

    if (this.detailSearchInput && this.detailSearchInput.term !== '') {
      const subTerms = this.detailSearchInput.term.split(' ');
      this.associatedSpellings = subTerms
        .map(subTerm => { return {
          term: subTerm,
          spellingItem: this.searchListItems.find(item => item.term === subTerm)}
        })
        .map(item => {
          return item.spellingItem ?
              new smm.AssociatedSpelling(item.spellingItem.id, item.term, true, item.spellingItem.additionalTermsForSearch)
            :
              new smm.AssociatedSpelling('', item.term, false, [])
        })
    } else {
      this.associatedSpellings = []
    }
  }

  public openDetailsForSpelling(id: string) {
    this.selectListItemById.emit(id)
  }

  public createNewSpellingItemForTerm(term: string) {
    const apiCall = () => this.searchManagementService.addNewSpellingItem(this.currentSolrIndexId, term);
    this.createItem.emit({ itemType: smm.ListItemType.Spelling, apiCall })
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
      id: this.randomUUID(),
      synonymType: 0,
      term: '',
      isActive: true
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
      id: this.randomUUID(),
      term: '',
      isActive: true
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
      id: this.randomUUID(),
      term: '',
      isActive: true
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
      id: this.randomUUID(),
      term: '',
      isActive: true
    };
    this.detailSearchInput
      .deleteRules.push(emptyDeleteRule);
  }

  // taken from https://stackoverflow.com/questions/105034/create-guid-uuid-in-javascript
  private randomUUID() {
    /* tslint:disable */
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
      const r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
      return v.toString(16);
    });
    /* tslint:enable */
  }

  public deleteDeleteRule(index: number) {
    console.log('In SearchInputDetailComponent :: deleteDeleteRule :: index = ' + index);

    this.detailSearchInput.deleteRules.splice(index, 1);
  }

  private currentlySelectedTags() {
    const ids: string[] = $('.inputTags').val();
    return this.availableTagsForCurrentSolrIndex().filter(tag => ids.indexOf(tag.id) !== -1)
  }

  private updateSelectedTagsInModel() {
    this.detailSearchInput.tags = this.currentlySelectedTags()
  }

  public addNewRedirectRule() {
    console.log('In SearchInputDetailComponent :: addNewRedirectRule');

    const emptyRedirectRule: smm.RedirectRule = {
      id: this.randomUUID(),
      target: '',
      isActive: true
    };
    this.detailSearchInput
        .redirectRules.push(emptyRedirectRule);
  }

  public deleteRedirectRule(index: number) {
    console.log('In SearchInputDetailComponent :: deleteRedirectRule :: index = ' + index);

    this.detailSearchInput.redirectRules.splice(index, 1);
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

    this.updateSelectedTagsInModel()

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
            boostMalusValue: smm.upDownDropdownDefinitionMappings[upDownRule.upDownDropdownDefinitionMapping].boostMalusValue,
            isActive: upDownRule.isActive
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
        this.refreshItemsInList.emit(this.currentSolrIndexId);

        this.saveError = null;
        this.showSuccessMsg.emit('Saving Details successful.');
      })
      .catch(error => {
        if (error.status === 400) {
          this.saveError = error.json().message
        } else {
          this.handleError(error);
        }
      });
  }

  public deleteSearchInputWithId(searchInputId: string) {
    console.log(`In SearchInputListComponent :: deleteSearchInputWithId :: id = ${searchInputId}`);
    // TODO maybe before even starting the deletion process, check if details are dirty and ask to cancel editing eventually
    // TODO reselect selected index, if deleted entry was the selected one
    // TODO reselect selected index, if deleted entry was the first one
    this.deleteItemByType.emit({itemType: 'RuleManagement', id: searchInputId});
  }
}
