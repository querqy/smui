import {Component, ElementRef, EventEmitter, Output, ViewChild} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {ToasterService} from "angular2-toaster";
import {
  SolrService,
  ModalService
} from '../../../services';
import {SolrIndex} from "../../../models";
import {RulesCollectionCreateComponent} from "../../admin";

@Component({
  selector: 'file-upload',
  templateUrl: "file-upload.component.html",
  styleUrls: ["file-upload.component.scss"]
})
export class FileUploadComponent {
  fileName = '';
  fileMessage = '';
  target: EventTarget | null;

  @ViewChild('fileUpload')
  myInputVariable: ElementRef;
  @Output() solrIndicesChange: EventEmitter<string> = new EventEmitter();
  @Output() showErrorMsg: EventEmitter<string> = new EventEmitter();
  @Output() refreshRulesCollectionList: EventEmitter<string> = new EventEmitter();

  constructor(private http: HttpClient,
              private toasterService: ToasterService,
              private solrService: SolrService) {
  }

  public showSuccessMsg(msgText: string) {
    this.toasterService.pop('success', '', msgText);
  }

  refreshSolrIndicies() {
    return this.solrService.listAllSolrIndices;
    //  : Promise.reject('No selected Solr index');
  }

  onFileSelected(event: Event) {
    const target = event.target as HTMLInputElement;
    if (target.files != null) {
      const file: File = target.files[0];
      if (file) {
        this.fileName = file.name;
        this.fileMessage = '... processing ...';
        const formData = new FormData();
        formData.append("uploadedFile", file);
        this.showSuccessMsg(this.fileName + this.fileMessage);
        const upload$ = this.http.post("/api/v1/upload-import", formData).toPromise()
          .then(() => {
            this.myInputVariable.nativeElement.value = '';
            this.refreshSolrIndicies();
            })
          .then(() => { this.solrService.listAllSolrIndices().then(() => this.solrService.emitRulesCollectionChangeEvent("")); })
          //.then(() => { this.showSuccessMsg.emit(this.fileName + " received and imported."); })
          .then(() => { this.fileMessage = '... Done Processing.'; })
      }

    }
  }
  //
  // reateRulesCollection( event: Event){
  //   console.log('In RulesCollectionCreateComponent :: createRulesCollection');
  //   if (true) {
  //     this.solrService
  //       .createSolrIndex(this.name, this.description)
  //       .then(() => this.solrService.listAllSolrIndices())
  //       .then(() => this.solrIndicesChange.emit())
  //       .then(() => this.showSuccessMsg.emit("Created new Rules Collection " + this.description))
  //       .then(() => this.solrService.emitRulesCollectionChangeEvent(""))
  //       .then(() => this.clearForm())
  //       .catch(error => {
  //         console.log(error);
  //         var errorMsg = 'Unknown Error'
  //         if ('message' in error.error) {
  //           errorMsg = error.error.message;
  //         }
  //         this.showErrorMsg.emit(errorMsg);
  //       });
  //   } else {
  //     this.showErrorMsg.emit("Fill in both name fields.");
  //   }
  // }
}
