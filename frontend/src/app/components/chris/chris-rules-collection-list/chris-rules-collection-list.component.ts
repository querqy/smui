import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnChanges,
  OnInit,
  SimpleChanges
} from '@angular/core';

import { SolrIndex } from '../../../models';
import {
  SolrService,
  ModalService
} from '../../../services';

@Component({
  selector: 'app-smui-chris-chris-rules-collection-list',
  templateUrl: './chris-rules-collection-list.component.html'
})
export class ChrisRulesCollectionListComponent implements OnInit, OnChanges {

  @Output() openDeleteConfirmModal: EventEmitter<any> = new EventEmitter();
  @Output() showErrorMsg: EventEmitter<string> = new EventEmitter();
  @Output() showSuccessMsg: EventEmitter<string> = new EventEmitter();
  @Output() solrIndicesChange: EventEmitter<string> = new EventEmitter();

  constructor(
    private solrService: SolrService,
  ) {
  }

  getSolrIndices() {
    return this.solrService.solrIndices
  }

  ngOnInit() {
    console.log('In ChrisRulesCollectionListComponent :: ngOnInit');
  }

  ngOnChanges(changes: SimpleChanges): void {
    console.log('In ChrisRulesCollectionListComponent :: ngOnChanges');
  }

  deleteChrisRulesCollection(id: string, event: Event) {
    event.stopPropagation();
    const deleteCallback = () =>
      this.solrService
        .deleteSolrIndex(id)
        .then(() => this.solrService.listAllSolrIndices())
        .then(() => this.solrIndicesChange.emit(id))
        .then(() => this.solrService.emitRulesCollectionChangeEvent(""))
        .then(() => this.showSuccessMsg.emit("Chris Rule collection successfully deleted."))
        .catch(error => {
          // unpack and emit error message
          var errorMsg = 'Unknown error'
          if( 'error' in error ) {
            errorMsg = error.error.message
          }
          this.showErrorMsg.emit(errorMsg)
        });

    this.openDeleteConfirmModal.emit({ deleteCallback });
  }
}
