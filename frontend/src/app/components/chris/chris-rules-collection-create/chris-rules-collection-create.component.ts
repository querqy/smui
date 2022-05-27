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
  thingName: string;

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
    this.thingName = '';
  }

  doTheThing( event: Event){
    console.log("doTheThing() , thingName: " + this.thingName);
    //console.log('In ChrisRulesCollectidonCreateComponent :: createChrisRulesCollection');
    this.solrService.putSomething2(this.thingName).then(() => console.log("done"));
    //
    // if (this.name && this.description) {
    //   this.solrService
    //     .createSolrIndex(this.name, this.description)
    //     .then(() => this.solrService.listAllSolrIndices())
    //     .then(() => this.solrIndicesChange.emit())
    //     .then(() => this.showSuccessMsg.emit("Created new Chris Rules Collection " + this.description))
    //     .then(() => this.solrService.emitRulesCollectionChangeEvent(""))
    //     .then(() => this.clearForm())
    //     .catch(error => this.showErrorMsg.emit(error));
    // }
  }

  getSomethings(event: Event) {
    this.solrService.putSomething2("something3").then(() => console.log("done"));
    this.solrService.putSomething2("something4").then(() => console.log("done"));
    console.log("getSomethings()");
  }

}
