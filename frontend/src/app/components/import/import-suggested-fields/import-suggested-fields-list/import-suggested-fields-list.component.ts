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
  SolrService
} from '../../../../services';

@Component({
  selector: 'app-smui-import-suggested-fields-list',
  templateUrl: './import-suggested-fields-list.component.html'
})
export class ImportSuggestedFieldsListComponent implements OnInit, OnChanges {

  @Input() solrIndex: SolrIndex;
  @Input() importSuggestedFields: Array<SuggestedSolrField>;

  @Output() openDeleteConfirmModal: EventEmitter<any> = new EventEmitter();
  @Output() showErrorMsg: EventEmitter<string> = new EventEmitter();
  @Output() solrIndicesChange: EventEmitter<string> = new EventEmitter();
  @Output() importSuggestedFieldsChange: EventEmitter<string> = new EventEmitter();

  constructor(
    private solrService: SolrService,
  ) {

  }

  ngOnInit() {
    console.log('In ImportSuggestedFieldsListComponent :: ngOnInit');

  }

  ngOnChanges(changes: SimpleChanges): void {
    console.log('In ImportSuggestedFieldsListComponent :: ngOnChanges');
  }


  lookupImportSuggestedFields() {
    console.log('In ImportSuggestedFieldsListComponent :: lookupSuggestedFields');
    this.solrService.getSuggestedFields(this.solrIndex.id)
      .then(importSuggestedFields => {
        this.importSuggestedFields = importSuggestedFields;
      })
      .catch(error => this.showErrorMsg.emit(error));

  }

  deleteImportSuggestedField(suggestedFieldId: string, event: Event) {
    event.stopPropagation();
    const deleteCallback = () =>
      this.solrService
        .deleteSuggestedField(this.solrIndex.id, suggestedFieldId)
        .then(() => this.lookupImportSuggestedFields())

        .catch(error => this.showErrorMsg.emit(error));


    this.openDeleteConfirmModal.emit({ deleteCallback });
  }
}
