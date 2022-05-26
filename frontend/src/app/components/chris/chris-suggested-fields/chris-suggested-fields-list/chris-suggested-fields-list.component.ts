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
  selector: 'app-smui-chris-suggested-fields-list',
  templateUrl: './chris-suggested-fields-list.component.html'
})
export class ChrisSuggestedFieldsListComponent implements OnInit, OnChanges {

  @Input() solrIndex: SolrIndex;
  @Input() chrisSuggestedFields: Array<SuggestedSolrField>;

  @Output() openDeleteConfirmModal: EventEmitter<any> = new EventEmitter();
  @Output() showErrorMsg: EventEmitter<string> = new EventEmitter();
  @Output() solrIndicesChange: EventEmitter<string> = new EventEmitter();
  @Output() chrisSuggestedFieldsChange: EventEmitter<string> = new EventEmitter();

  constructor(
    private solrService: SolrService,
  ) {

  }

  ngOnInit() {
    console.log('In ChrisSuggestedFieldsListComponent :: ngOnInit');

  }

  ngOnChanges(changes: SimpleChanges): void {
    console.log('In ChrisSuggestedFieldsListComponent :: ngOnChanges');
  }


  lookupChrisSuggestedFields() {
    console.log('In ChrisSuggestedFieldsListComponent :: lookupSuggestedFields');
    this.solrService.getSuggestedFields(this.solrIndex.id)
      .then(chrisSuggestedFields => {
        this.chrisSuggestedFields = chrisSuggestedFields;
      })
      .catch(error => this.showErrorMsg.emit(error));

  }

  deleteChrisSuggestedField(suggestedFieldId: string, event: Event) {
    event.stopPropagation();
    const deleteCallback = () =>
      this.solrService
        .deleteSuggestedField(this.solrIndex.id, suggestedFieldId)
        .then(() => this.lookupChrisSuggestedFields())

        .catch(error => this.showErrorMsg.emit(error));


    this.openDeleteConfirmModal.emit({ deleteCallback });
  }
}
