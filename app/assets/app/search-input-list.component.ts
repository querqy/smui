import {Component, Input, Output, EventEmitter, OnInit} from '@angular/core';

import * as smm from './search-management.model';
import {ListItem, ListItemType} from './search-management.model';
import {SearchManagementService} from './search-management.service';
import {FeatureToggleService} from './feature-toggle.service';

declare var $: any; // TODO include @types/jquery properly, make this workaround unnecessary

@Component({
  selector: 'smui-search-input-list',
  templateUrl: './search-input-list.component.html',
  styleUrls: ['./search-input-list.component.css'],
  providers: [FeatureToggleService]
})
export class SearchInputListComponent implements OnInit {

  @Input() currentSolrIndexId = '-1';
  @Input() selectedListItem: ListItem = null;
  @Output() selectedListItemChange = new EventEmitter<ListItem>();
  @Output() executeWithChangeCheck: EventEmitter<any> = new EventEmitter();
  @Output() showErrorMsg: EventEmitter<string> = new EventEmitter();
  @Output() createItem: EventEmitter<any> = new EventEmitter();
  @Output() deleteItemByType: EventEmitter<any> = new EventEmitter();

  public allTags: smm.InputTag[] = [];
  private listItems: smm.ListItem[] = [];
  private tagFilter: smm.InputTag = null;
  private searchInputTerm = '';
  private limitItemsTo = +this.featureToggleService.getSyncToggleUiListLimitItemsTo();
  private showMoreButton = false;
  private isShowingAllItems = false;

  constructor(
    private searchManagementService: SearchManagementService,
    public featureToggleService: FeatureToggleService /*, TODO use or remove "route" DI
    private route: ActivatedRoute */) {
  }

  ngOnInit() {
    console.log('In SearchInputSearchComponent :: ngOnInit');
  }

  valuechange(value) {
    console.log(value)
  }

  handleError(error: any) {
    console.log('In SearchInputSearchComponent :: handleError');
    console.log(':: error = ' + error);
    this.showErrorMsg.emit('An error occurred.'); // TODO Do a more detaillied error description
  }

  private listItemContainsString(item: smm.ListItem, searchTermLower: string): Boolean {
    if (searchTermLower.length === 0) {
      return true;
    }
    if (item.term.toLowerCase().indexOf(searchTermLower) !== -1) {
      return true;
    }
    // otherwise, we have a chance in the synonyms ...
    // TODO evaluate to check for undirected synonyms (synonymType) only
    for (const s of item.synonyms) {
      if (s.toLowerCase().indexOf(searchTermLower) !== -1) {
        return true;
      }
    }
    return false;
  }

  private listItemContainsTag(i: smm.ListItem, tag: smm.InputTag): Boolean {
    if (!tag) {
      return true;
    }
    for (const t of i.tags) {
      if (t.id === tag.id) {
        return true;
      }
    }
    return false;
  }

  public isTaggingActive(): Boolean {
    return this.featureToggleService.isRuleTaggingActive()
  }

  public getFilteredListItems(): smm.ListItem[] {
    const searchTerm = this.searchInputTerm.trim().toLowerCase();
    this.setShowMoreButtonState();

    if (searchTerm.length > 0 || this.tagFilter) {
      return this.listItems.filter(item => {
        return this.listItemContainsString(item, searchTerm) && this.listItemContainsTag(item, this.tagFilter)
      });
    } else {
      return this.listItems;
    }
  }

  public openAddItemModal() {
    console.log('In SearchInputSearchComponent :: openAddItemModal');
    $('#addItemModal').modal('show');
  }

  createNewSpellingItem() {
    const apiCall = () => this.searchManagementService.addNewSpellingItem(this.currentSolrIndexId, this.searchInputTerm);
    this.createItem.emit({itemType: ListItemType.Spelling, apiCall})
  }

  createNewRuleItem() {
    const tags = this.tagFilter ? [this.tagFilter.id] : [];
    const apiCall = () => this.searchManagementService.addNewRuleItem(this.currentSolrIndexId, this.searchInputTerm, tags);
    this.createItem.emit({itemType: ListItemType.RuleManagement, apiCall})
  }

  deleteItem(itemType: string, id: string) {
    // TODO maybe before even starting the deletion process, check if details are dirty and ask to cancel editing eventually
    // TODO reselect selected index, if deleted entry was the selected one
    // TODO reselect selected index, if deleted entry was the first one
    this.deleteItemByType.emit({itemType, id});
  }

  public refreshItemsInList(solrIndexId: string) {
    console.log('In SearchInputListComponent :: updateItemList')
    console.log(solrIndexId)

    return this.searchManagementService
      .getAllItemsForInputList(solrIndexId)
      .then(listItems => {
        this.listItems = listItems;
        this.searchInputTerm = '';
        this.setShowMoreButtonState();
        return listItems
      })
      .then(listItems => this.refreshTags(listItems))
      .catch(error => this.handleError(error));
  }

  private refreshTags(listItems: smm.ListItem[]) {
    const tags = new Map<string, smm.InputTag>();
    for (const i of listItems) {
      for (const t of i.tags) {
        tags.set(t.displayValue, t);
      }
    }
    this.allTags = Array.from(tags.values()).sort((a, b) => a.displayValue.localeCompare(b.displayValue));
    // Reset tagFilter if the tag is no longer available in the current search inputs
    if (this.tagFilter && !tags.get(this.tagFilter.displayValue)) {
      this.tagFilter = null;
    }
  }

  public selectListItemById(listItemId: string) {
    const listItem = this.listItems.find(item => item.id === listItemId);
    this.selectListItem(listItem)
  }

  public selectListItem(listItem: ListItem) {
    console.log(`In SearchInputListComponent :: selectListItem :: id = ${listItem ? JSON.stringify(listItem) : 'null'}`);

    this.executeWithChangeCheck.emit({
      executeFnOk: () => {
        this.selectedListItem = listItem;
        this.selectedListItemChange.emit(listItem);
      },
      executeFnCancel: () => ({})
    });
  }

  private setShowMoreButtonState() {
    this.showMoreButton =
      !this.isShowingAllItems && (
      this.limitItemsTo > 0 &&
      this.listItems.length > this.limitItemsTo &&
      this.searchInputTerm === '');
  }

  public toggleShowMore() {
    this.isShowingAllItems = !this.isShowingAllItems
    this.setShowMoreButtonState()
  }
}
