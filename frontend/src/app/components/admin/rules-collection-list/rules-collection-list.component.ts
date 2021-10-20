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
  selector: 'app-smui-admin-rules-collection-list',
  templateUrl: './rules-collection-list.component.html'
})
export class RulesCollectionListComponent implements OnInit, OnChanges {

  @Input() solrIndices: Array<SolrIndex> = [];

  @Output() openDeleteConfirmModal: EventEmitter<any> = new EventEmitter();
  @Output() showErrorMsg: EventEmitter<string> = new EventEmitter();
  @Output() solrIndicesChange: EventEmitter<string> = new EventEmitter();

  constructor(
    private solrService: SolrService,
  ) {

  }
  ngOnInit() {
    console.log('In RulesCollectionListComponent :: ngOnInit');
  }

  ngOnChanges(changes: SimpleChanges): void {
    console.log('In RulesCollectionListComponent :: ngOnChanges');
  }

  deleteRulesCollection(id: string, event: Event) {
    event.stopPropagation();
    const deleteCallback = () =>
      this.solrService
        .deleteSolrIndex(id)
        .then(() => this.solrService.refreshSolrIndices())
        .then(() => this.solrIndicesChange.emit(id))
        .then(() => this.solrService.emitRulesCollectionChangeEvent(""))
        .catch(error => this.showErrorMsg.emit(error));


    this.openDeleteConfirmModal.emit({ deleteCallback });
  }
}
