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
  selector: 'app-smui-chris-suggested-fields-create',
  templateUrl: './chris-suggested-fields-create.component.html'
})
export class ChrisSuggestedFieldsCreateComponent implements OnInit, OnChanges {

  @Input() solrIndex: SolrIndex;
  @Output() showErrorMsg: EventEmitter<string> = new EventEmitter();
  @Output() showSuccessMsg: EventEmitter<string> = new EventEmitter();
  @Output() chrisSuggestedFieldsChange: EventEmitter<string> = new EventEmitter();

  name: string;
  suggestedFields: SuggestedSolrField[] = [];

  constructor(
    private solrService: SolrService,
  ) {

  }
  ngOnInit() {
    console.log('In ChrisSuggestedFieldsCreateComponent :: ngOnInit');
  }

  ngOnChanges(changes: SimpleChanges): void {
    console.log('In ChrisSuggestedFieldsCreateComponent :: ngOnChanges');
  }


  clearForm() {
    this.name = '';
  }

  createChrisSuggestedField( event: Event){
    console.log('In ChrisSuggestedFieldsCreateComponent :: createSuggestedField');
    if (this.name) {
      this.solrService
        .createSuggestedField(this.solrIndex.id, this.name)
        .then(() => this.showSuccessMsg.emit("Created new Chris Suggested Field " + this.name))
        .then(() => this.chrisSuggestedFieldsChange.emit())
        .then(() => this.clearForm())
        .catch(error => this.showErrorMsg.emit(error));
    }
  }


}
