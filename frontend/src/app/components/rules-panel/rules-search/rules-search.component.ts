import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnChanges,
  SimpleChanges
} from '@angular/core';

import {
  FeatureToggleService,
  ModalService,
  RuleManagementService,
  SpellingsService,
  TagsService
} from '../../../services';
import Papa from 'papaparse';
import {InputTag, ListItem} from '../../../models';

@Component({
  selector: 'app-smui-rules-search',
  templateUrl: './rules-search.component.html',
  styleUrls: ['./rules-search.component.css']
})
export class RulesSearchComponent implements OnChanges {
  @Input() currentSolrIndexId?: string;
  @Input() searchInputTerm?: string;
  @Input() appliedTagFilter?: InputTag = undefined;
  @Input() listItems: ListItem[] = [];

  @Output() searchInputTermChange: EventEmitter<string> = new EventEmitter();
  @Output() appliedTagFilterChange: EventEmitter<InputTag> = new EventEmitter();
  @Output() refreshAndSelectListItemById: EventEmitter<string> = new EventEmitter();
  @Output() executeWithChangeCheck: EventEmitter<any> = new EventEmitter();
  @Output() showSuccessMsg: EventEmitter<string> = new EventEmitter();
  @Output() showErrorMsg: EventEmitter<string> = new EventEmitter();

  allTags: InputTag[] = [];
  readonly isTaggingActive = this.featureToggleService.isRuleTaggingActive();
  private readonly isSpellingActive = this.featureToggleService.getSyncToggleActivateSpelling();

  constructor(
    private featureToggleService: FeatureToggleService,
    private ruleManagementService: RuleManagementService,
    private spellingsService: SpellingsService,
    private tagsService: TagsService,
    private modalService: ModalService
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.listItems && changes.listItems.currentValue && this.isTaggingActive) {
      this.refreshTags(changes.listItems.currentValue);
    }
  }

  filterByTag(tag?: InputTag) {
    this.appliedTagFilter = undefined;
    this.appliedTagFilterChange.emit(tag);
  }

  createItem(): void {
    this.executeWithChangeCheck.emit({
      executeFnOk: this.isSpellingActive
        ? () => this.modalService.open('create-modal')
        : () => this.createNewRuleItem()
    });
  }

  openFileModal(): void {
    this.modalService.open('file-import');
  }

  fileSelect(event: Event): void {
    const element = event.currentTarget as HTMLInputElement;
    const file = element?.files?.[0];
    Papa.parse(file, {
      complete: (results) => {
        const ruleCreations = results
          .data.filter(row => row.length === 3)
          .map(row => {
            console.log({row});
            this.ruleManagementService
              .addNewRuleItem(this.currentSolrIndexId, row[0], [])
              .then(ruleId => {
                console.log(ruleId);
                this.ruleManagementService.updateSearchInput({
                  id: ruleId.returnId,
                  term: row[0],
                  synonymRules: [{term: row[1], isActive: true, synonymType: 0, id: this.randomUUID()}],
                  isActive: true,
                  redirectRules: [],
                  deleteRules: [],
                  filterRules: [],
                  tags: [],
                  upDownRules: [],
                  comment: row[2],
                  term: row[0]
                })
                  .then(ruleId =>
                    this.refreshAndSelectListItemById.emit(ruleId.returnId)
                  );
              })
          });
        Promise.all(ruleCreations).then(
          () => this.modalService.close('file-import')
        );
      }
    });
  }

  createNewSpellingItem() {
    if (this.currentSolrIndexId) {
      this.spellingsService
        .addNewSpelling(this.currentSolrIndexId, this.searchInputTerm)
        .then(spellingId =>
          this.refreshAndSelectListItemById.emit(spellingId.returnId)
        )
        .then(() => this.changeSearchInput(''))
        .then(() => this.modalService.close('create-modal'))
        .catch(error => this.showErrorMsg.emit(error.error.message));
    }
  }

  private randomUUID() {
    /* eslint-disable */
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
      const r = (Math.random() * 16) | 0,
        v = c == 'x' ? r : (r & 0x3) | 0x8
      return v.toString(16)
    })
    /* eslint-enable */
  }

  createNewRuleItem() {
    if (this.currentSolrIndexId) {
      const tags = this.appliedTagFilter ? [this.appliedTagFilter.id] : [];
      this.ruleManagementService
        .addNewRuleItem(this.currentSolrIndexId, this.searchInputTerm, tags)
        .then(spellingId =>
          this.refreshAndSelectListItemById.emit(spellingId.returnId)
        )
        .then(() => this.changeSearchInput(''))
        .then(() => this.modalService.close('create-modal'))
        .catch(error => this.showErrorMsg.emit(error.error.message));
    }
  }

  private refreshTags(listItems: ListItem[]) {
    this.allTags = this.tagsService.getAllTagsFromListItems(listItems);

    // Reset tagFilter if the tag is no longer available in the current search inputs
    if (this.appliedTagFilter) {
      const {displayValue} = this.appliedTagFilter;
      if (this.allTags.filter(tag => tag.displayValue === displayValue).length === 0) {
        this.filterByTag(undefined);
      }
    }
  }

  private changeSearchInput(value: string) {
    this.searchInputTerm = value;
    this.searchInputTermChange.emit(value);
  }
}
