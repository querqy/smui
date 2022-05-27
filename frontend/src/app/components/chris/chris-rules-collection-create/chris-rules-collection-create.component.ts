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
  valueName: string;
  resultString: string;

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
    this.valueName = '';
  }

  addOneValue( event: Event){
    console.log("addOneValue() , thingName: " + this.valueName);
    //console.log('In ChrisRulesCollectidonCreateComponent :: createChrisRulesCollection');
    this.solrService.putSomething2(this.valueName).then(() => console.log("done"));
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

  putRows(event: Event) {
    this.solrService.putSomething2("test row 1").then(() => console.log("added test row 1"));
    this.solrService.putSomething2("test row 2").then(() => console.log("added test row 2"));
    this.solrService.putSomething2("test row 3").then(() => console.log("added test row 3"));
    console.log("putRows()");
  }

  getRows(event: Event) {
    console.log("getRows()");
    //this.solrService.putSomething2("something3").then(() => console.log("done"));
    //this.solrService.putSomething2("something4").then(() => console.log("done"));
    this.solrService.getSomethings().then(
      result => {
        console.log(result)
      }
    )
  }

  download(event: Event) {
    console.log("download()");
    this.solrService.getSomethings().then(
      result => {
        var str = JSON.stringify(result);
        this.downloadStringAsFile(
          "file.json.txt",
          str);
      }
    );
  }

  downloadStringAsFile(filename: string, text: string) {
    var element = document.createElement('a');
    element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(text));
    element.setAttribute('download', filename);
    element.style.display = 'none';
    document.body.appendChild(element);
    element.click();
    document.body.removeChild(element);
  }

}
