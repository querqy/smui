import { Component, Input, Output, OnChanges, EventEmitter } from '@angular/core';

import { SpellingsService } from '../../../services/index';
import { CommonsService } from '../../../helpers/index';
import { AlternativeSpelling, CanonicalSpelling } from '../../../models/index';

@Component({
  selector: 'smui-spellings',
  templateUrl: './spellings.component.html',
})
export class SpellingsComponent implements OnChanges {

  @Input() selectedListItem = null;
  @Input() currentSolrIndexId = '-1';

  @Output() refreshAndSelectListItemById: EventEmitter<string> = new EventEmitter();
  @Output() showErrorMsg: EventEmitter<string> = new EventEmitter();
  @Output() showSuccessMsg: EventEmitter<string> = new EventEmitter();
  @Output() openDeleteConfirmModal: EventEmitter<any> = new EventEmitter();

  private detailSpelling: CanonicalSpelling = null;
  private detailSpellingCleanState: string = null;
  private errors = [];

  constructor(
    private spellingsService: SpellingsService,
    private commonsService: CommonsService
  ) { }

  ngOnChanges() {
    if (this.selectedListItem) {
      this.showDetailsForSpelling(this.selectedListItem.id)
    }
  }

  public showDetailsForSpelling(spellingId: string): void {
    if (spellingId === null) {
      this.detailSpelling = null;
    } else {
      this.spellingsService
        .getDetailedSpelling(spellingId)
        .then(canonicalSpelling => {
          this.errors = [];
          this.detailSpelling = canonicalSpelling;
          this.detailSpellingCleanState = JSON.stringify(canonicalSpelling);
        })
        .catch(error => this.showErrorMsg.emit(error));
    }
  }

  public addNewAlternativeSpelling(): void {
    const emptyAlternativeSpelling: AlternativeSpelling = {
      id: this.commonsService.generateUUID(),
      canonicalSpellingId: this.detailSpelling.id,
      term: '',
      isActive: true
    };

    this.detailSpelling
      .alternativeSpellings.push(emptyAlternativeSpelling);
  }

  public deleteAlternativeSpelling(index: number): void {
    this.detailSpelling.alternativeSpellings.splice(index, 1);
  }

  public saveSpellingsDetails(): void {
    this.spellingsService
      .updateSpellingItem(this.currentSolrIndexId, this.detailSpelling)
      .then(spellingId => this.refreshAndSelectListItemById.emit(spellingId))
      .then(_ => this.showSuccessMsg.emit('Saving Details successful.'))
      .catch(error => {
        if (error.status === 400) {
          const msg = error.json().message;
          this.errors = msg.split('\n')
        } else {
          this.showErrorMsg.emit(error)
        }
      })
  }

  private deleteSpelling() {
    const deleteCallback = () =>
      this.spellingsService.deleteSpelling(this.detailSpelling.id)
        .then(() => this.refreshAndSelectListItemById.emit(null))
        .catch(error => this.showErrorMsg.emit(error))

    this.openDeleteConfirmModal.emit({itemType: 'spelling item', deleteCallback})
  }

  public isDirty(): boolean {
    return this.commonsService.isDirty(this.detailSpelling, this.detailSpellingCleanState)
  }

}
