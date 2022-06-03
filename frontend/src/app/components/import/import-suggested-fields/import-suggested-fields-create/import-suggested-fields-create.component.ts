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
  selector: 'app-smui-import-suggested-fields-create',
  templateUrl: './import-suggested-fields-create.component.html'
})
export class ImportSuggestedFieldsCreateComponent implements OnInit, OnChanges {

  @Input() solrIndex: SolrIndex;
  @Output() showErrorMsg: EventEmitter<string> = new EventEmitter();
  @Output() showSuccessMsg: EventEmitter<string> = new EventEmitter();
  @Output() importSuggestedFieldsChange: EventEmitter<string> = new EventEmitter();

  name: string;
  suggestedFields: SuggestedSolrField[] = [];

  constructor(
    private solrService: SolrService,
  ) {

  }
  ngOnInit() {
    console.log('In ImportSuggestedFieldsCreateComponent :: ngOnInit');
  }

  ngOnChanges(changes: SimpleChanges): void {
    console.log('In ImportSuggestedFieldsCreateComponent :: ngOnChanges');
  }


  clearForm() {
    this.name = '';
  }

  createImportSuggestedField( event: Event){
    console.log('In ImportSuggestedFieldsCreateComponent :: createSuggestedField');
    if (this.name) {
      this.solrService
        .createSuggestedField(this.solrIndex.id, this.name)
        .then(() => this.showSuccessMsg.emit("Created new Import Suggested Field " + this.name))
        .then(() => this.importSuggestedFieldsChange.emit())
        .then(() => this.clearForm())
        .catch(error => this.showErrorMsg.emit(error));
    }
  }


}
