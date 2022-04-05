import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnChanges,
  SimpleChanges
} from '@angular/core';

import {
  CommonsService,
  FeatureToggleService,
  ListItemsService,
  RuleManagementService,
  SpellingsService
} from '../../../services';
import { InputTag, ListItem } from '../../../models';

@Component({
  selector: 'app-smui-rules-list',
  templateUrl: './rules-list.component.html',
  styleUrls: ['./rules-list.component.css']
})
export class RulesListComponent implements OnChanges {
  @Input() currentSolrIndexId?: string;
  @Input() searchInputTerm?: string;
  @Input() appliedTagFilter?: InputTag;
  @Input() selectedListItem?: ListItem;
  @Input() listItems: Array<ListItem> = [];

  @Output() selectedListItemChange = new EventEmitter<ListItem>();
  @Output() listItemsChange = new EventEmitter<Array<ListItem>>();
  @Output() openDeleteConfirmModal: EventEmitter<any> = new EventEmitter();
  @Output() executeWithChangeCheck: EventEmitter<any> = new EventEmitter();
  @Output() showErrorMsg: EventEmitter<string> = new EventEmitter();

  readonly limitItemsTo: number = +this.featureToggleService.getSyncToggleUiListLimitItemsTo();
  isShowingAllItems: boolean = this.limitItemsTo < 0;

  constructor(
    private featureToggleService: FeatureToggleService,
    private ruleManagementService: RuleManagementService,
    private spellingsService: SpellingsService,
    private listItemsService: ListItemsService,
    private commonService: CommonsService
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (this.commonService.hasChanged(changes, 'currentSolrIndexId')) {
      this.refreshItemsInList().catch(error => this.showErrorMsg.emit(error));
    }
  }

  refreshItemsInList() {
    return this.currentSolrIndexId
      ? this.listItemsService
          .getAllItemsForInputList(this.currentSolrIndexId)
          .then(listItems => {
            this.listItems = listItems;
            this.listItemsChange.emit(listItems);
            this.searchInputTerm = '';
          })
      : Promise.reject('No selected Solr index');
  }

  refreshAndSelectListItemById(listItemId: string) {
    return this.refreshItemsInList()
      .then(() => {
        const listItem = this.listItems.find(item => item.id === listItemId);
        this.selectListItem(listItem || undefined);
      })
      .catch(error => this.showErrorMsg.emit(error));
  }

  getFilteredListItems(): ListItem[] {
    if (this.searchInputTerm || this.appliedTagFilter) {
      return this.listItems.filter(item => (
          this.listItemContainsString(item) &&
          this.listItemContainsTag(item)
        ));
    }

    return this.listItems;
  }

  selectListItemWithCheck(listItem: ListItem) {
    this.executeWithChangeCheck.emit({
      executeFnOk: () => this.selectListItem(listItem)
    });
  }

  deleteSpellingItem(id: string, event: Event) {
    event.stopPropagation();
    const deleteCallback = () =>
      this.spellingsService
        .deleteSpelling(id)
        .then(() => this.refreshItemsInList())
        .then(() => this.selectListItem(undefined))
        .catch(error => this.showErrorMsg.emit(error));

    this.openDeleteConfirmModal.emit({ deleteCallback });
  }

  deleteRuleItem(id: string, event: Event) {
    event.stopPropagation();
    const deleteCallback = () =>
      this.ruleManagementService
        .deleteSearchInput(id)
        .then(() => this.refreshItemsInList())
        .then(() => this.selectListItem(undefined))
        .catch(error => this.showErrorMsg.emit(error));

    this.openDeleteConfirmModal.emit({ deleteCallback });
  }

  toggleShowMore() {
    this.isShowingAllItems = !this.isShowingAllItems;
  }

  private selectListItem(listItem?: ListItem) {
    console.log(
      `In SearchInputListComponent :: selectListItem :: id = ${
        listItem ? JSON.stringify(listItem) : 'null'
      }`
    );

    this.selectedListItem = listItem;
    this.selectedListItemChange.emit(listItem);
  }

  private listItemContainsString(item: ListItem): boolean {
    const searchTermLower = (this.searchInputTerm || '').trim().toLowerCase();

    const searchTermIncludesString = (s: string) => s.toLowerCase().indexOf(searchTermLower) !== -1;

    if (searchTermLower.length === 0) {
      return true;
    }

    if (searchTermIncludesString(item.term)) {
      return true;
    }

    if (searchTermLower == item.id) {
      return true;
    }

    // otherwise, we have a chance in the synonyms ...
    // TODO evaluate to check for undirected synonyms (synonymType) only
    for (const s of item.synonyms) {
      if (searchTermIncludesString(s)) {
        return true;
      }
    }

    for (const at of item.additionalTermsForSearch) {
      if (searchTermIncludesString(at)) {
        return true;
      }
    }

    return false;
  }

  private listItemContainsTag(i: ListItem): boolean {
    if (!this.appliedTagFilter) {
      return true;
    }

    for (const t of i.tags) {
      if (t.id === this.appliedTagFilter.id) {
        return true;
      }
    }
    return false;
  }
}
