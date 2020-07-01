import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';

import { SearchManagementService } from './search-management.service';
import { FeatureToggleService } from './feature-toggle.service';
import { AlternativeSpelling, CanonicalSpelling } from './search-management.model';

@Component({
  selector: 'smui-spelling-detail',
  templateUrl: './spelling-detail.component.html',
  styleUrls: ['./spelling-detail.component.css'],
  providers: [FeatureToggleService]
})
export class SpellingDetailComponent implements OnInit {

  @Input() selectedListItem = null;
  @Input() currentSolrIndexId = '-1';

  @Output() showErrorMsg: EventEmitter<string> = new EventEmitter();
  @Output() showSuccessMsg: EventEmitter<string> = new EventEmitter();
  @Output() refreshItemsInList: EventEmitter<string> = new EventEmitter();
  @Output() deleteItemByType: EventEmitter<any> = new EventEmitter();

  private detailSpelling: CanonicalSpelling = null;
  private detailSpellingCleanState: string = null;
  private saveError: string = null;

  constructor(
    private searchManagementService: SearchManagementService) {
  }

  ngOnInit() {
    console.log('In SpellingDetailComponent :: ngOnInit');
  }

  ngOnChanges() {
    if (this.selectedListItem) {
      this.showDetailsForSpelling(this.selectedListItem.id)
    }
  }

  handleError(error: any) {
    console.log('In SpellingDetailComponent :: handleError');
    console.log(':: error = ' + error);
    this.showErrorMsg.emit('An error occurred.');
  }

  private randomUUID() {
    /* tslint:disable */
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
      const r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
      return v.toString(16);
    });
    /* tslint:enable */
  }

  public showDetailsForSpelling(spellingId: string) {
    console.log(`In SpellingDetailComponent :: showDetailsForSpelling :: id = ${spellingId}`);

    if (spellingId === null) {
      this.detailSpelling = null;
    } else {
      this.searchManagementService
        .getDetailedSpelling(spellingId)
        .then(canonicalSpelling => {
          this.saveError = null;
          this.detailSpelling = canonicalSpelling;
          this.detailSpellingCleanState = JSON.stringify(canonicalSpelling);
        })
        .catch(error => this.handleError(error));
    }
  }

  public addNewAlternativeSpelling() {
    console.log('In SpellingDetailComponent :: addNewAlternativeSpelling');

    const emptyAlternativeSpelling: AlternativeSpelling = {
      id: this.randomUUID(),
      canonicalSpellingId: this.detailSpelling.id,
      term: ''
    };

    this.detailSpelling
      .alternativeSpellings.push(emptyAlternativeSpelling);
  }

  public deleteAlternativeSpelling(index: number) {
    console.log(`In SpellingDetailComponent :: deleteAlternativeSpelling :: index = ${index}`);

    this.detailSpelling.alternativeSpellings.splice(index, 1);
  }

  public saveSpellingsDetails() {
    console.log('In SpellingDetailComponent :: saveSpelling');

    this.searchManagementService
      .updateSpellingItem(this.currentSolrIndexId, this.detailSpelling)
      .then(_ => this.showDetailsForSpelling(this.detailSpelling.id))
      .then(_ => this.refreshItemsInList.emit(this.currentSolrIndexId))
      .then(_ => {
        this.saveError = null;
        this.showSuccessMsg.emit('Saving Details successful.');
      })
      .catch(error => {
        if (error.status === 400) {
          this.saveError = error.json().message
        } else {
          this.handleError(error);
        }
      })
  }

  public deleteSpelling() {
    console.log(`In SpellingDetailComponent :: deleteSpelling :: id = ${this.detailSpelling.id}`);
    this.deleteItemByType.emit({itemType: 'Spelling', id: this.detailSpelling.id});
  }

  public isDirty(): boolean {
    console.log('In SearchInputDetailComponent :: isDirty');

    if (this.detailSpelling === null) {
      return false;
    } else {
      return JSON.stringify(this.detailSpelling) !== this.detailSpellingCleanState;
    }
  }

}
