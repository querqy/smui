import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges } from '@angular/core';

import {FeatureToggleService, RuleManagementService, SpellingsService, TagsService} from '../../../services/index';
import {InputTag, ListItem} from '../../../models/index';

declare var $: any; // TODO include @types/jquery properly, make this workaround unnecessary

@Component({
  selector: 'smui-rules-search',
  templateUrl: './rules-search.component.html',
  styleUrls: ['./rules-search.component.css']
})
export class RulesSearchComponent implements OnChanges {

  @Input() currentSolrIndexId = '-1';
  @Input() searchInputTerm = '';
  @Input() appliedTagFilter: InputTag = null;
  @Input() listItems: ListItem[] = [];

  @Output() searchInputTermChange: EventEmitter<string> = new EventEmitter();
  @Output() appliedTagFilterChange: EventEmitter<InputTag> = new EventEmitter();
  @Output() refreshAndSelectListItemById: EventEmitter<string> = new EventEmitter();
  @Output() executeWithChangeCheck: EventEmitter<any> = new EventEmitter();
  @Output() showSuccessMsg: EventEmitter<string> = new EventEmitter();
  @Output() showErrorMsg: EventEmitter<string> = new EventEmitter();

  private readonly isSpellingActive = this.featureToggleService.getSyncToggleActivateSpelling();
  private readonly isTaggingActive = this.featureToggleService.isRuleTaggingActive();
  private allTags: InputTag[] = [];

  constructor(
    private featureToggleService: FeatureToggleService,
    private ruleManagementService: RuleManagementService,
    private spellingsService: SpellingsService,
    private tagsService: TagsService
  ) { }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.listItems && changes.listItems.currentValue) {
      this.refreshTags(changes.listItems.currentValue)
    }
  }

  private refreshTags(listItems: ListItem[]) {
    this.allTags = this.tagsService.getAllTagsFromListItems(listItems);

    // Reset tagFilter if the tag is no longer available in the current search inputs
    if (this.appliedTagFilter) {
      if (this.allTags.filter(tag => tag.displayValue === this.appliedTagFilter.displayValue).length === 0) {
        this.filterByTag(null)
      }
    }
  }

  private filterByTag(tag: InputTag) {
    this.appliedTagFilter = null;
    this.appliedTagFilterChange.emit(tag)
  }

  private changeSearchInput(value: string) {
    this.searchInputTerm = value;
    this.searchInputTermChange.emit(value)
  }

  private createItem(): void {
    this.executeWithChangeCheck.emit({
      executeFnOk: this.isSpellingActive ? this.openAddItemModal : this.createNewRuleItem,
      executeFnCancel: () => ({})
    })
  }

  private openAddItemModal() {
    $('#addItemModal').modal('show');
  }

  private createNewSpellingItem() {
    this.spellingsService.addNewSpelling(this.currentSolrIndexId, this.searchInputTerm)
      .then(spellingId => this.refreshAndSelectListItemById.emit(spellingId))
      .catch(error => this.showErrorMsg.emit(error))
  }

  private createNewRuleItem() {
    const tags = this.appliedTagFilter ? [this.appliedTagFilter.id] : [];
    this.ruleManagementService.addNewRuleItem(this.currentSolrIndexId, this.searchInputTerm, tags)
      .then(spellingId => this.refreshAndSelectListItemById.emit(spellingId))
      .catch(error => this.showErrorMsg.emit(error))
  }

}
