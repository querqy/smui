import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnChanges,
  OnInit,
  SimpleChanges
} from '@angular/core';

import { SolrIndex, SuggestedSolrField } from '../../../../models';
import {
  SolrService,
  ModalService
} from '../../../../services';

@Component({
  selector: 'app-smui-admin-suggested-fields-list',
  templateUrl: './suggested-fields-list.component.html'
})
export class SuggestedFieldsListComponent implements OnInit, OnChanges {

  @Input() solrIndex: SolrIndex;

  @Output() openDeleteConfirmModal: EventEmitter<any> = new EventEmitter();
  @Output() showErrorMsg: EventEmitter<string> = new EventEmitter();
  @Output() solrIndicesChange: EventEmitter<string> = new EventEmitter();
  @Output() suggestedFieldsChange: EventEmitter<string> = new EventEmitter();

  constructor(
    private solrService: SolrService,
  ) {

  }

  suggestedFields: Array<SuggestedSolrField>;

  ngOnInit() {
    console.log('In SuggestedFieldsListComponent :: ngOnInit');
    //console.log("Solr id?" + this.solrIndex.id)
  }

  ngOnChanges(changes: SimpleChanges): void {
    console.log('In SuggestedFieldsListComponent :: ngOnChanges');
    console.log("Solr id?" + this.solrIndex.id)
    if (this.solrIndex) {
      this.lookupSuggestedFields();
    }
  }


  lookupSuggestedFields() {
    console.log('In SuggestedFieldsListComponent :: lookupSuggestedFields');
    console.log("Solr id?" + this.solrIndex.id)
    this.solrService.getSuggestedFields(this.solrIndex.id)
      .then(suggestedFields => {
        this.suggestedFields = suggestedFields;
      })
      .catch(error => this.showErrorMsg.emit(error));
    //  .listAllSuggestedSolrFields(this.solrIndex.id)
    //  .then(suggestedSolrFieldNames => {
    //    this.suggestedFieldNames = suggestedSolrFieldNames;
    //  })
    //  .catch(error => this.showErrorMsg(error));
  }

  deleteSuggestedField(id: string, event: Event) {
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
