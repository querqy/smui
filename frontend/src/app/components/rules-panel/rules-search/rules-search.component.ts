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
import {rowsToSearchInputs} from '../../../lib/csv';
const fileImportModal = 'file-import';

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
    this.modalService.open(fileImportModal);
  }

  fileSelect(event: Event): void {
    const element = event.currentTarget as HTMLInputElement;
    const file = element?.files?.[0];
    Papa.parse(file, {
      complete: (results) => {
        const searchInputs = rowsToSearchInputs(results.data);
        const ruleCreations = searchInputs
          .reduce((chain, searchInput) => {
            return chain
              .then(() => {
                return this.ruleManagementService.addNewRuleItem(this.currentSolrIndexId, searchInput.term, [])
                  .then(inputId => {
                    searchInput.id = inputId.returnId;
                    return this.ruleManagementService.updateSearchInput(searchInput)
                  });
              });
          }, Promise.resolve());
        ruleCreations
          .then(
            () => {
              this.modalService.close(fileImportModal)
              this.refreshAndSelectListItemById.emit(searchInputs[0].id);
            }
          )
          .error(err => this.showErrorMsg.emit(err.message));
      },
      error: (err, file, inputElem, reason) => {
        this.showErrorMsg.emit(err);
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
