import {
  Component,
  Input,
  Output,
  OnChanges,
  EventEmitter
} from '@angular/core';

import {CommonsService, SpellingsService} from '../../../services';
import {
  AlternativeSpelling,
  CanonicalSpelling,
  ListItem,
  LevenshteinDistance
} from '../../../models';

@Component({
  selector: 'app-smui-spellings',
  templateUrl: './spellings.component.html'
})
export class SpellingsComponent implements OnChanges {
  @Input() currentSolrIndexId?: string;
  @Input() selectedListItem?: ListItem;

  @Output() refreshAndSelectListItemById: EventEmitter<string> = new EventEmitter();
  @Output() showErrorMsg: EventEmitter<string> = new EventEmitter();
  @Output() showSuccessMsg: EventEmitter<string> = new EventEmitter();
  @Output() openDeleteConfirmModal: EventEmitter<any> = new EventEmitter();

  detailSpelling?: CanonicalSpelling;
  detailSpellingCleanState?: string;
  errors: string[] = [];

  constructor(
    private spellingsService: SpellingsService,
    private commonsService: CommonsService
  ) {
  }

  ngOnChanges() {
    if (this.selectedListItem) {
      this.showDetailsForSpelling(this.selectedListItem.id);
    }
  }

  showDetailsForSpelling(spellingId: string): void {
    if (spellingId === null) {
      this.detailSpelling = undefined;
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

  addNewAlternativeSpelling(): void {
    if (this.detailSpelling) {
      const emptyAlternativeSpelling: AlternativeSpelling = {
        id: this.commonsService.generateUUID(),
        canonicalSpellingId: this.detailSpelling.id,
        term: '',
        isActive: true
      };

      this.detailSpelling.alternativeSpellings.push(emptyAlternativeSpelling);
    }
  }

  deleteAlternativeSpelling(index: number): void {
    if (this.detailSpelling) {
      this.detailSpelling.alternativeSpellings.splice(index, 1);
    }
  }

  saveSpellingsDetails(): void {
    if (this.detailSpelling && this.currentSolrIndexId) {
      this.spellingsService
        .updateSpellingItem(this.currentSolrIndexId, this.detailSpelling)
        .then(apiResult =>
          this.refreshAndSelectListItemById.emit(apiResult.returnId)
        )
        .then(_ => this.showSuccessMsg.emit('Saving Details successful.'))
        .catch(error => {
          if (error.status === 400) {
            const msg = error.error.message;
            this.errors = msg.split('\n');
          } else {
            this.showErrorMsg.emit(error);
          }
        });
    }
  }

  deleteSpelling(): void {
    if (this.detailSpelling) {
      const {id} = this.detailSpelling;
      if (id) {
        const deleteCallback = () =>
          this.spellingsService
            .deleteSpelling(id)
            .then(() => this.refreshAndSelectListItemById.emit(undefined))
            .catch(error => this.showErrorMsg.emit(error));

        this.openDeleteConfirmModal.emit({deleteCallback});
      }
    }
  }

  public isDirty(): boolean {
    return this.detailSpelling && this.detailSpellingCleanState
      ? this.commonsService.isDirty(
        this.detailSpelling,
        this.detailSpellingCleanState
      )
      : false;
  }

  editDistance(canonicalTerm: string, alternativeTerm: string): number {
    return LevenshteinDistance.calc(
      canonicalTerm.trim().toLowerCase(),
      alternativeTerm.trim().toLowerCase()
    )
  }
}
