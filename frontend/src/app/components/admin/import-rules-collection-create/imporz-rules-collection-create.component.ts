// import {
//   Component,
//   Input,
//   Output,
//   EventEmitter,
//   OnChanges,
//   OnInit,
//   SimpleChanges
// } from '@angular/core';
//
// import { SolrIndex } from '../../../models';
// import {
//   SolrService,
//   ModalService
// } from '../../../services';
//
// import {FilzUploadComponent} from "./filz-upload.component";
//
// @Component({
//   selector: 'app-smui-admin-import-rules-collection-create',
//   templateUrl: './imporz-rules-collection-create.component.html'
// })
// export class ImporzRulesCollectionCreateComponent implements OnInit, OnChanges {
//
//   //@Output() updateRulesCollectionList: EventEmitter<> = new EventEmitter();
//   @Output() showErrorMsg: EventEmitter<string> = new EventEmitter();
//   @Output() showSuccessMsg: EventEmitter<string> = new EventEmitter();
//   @Output() refreshImportRulesCollectionList: EventEmitter<string> = new EventEmitter();
//   @Output() solrIndicesChange: EventEmitter<string> = new EventEmitter();
//   fuc: FilzUploadComponent;
//
//   solrIndices: SolrIndex[];
//   valueName: string;
//
//   constructor(
//     private solrService: SolrService,
//   ) {
//
//   }
//   ngOnInit() {
//     console.log('In ImportRulesCollectionCreateComponent :: ngOnInit');
//     this.solrIndices = this.solrService.solrIndices;
//   }
//
//   ngOnChanges(changes: SimpleChanges): void {
//     console.log('In ImportRulesCollectionCreateComponent :: ngOnChanges');
//   }
//
//   refreshSolrIndicies() {
//     return this.solrService.listAllSolrIndices;
//     // : Promise.reject('No selected Solr index');
//   }
//
//   clearForm() {
//     this.valueName = '';
//   }
//
//   addOneValue( event: Event){
//     if (this.valueName) {
//       console.log("addOneValue() , thingName: " + this.valueName);
//       //console.log('In ImportRulesCollectidonCreateComponent :: createImportRulesCollection');
//       this.solrService.putSomething2(this.valueName)
//         .then(() => this.showSuccessMsg.emit("Add Value: OK"));
//       //
//       // if (this.name && this.description) {
//       //   this.solrService
//       //     .createSolrIndex(this.name, this.description)
//       //     .then(() => this.solrService.listAllSolrIndices())
//       //     .then(() => this.solrIndicesChange.emit())
//       //     .then(() => this.showSuccessMsg.emit("Created new Import Rules Collection " + this.description))
//       //     .then(() => this.solrService.emitRulesCollectionChangeEvent(""))
//       //     .then(() => this.clearForm())
//       //     .catch(error => this.showErrorMsg.emit(error));
//       // }
//     } else {
//       this.showErrorMsg.emit("Add Value: oops");
//     }
//   }
//
//   putRows(event: Event) {
//     this.solrService.putSomething2("test row 1").then(() => console.log("added test row 1"));
//     this.solrService.putSomething2("test row 2").then(() => console.log("added test row 2"));
//     this.solrService.putSomething2("test row 3").then(() => console.log("added test row 3"));
//     this.showSuccessMsg.emit("Put Rows: OK")
//   }
//
//   getRows(event: Event) {
//     console.log("getRows()");
//     //this.solrService.putSomething2("something3").then(() => console.log("done"));
//     //this.solrService.putSomething2("something4").then(() => console.log("done"));
//     this.solrService.getExport().then(
//       result => {
//         console.log(result)
//         this.showSuccessMsg.emit("Get Rows: OK, see console log")
//       }
//     )
//   }
//
//   download(event: Event) {
//     console.log("download()");
//     this.solrService.getExport().then(
//       result => {
//         var str = JSON.stringify(result);
//         this.downloadStringAsFile(
//           "file.json.txt",
//           str);
//         this.showSuccessMsg.emit("Download: OK")
//       }
//     );
//   }
//
//   downloadStringAsFile(filename: string, text: string) {
//     var element = document.createElement('a');
//     element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(text));
//     element.setAttribute('download', filename);
//     element.style.display = 'none';
//     document.body.appendChild(element);
//     element.click();
//     document.body.removeChild(element);
//   }
//
//   onFileSelected(event: Event) {
//     console.log("event:" + event.target);
//   }
//
//   putty(event: Event) {
//     this.solrService.putty().then(() => console.log("did putty"));
//     this.showSuccessMsg.emit("Putty: OK")
//   }
// }
