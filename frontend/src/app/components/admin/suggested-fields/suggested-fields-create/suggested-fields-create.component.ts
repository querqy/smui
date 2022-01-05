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
  selector: 'app-smui-admin-suggested-fields-create',
  templateUrl: './suggested-fields-create.component.html'
})
export class SuggestedFieldsCreateComponent implements OnInit, OnChanges {

  @Input() solrIndex: SolrIndex;
  @Output() showErrorMsg: EventEmitter<string> = new EventEmitter();
  @Output() showSuccessMsg: EventEmitter<string> = new EventEmitter();
  @Output() suggestedFieldsChange: EventEmitter<string> = new EventEmitter();

  name: string;
  suggestedFields: SuggestedSolrField[] = [];

  constructor(
    private solrService: SolrService,
  ) {

  }
  ngOnInit() {
    console.log('In SuggestedFieldsCreateComponent :: ngOnInit');
  }

  ngOnChanges(changes: SimpleChanges): void {
    console.log('In SuggestedFieldsCreateComponent :: ngOnChanges');
  }


  clearForm() {
    this.name = '';
  }

  createSuggestedField( event: Event){
    console.log('In SuggestedFieldsCreateComponent :: createSuggestedField');
    if (this.name) {
      this.solrService
        .createSuggestedField(this.solrIndex.id, this.name)
        .then(() => this.showSuccessMsg.emit("Created new Suggested Field " + this.name))
        .then(() => this.suggestedFieldsChange.emit())
        .then(() => this.clearForm())
        .catch(error => this.showErrorMsg.emit(error));
    }
  }


}
