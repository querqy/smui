// import {Component, ElementRef, EventEmitter, Output, ViewChild} from "@angular/core";
// import {HttpClient} from "@angular/common/http";
// import {ToasterService} from "angular2-toaster";
// import {
//   SolrService,
//   ModalService
// } from '../../../services';
// import {SolrIndex} from "../../../models";
// import {RulesCollectionCreateComponent} from "../../admin";
//
// @Component({
//   selector: 'filz-upload',
//   templateUrl: "filz-upload.component.html",
//   styleUrls: ["filz-upload.component.scss"]
// })
// export class FilzUploadComponent {
//   fileName = '';
//   fileMessage = '';
//   target: EventTarget | null;
//
//   @ViewChild('fileUpload')
//   myInputVariable: ElementRef;
//   @Output() solrIndicesChange: EventEmitter<string> = new EventEmitter();
//   @Output() showErrorMsg: EventEmitter<string> = new EventEmitter();
//   @Output() refreshRulesCollectionList: EventEmitter<string> = new EventEmitter();
//
//   constructor(private http: HttpClient,
//               private toasterService: ToasterService,
//               private solrService: SolrService) {
//   }
//
//   public showSuccessMsg(msgText: string) {
//     this.toasterService.pop('success', '', msgText);
//   }
//
//   refreshSolrIndicies() {
//     return this.solrService.listAllSolrIndices;
//   }
//
//   onFileSelected(event: Event) {
//     const target = event.target as HTMLInputElement;
//     if (target.files != null) {
//       const file: File = target.files[0];
//       if (file) {
//         this.fileName = file.name;
//         this.fileMessage = '... processing ...';
//         const formData = new FormData();
//         formData.append("uploadedFile", file);
//         this.showSuccessMsg(this.fileName + this.fileMessage);
//         const upload$ = this.http.post("/api/v1/upload-import", formData).toPromise()
//           .then(() => {
//             this.myInputVariable.nativeElement.value = '';
//             this.refreshSolrIndicies();
//             })
//           .then(() => { this.solrService.listAllSolrIndices().then(() => this.solrService.emitRulesCollectionChangeEvent("")); })
//           .then(() => { this.fileMessage = '... Done Processing.'; })
//           .then(() => { this.showSuccessMsg("Imported: " + this.fileName); })
//       }
//     }
//   }
//
// }
