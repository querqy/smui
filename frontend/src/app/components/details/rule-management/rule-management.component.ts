import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnInit,
  Output,
  SimpleChanges,
  ChangeDetectorRef,
  AfterContentChecked
} from '@angular/core';
import { Observable } from 'rxjs';
import { debounceTime, distinctUntilChanged, map } from 'rxjs/operators';
import { DropdownSettings } from 'angular2-multiselect-dropdown/lib/multiselect.interface';

import {
  AssociatedSpelling,
  DeleteRule,
  FilterRule,
  InputTag,
  ListItem,
  ListItemType,
  RedirectRule,
  SearchInput,
  SuggestedSolrField,
  SynonymRule,
  UpDownRule,
  PreviewSection,
  PreviewItem
} from '../../../models';
import {
  CommonsService,
  FeatureToggleService,
  RuleManagementService,
  SpellingsService
} from '../../../services';

@Component({
  selector: 'app-smui-rule-management',
  templateUrl: './rule-management.component.html',
  styleUrls: ['./rule-management.component.css']
})
export class RuleManagementComponent implements OnChanges, OnInit, AfterContentChecked {
  @Input() selectedListItem?: ListItem;
  @Input() currentSolrIndexId?: string;
  @Input() listItems: ListItem[] = [];
  @Input() suggestedSolrFieldNames: string[] = [];
  @Input() showTags = false;
  @Input() allTags: InputTag[] = [];

  @Output() refreshAndSelectListItemById: EventEmitter<
    string
    > = new EventEmitter();
  @Output() executeWithChangeCheck: EventEmitter<any> = new EventEmitter();
  @Output() showErrorMsg: EventEmitter<string> = new EventEmitter();
  @Output() showSuccessMsg: EventEmitter<string> = new EventEmitter();
  @Output() openDeleteConfirmModal: EventEmitter<any> = new EventEmitter();

  detailSearchInput?: SearchInput;
  initDetailSearchInputHashForDirtyState?: string;
  saveError?: string;
  associatedSpellings: AssociatedSpelling[] = [];
  activateSpelling?: string = this.featureToggleService.getSyncToggleActivateSpelling();
  searchListItems: ListItem[] = [];
  tagsDropDownSettings?: DropdownSettings;
  availableTags: InputTag[] = [];
  selectedTags: InputTag[] = [];
  previewVisible: boolean;

  constructor(
    private commonsService: CommonsService,
    private ruleManagementService: RuleManagementService,
    private spellingService: SpellingsService,
    private changeDetector: ChangeDetectorRef,
    public featureToggleService: FeatureToggleService
  ) {}

  ngOnInit() {
    // @ts-ignore
    this.tagsDropDownSettings = {
      singleSelection: false,
      enableCheckAll: false,
      text: 'Tags',
      enableSearchFilter: true,
      badgeShowLimit: 2,
      labelKey: 'displayValue',
      noDataLabel: 'No Tags available'
    };

    this.previewVisible = true
  }

  ngOnChanges(changes: SimpleChanges) {
    console.log('In SearchInputDetailComponent :: ngOnChanges');

    if (this.selectedListItem) {
      this.showDetailsForSearchInputWithId(this.selectedListItem.id);
    }

    if (changes.selectedListItem && !this.selectedListItem) {
      this.showDetailsForSearchInputWithId(undefined);
    }

    if (changes.listItems && changes.listItems.currentValue) {
      this.searchListItems = this.filterSearchListItems(this.listItems);
    }
  }

  ngAfterContentChecked(): void {
    this.changeDetector.detectChanges();
  }

  // TODO open typeahead popup on focus -- focus$ = new Subject<string>();
  searchSuggestedSolrFieldNames = (text$: Observable<string>) =>
    text$.pipe(
      debounceTime(200),
      distinctUntilChanged(),
      map((term: string) =>
        this.suggestedSolrFieldNames === null
          ? []
          : (term === ''
            ? this.suggestedSolrFieldNames
            : this.suggestedSolrFieldNames.filter(
              (s: string) =>
                s.toLowerCase().indexOf(term.toLowerCase()) > -1
            )
          ).slice(0, 10)
      )
    );

  handleError(error: any) {
    console.log('In SearchInputDetailComponent :: handleError');
    console.log(':: error = ' + error);
    this.showErrorMsg.emit('An error occurred.'); // TODO Do a more detailled error description
  }

  // TODO consider evaluate a more elegant solution to dispatch upDownDropdownDefinitionMappings from smm to the template
  upDownDropdownDefinitionMappings() {
    return this.ruleManagementService.upDownDropdownDefinitionMappings;
  }

  onDeSelectAllTags() {
    this.selectedTags = [];
  }

  extractSuggestedSolrFieldName(objList: Array<any>) {
    // eslint-disable-next-line @typescript-eslint/prefer-for-of
    for (let i = 0; i < objList.length; i++) {
      // TODO 'term' and 'suggestedSolrFieldName' attributes are implicitly assumed. Change approach (maybe put into model or even backend)
      objList[i].suggestedSolrFieldName = '';
      if (objList[i].term !== null) {
        const term = objList[i].term.trim();
        if (term.length > 0) {
          const regexSolrSyntax = new RegExp('^\\*(.*?):');
          const found = term.match(regexSolrSyntax);
          if (found !== null && found.length > 1) {
            objList[i].suggestedSolrFieldName = found[1].trim();
            objList[i].term = objList[i].term.substr(
              found[0].length,
              objList[i].term.length - found[0].length
            );
          }
        }
      }
    }
  }

  integrateSuggestedSolrFieldName(objList: Array<any>) {
    // eslint-disable-next-line @typescript-eslint/prefer-for-of
    for (let i = 0; i < objList.length; i++) {
      if (objList[i].suggestedSolrFieldName) {
        if (objList[i].suggestedSolrFieldName.trim().length > 0) {
          objList[i].term =
            '* ' + objList[i].suggestedSolrFieldName + ':' + objList[i].term;
          // TODO not very elegant incl. 'delete' operator ... Refactor!
          delete objList[i].suggestedSolrFieldName;
        }
      }
    }
  }

  showDetailsForSearchInputWithId(searchInputId?: string) {
    console.log(
      'In SearchInputDetailComponent :: showDetailsForSearchInputWithId :: searchInputId = ' +
      searchInputId
    );

    if (!searchInputId) {
      this.detailSearchInput = undefined;
    } else {
      this.ruleManagementService
        .getDetailedSearchInput(searchInputId)
        .then(retSearchInput => {
          this.saveError = undefined;
          this.detailSearchInput = retSearchInput;

          if (this.showTags) {
            this.initTags(retSearchInput.tags);
          }

          // take care of extracted Solr syntax
          if (
            this.featureToggleService.getSyncToggleUiConceptAllRulesWithSolrFields()
          ) {
            this.extractSuggestedSolrFieldName(
              this.detailSearchInput.upDownRules
            );
            this.extractSuggestedSolrFieldName(
              this.detailSearchInput.filterRules
            );
          }

          // take care of UP/DOWN mappings
          this.encodeValuesToUpDownMappings();

          this.initDetailSearchInputHashForDirtyState = JSON.stringify(
            this.detailSearchInput
          ); // TODO hash string value

          this.changeDetector.detectChanges();
        })
        .then(() => this.findSpellingsForSearchInput())
        .catch(error => this.handleError(error));
    }
  }

  findSpellingsForSearchInput() {
    console.log('In SearchInputDetailComponent :: findSpellingsForSearchInput');

    if (
      this.activateSpelling &&
      this.detailSearchInput &&
      this.detailSearchInput.term !== ''
    ) {
      const subTerms = this.detailSearchInput.term.split(' ');
      this.associatedSpellings = subTerms
        .map(subTerm => {
          const term = this.commonsService.removeQuotes(subTerm);
          return {
            term,
            spellingItem: this.searchListItems.find(
              item => this.commonsService.removeQuotes(item.term) === term
            )
          };
        })
        .map(item => item.spellingItem
          ? new AssociatedSpelling(
            item.spellingItem.id,
            item.term,
            true,
            item.spellingItem.additionalTermsForSearch
          )
          : new AssociatedSpelling('', item.term, false, []));
    } else {
      this.associatedSpellings = [];
    }
  }

  createNewSpellingItemForTerm(term: string) {
    const createSpellingItemCallback = () => {
      if (this.currentSolrIndexId) {
        this.spellingService
          .addNewSpelling(this.currentSolrIndexId, term)
          .then(apiResult =>
            this.refreshAndSelectListItemById.emit(apiResult.returnId)
          )
          .catch(error => this.showErrorMsg.emit(error));
      }
    };

    this.executeWithChangeCheck.emit({
      executeFnOk: createSpellingItemCallback
    });
  }

  isDirty(): boolean {
    return this.detailSearchInput && this.initDetailSearchInputHashForDirtyState
      ? this.commonsService.isDirty(
        this.detailSearchInput,
        this.initDetailSearchInputHashForDirtyState
      )
      : false;
  }

  addNewSynonymRule() {
    console.log('In SearchInputDetailComponent :: addNewSynonym');

    const emptySynonymRule: SynonymRule = {
      id: this.randomUUID(),
      synonymType: 0,
      term: '',
      isActive: true
    };
    if (this.detailSearchInput) {
      this.detailSearchInput.synonymRules.push(emptySynonymRule);
    }
  }

  deleteSynonymRule(index: number) {
    console.log(
      'In SearchInputDetailComponent :: deleteSynonymRule :: index = ' + index
    );

    if (this.detailSearchInput) {
      this.detailSearchInput.synonymRules.splice(index, 1);
    }
  }

  addNewUpDownRule() {
    console.log('In SearchInputDetailComponent :: addNewUpDownRule');

    const emptyUpDownRule: UpDownRule = {
      id: this.randomUUID(),
      term: '',
      isActive: true
    };
    if (this.featureToggleService.getSyncToggleUiConceptUpDownRulesCombined()) {
      // NOTE: the attribute 'upDownDropdownDefinitionMapping' is frontend-only and not supposed to be part of REST transfer
      emptyUpDownRule.upDownDropdownDefinitionMapping = this.defaultIdxUpDownDropdownMappingForType();
    } else {
      emptyUpDownRule.upDownType = 0;
      emptyUpDownRule.boostMalusValue = 0;
    }

    if (this.detailSearchInput) {
      this.detailSearchInput.upDownRules.push(emptyUpDownRule);
    }
    this.changeDetector.detectChanges();
  }

  deleteUpDownRule(index: number) {
    console.log(
      'In SearchInputDetailComponent :: deleteUpDownRule :: index = ' + index
    );

    if (this.detailSearchInput) {
      this.detailSearchInput.upDownRules.splice(index, 1);
    }
  }

  addNewFilterRule() {
    console.log('In SearchInputDetailComponent :: addNewFilterRule');

    const emptyFilterRule: FilterRule = {
      id: this.randomUUID(),
      term: '',
      isActive: true
    };
    if (
      this.featureToggleService.getSyncToggleUiConceptAllRulesWithSolrFields()
    ) {
      emptyFilterRule.suggestedSolrFieldName = '';
    }

    if (this.detailSearchInput) {
      this.detailSearchInput.filterRules.push(emptyFilterRule);
    }
  }

  deleteFilterRule(index: number) {
    console.log(
      'In SearchInputDetailComponent :: deleteFilterRule :: index = ' + index
    );

    if (this.detailSearchInput) {
      this.detailSearchInput.filterRules.splice(index, 1);
    }
  }

  addNewDeleteRule() {
    console.log('In SearchInputDetailComponent :: addNewDeleteRule');

    const emptyDeleteRule: DeleteRule = {
      id: this.randomUUID(),
      term: '',
      isActive: true
    };

    if (this.detailSearchInput) {
      this.detailSearchInput.deleteRules.push(emptyDeleteRule);
    }
  }

  deleteDeleteRule(index: number) {
    console.log(
      'In SearchInputDetailComponent :: deleteDeleteRule :: index = ' + index
    );

    if (this.detailSearchInput) {
      this.detailSearchInput.deleteRules.splice(index, 1);
    }
  }

  addNewRedirectRule() {
    console.log('In SearchInputDetailComponent :: addNewRedirectRule');

    const emptyRedirectRule: RedirectRule = {
      id: this.randomUUID(),
      target: '',
      isActive: true
    };

    if (this.detailSearchInput) {
      this.detailSearchInput.redirectRules.push(emptyRedirectRule);
    }
  }

  deleteRedirectRule(index: number) {
    console.log(
      'In SearchInputDetailComponent :: deleteRedirectRule :: index = ' + index
    );

    if (this.detailSearchInput) {
      this.detailSearchInput.redirectRules.splice(index, 1);
    }
  }

  saveSearchInputDetails() {
    console.log('In SearchInputDetailComponent :: saveSearchInputDetails');

    // TODO routine directly operating on this.detailSearchInput frontend model. Therefore it flickers. Refactor!

    // take care of extracted Solr syntax
    // WARNING: this must be done first (before UP/DOWN mappings) as below routine potentially removes 'suggestedSolrFieldName' attribute
    if (this.detailSearchInput) {
      const { upDownRules, filterRules } = this.detailSearchInput;

      if (
        this.featureToggleService.getSyncToggleUiConceptAllRulesWithSolrFields()
      ) {
        this.integrateSuggestedSolrFieldName(upDownRules);
        this.integrateSuggestedSolrFieldName(filterRules);
      }

      this.updateSelectedTagsInModel();

      // take care of UP/DOWN mappings
      this.decodeUpDownMappingsToValues();

      // and persist against REST backend
      this.ruleManagementService
        .updateSearchInput(this.detailSearchInput)
        .then(apiResult =>
          this.refreshAndSelectListItemById.emit(apiResult.returnId)
        )
        .then(_ => this.showSuccessMsg.emit('Saving Details successful.'))
        .catch(error => {
          if (error.status === 400) {
            console.log(
              ':: ruleManagementService :: catch :: error = ' +
              JSON.stringify(error)
            );
            // take care of extracted Solr syntax and UP/DOWN mappings
            if (
              this.featureToggleService.getSyncToggleUiConceptAllRulesWithSolrFields()
            ) {
              this.extractSuggestedSolrFieldName(upDownRules);
              this.extractSuggestedSolrFieldName(filterRules);
            }
            this.encodeValuesToUpDownMappings();
            // pass message to frontend
            const msg = error.error.message;
            this.saveError = msg.split('\n');
          } else {
            this.showErrorMsg.emit(error);
          }
        });
    }
  }

  deleteSearchInput() {
    if (this.detailSearchInput) {
      const { id } = this.detailSearchInput;
      const deleteCallback = () =>
        this.ruleManagementService
          .deleteSearchInput(id)
          .then(() => this.refreshAndSelectListItemById.emit(undefined))
          .catch(error => this.showErrorMsg.emit(error));

      this.openDeleteConfirmModal.emit({ deleteCallback });
    }
  }

  openDetailsForSpelling(spellingId: string) {
    this.executeWithChangeCheck.emit({
      executeFnOk: () => this.refreshAndSelectListItemById.emit(spellingId)
    });
  }

  private availableTagsForCurrentSolrIndex(): InputTag[] {
    return this.allTags.filter(
      tag => !tag.solrIndexId || tag.solrIndexId === this.currentSolrIndexId
    );
  }

  private initTags(tags: InputTag[]) {
    this.availableTags = this.availableTagsForCurrentSolrIndex();
    this.selectedTags = tags;
  }

  // TODO add tests for different UP/DOWN mapping definitions

  // Helper to find the closest matching mapping to an existing persisted UP/DOWN rule
  // TODO heaps of implicit knowledge about the model is necessary here. Consider refactoring (maybe model or even backend)

  private defaultIdxUpDownDropdownMappingForType() {
    // make that value depend on featureToggleService.getSyncToggleCustomUpDownDropdownMappings()
    const upDownMappings = this.featureToggleService.getSyncToggleCustomUpDownDropdownMappings();
    let idxMinimumForType = 0;
    for (let i = 1; i < upDownMappings.length; i++) {
      if (
        upDownMappings[i].boostMalusValue <
        upDownMappings[idxMinimumForType].boostMalusValue
      ) {
        idxMinimumForType = i;
      }
    }
    return idxMinimumForType;
  }

  private findIdxClosestUpDownDropdownDefinitionMapping({
    boostMalusValue,
    upDownType
  }: UpDownRule) {
    // get UP/DOWN mapping definitions from the configuration (featureToggleService) as array of objects, that look like, e.g.:
    // > {"displayName":"UP(+++++)","upDownType":0,"boostMalusValue":500}
    const upDownMappings = this.featureToggleService.getSyncToggleCustomUpDownDropdownMappings();

    const boostMalusValueForUpDownType = (type: number | undefined) => {
      const boostMalusValues = [];
      for (let i = 0; i < upDownMappings.length; i++) {
        if (type === upDownMappings[i].upDownType) {
          boostMalusValues.push({
            idx: i,
            boostMalusValue: upDownMappings[i].boostMalusValue
          });
        }
      }
      return boostMalusValues;
    };

    const arrayComparison = boostMalusValueForUpDownType(upDownType);

    // Find minimum distance to comparison array
    // TODO Consider a more elegant functional solution
    let idxMinimumDistance = arrayComparison[0].idx;
    if (boostMalusValue) {
      let lastMinimumDistance = Math.abs(
        arrayComparison[0].boostMalusValue - boostMalusValue
      );
      for (let i = 1; i < arrayComparison.length; i++) {
        const nextMinimumDistance = Math.abs(
          arrayComparison[i].boostMalusValue - boostMalusValue
        );
        if (nextMinimumDistance < lastMinimumDistance) {
          lastMinimumDistance = nextMinimumDistance;
          idxMinimumDistance = arrayComparison[i].idx;
        }
      }
    }

    return idxMinimumDistance;
  }

  // taken from https://stackoverflow.com/questions/105034/create-guid-uuid-in-javascript
  private randomUUID() {
    /* eslint-disable */
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
      const r = (Math.random() * 16) | 0,
        v = c == 'x' ? r : (r & 0x3) | 0x8
      return v.toString(16)
    })
    /* eslint-enable */
  }

  private updateSelectedTagsInModel() {
    if (this.detailSearchInput) {
      this.detailSearchInput.tags = this.selectedTags;
    }
  }

  private encodeValuesToUpDownMappings() {
    if (
      this.detailSearchInput &&
      this.featureToggleService.getSyncToggleUiConceptUpDownRulesCombined()
    ) {
      const { upDownRules } = this.detailSearchInput;
      if (upDownRules && upDownRules.length > 0) {
        // convert to simple combined UP/DOWN dropdown definition mappings
        // TODO consider a more elegant functional solution
        // eslint-disable-next-line @typescript-eslint/prefer-for-of
        for (
          let idxUpDownRules = 0;
          idxUpDownRules < upDownRules.length;
          idxUpDownRules++
        ) {
          upDownRules[
            idxUpDownRules
          ].upDownDropdownDefinitionMapping = this.findIdxClosestUpDownDropdownDefinitionMapping(
            upDownRules[idxUpDownRules]
          );
        }
      }
    }
  }

  private decodeUpDownMappingsToValues() {
    if (
      this.detailSearchInput &&
      this.featureToggleService.getSyncToggleUiConceptUpDownRulesCombined()
    ) {
      const { upDownRules } = this.detailSearchInput;
      if (upDownRules && upDownRules.length > 0) {
        // convert from simple combined UP/DOWN dropdown definition mappings to detailed upDownType and bonus/malus value
        this.detailSearchInput.upDownRules = upDownRules.map(
          (upDownRule: UpDownRule, index: number) => {
            const { upDownDropdownDefinitionMapping } = upDownRule;
            let idx = upDownDropdownDefinitionMapping === undefined ? -1 : upDownDropdownDefinitionMapping
            if (idx == -1) throw Error("upDownDropdownDefinitionMapping was undefined");
            return {
              id: upDownRule.id,
              term: upDownRule.term,
              upDownType: this.ruleManagementService.upDownDropdownDefinitionMappings[idx].upDownType,
              boostMalusValue: this.ruleManagementService.upDownDropdownDefinitionMappings[idx].boostMalusValue,
              isActive: upDownRule.isActive
            };
          }
        );
      }
    }
  }

  private filterSearchListItems(listItems: ListItem[]) {
    return listItems.filter(
      item =>
        item.itemType.toString() ===
        ListItemType[ListItemType.Spelling].toString()
    );
  }

  warn_searchinput_exact(): boolean {
    if(!this.detailSearchInput) {
      return false
    } else {
      const trimmedTerm = this.detailSearchInput.term.trim()
      return trimmedTerm.startsWith('"') && trimmedTerm.endsWith('"')
    }
  }

  previewData(): PreviewSection[] {
    return [
      new PreviewSection('LIVE', [
        new PreviewItem('rule1','https://domain.tld/search/?query=rule1'),
        new PreviewItem('rule2','https://domain.tld/search/?query=rule2')
      ]),
      new PreviewSection('PRELIVE (Staging)', [
        new PreviewItem('rule1','https://staging-domain.tld/search/?query=rule1'),
        new PreviewItem('rule2','https://staging-domain.tld/search/?query=rule2')
      ]),
    ]
  }

}
