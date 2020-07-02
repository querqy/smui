import {Component, Input, Output, EventEmitter, OnInit} from '@angular/core';

import * as smm from './search-management.model';
import { ListItem, ListItemType } from './search-management.model';
import { SearchManagementService } from './search-management.service';
import { FeatureToggleService } from './feature-toggle.service';

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
  public searchListItems: smm.ListItem[] = [];
  private listItems: smm.ListItem[] = [];
  private tagFilter: smm.InputTag = null;
  private searchInputTerm = '';
  private activateSpelling = this.featureToggleService.getSyncToggleActivateSpelling();
  private limitItemsTo = +this.featureToggleService.getSyncToggleUiListLimitItemsTo();
  private isShowingAllItems = this.limitItemsTo < 0;

  constructor(
    private searchManagementService: SearchManagementService,
    public featureToggleService: FeatureToggleService /*, TODO use or remove "route" DI
    private route: ActivatedRoute */) {
  }

  ngOnInit() {
    console.log('In SearchInputSearchComponent :: ngOnInit');
  }

  handleError(error: any) {
    console.log('In SearchInputSearchComponent :: handleError');
    console.log(':: error = ' + error);
    this.showErrorMsg.emit('An error occurred.'); // TODO Do a more detaillied error description
  }

  private listItemContainsString(item: smm.ListItem, searchTermLower: string): Boolean {
    function searchTermIncludesString(s: string) {
      return s.toLowerCase().indexOf(searchTermLower) !== -1
    }

    if (searchTermLower.length === 0) {
      return true;
    }
    if (searchTermIncludesString(item.term)) {
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

  public filterByTag(tag: smm.InputTag) {
    this.tagFilter = tag;
  }

  public getFilteredListItems(): smm.ListItem[] {
    const searchTerm = this.searchInputTerm.trim().toLowerCase();

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

    return this.searchManagementService
      .getAllItemsForInputList(solrIndexId)
      .then(listItems => {
        this.listItems = listItems;
        this.searchListItems = this.filterSearchListItems(listItems);
        this.searchInputTerm = '';
        return listItems
      })
      .then(listItems => this.refreshTags(listItems))
      .catch(error => this.handleError(error));
  }

  private filterSearchListItems(listItems: ListItem[]) {
    return listItems.filter(item =>
      item.itemType.toString() === ListItemType[ListItemType.Spelling].toString()
    );
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

    this.selectedListItem = listItem;
    this.selectedListItemChange.emit(listItem);
  }

  public selectListItemWithCheck(listItem: ListItem) {
    this.executeWithChangeCheck.emit({
      executeFnOk: () => this.selectListItem(listItem),
      executeFnCancel: () => ({})
    })
  }

  public toggleShowMore() {
    this.isShowingAllItems = !this.isShowingAllItems;
  }
}
