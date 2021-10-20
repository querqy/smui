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
  selector: 'app-smui-admin-rules-collection-create',
  templateUrl: './rules-collection-create.component.html'
})
export class RulesCollectionCreateComponent implements OnInit, OnChanges {

  //@Output() updateRulesCollectionList: EventEmitter<> = new EventEmitter();
  @Output() showErrorMsg: EventEmitter<string> = new EventEmitter();
  @Output() showSuccessMsg: EventEmitter<string> = new EventEmitter();
  @Output() refreshRulesCollectionList: EventEmitter<string> = new EventEmitter();

  solrIndices: SolrIndex[];
  name: string;
  description: string;

  constructor(
    private solrService: SolrService,
  ) {

  }
  ngOnInit() {
    console.log('In RulesCollectionCreateComponent :: ngOnInit');
    this.solrIndices = this.solrService.solrIndices;
  }

  ngOnChanges(changes: SimpleChanges): void {
    console.log('In RulesCollectionCreateComponent :: ngOnChanges');
    //if (this.commonService.hasChanged(changes, 'currentSolrIndexId')) {
      //this.refreshItemsInList().catch(error => this.showErrorMsg.emit(error));
    //}
  }

  refreshSolrIndicies() {
    return this.solrService.listAllSolrIndices;
    //  : Promise.reject('No selected Solr index');
  }

  createRulesCollection( event: Event){
    console.log('In RulesCollectionCreateComponent :: createRulesCollection');
    if (this.name && this.description) {
      this.solrService
        .createSolrIndex(this.name, this.description)
        .then(() => this.showSuccessMsg.emit("Created new Rules Collection " + this.description))
        .then(spellingId =>
          this.refreshRulesCollectionList.emit(this.name)
        )
        .catch(error => this.showErrorMsg.emit(error));
    }
  }


}
