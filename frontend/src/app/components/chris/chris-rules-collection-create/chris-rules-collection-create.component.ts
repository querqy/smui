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
  selector: 'app-smui-chris-chris-rules-collection-create',
  templateUrl: './chris-rules-collection-create.component.html'
})
export class ChrisRulesCollectionCreateComponent implements OnInit, OnChanges {

  //@Output() updateRulesCollectionList: EventEmitter<> = new EventEmitter();
  @Output() showErrorMsg: EventEmitter<string> = new EventEmitter();
  @Output() showSuccessMsg: EventEmitter<string> = new EventEmitter();
  @Output() refreshChrisRulesCollectionList: EventEmitter<string> = new EventEmitter();
  @Output() solrIndicesChange: EventEmitter<string> = new EventEmitter();

  solrIndices: SolrIndex[];
  name: string;
  description: string;

  constructor(
    private solrService: SolrService,
  ) {

  }
  ngOnInit() {
    console.log('In ChrisRulesCollectionCreateComponent :: ngOnInit');
    //this.solrIndices = this.solrService.solrIndices;
  }

  ngOnChanges(changes: SimpleChanges): void {
    console.log('In ChrisRulesCollectionCreateComponent :: ngOnChanges');
  }

  refreshSolrIndicies() {
    return this.solrService.listAllSolrIndices;
    // : Promise.reject('No selected Solr index');
  }

  clearForm() {
    this.name = '';
    this.description = '';
  }

  createChrisRulesCollection( event: Event){
    console.log('In ChrisRulesCollectionCreateComponent :: createChrisRulesCollection');
    if (this.name && this.description) {
      this.solrService
        .createSolrIndex(this.name, this.description)
        .then(() => this.solrService.listAllSolrIndices())
        .then(() => this.solrIndicesChange.emit())
        .then(() => this.showSuccessMsg.emit("Created new Chris Rules Collection " + this.description))
        .then(() => this.solrService.emitRulesCollectionChangeEvent(""))
        .then(() => this.clearForm())
        .catch(error => this.showErrorMsg.emit(error));
    }
  }


}
