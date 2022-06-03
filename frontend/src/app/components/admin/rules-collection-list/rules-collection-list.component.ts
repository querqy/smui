import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnChanges,
  OnInit,
  SimpleChanges
} from '@angular/core';

import { SolrIndex, RulesReport } from '../../../models';

import { ToasterService } from 'angular2-toaster';


import { DownloadableRule } from '../../../models/downloadableRule.model';
import { DownloadableRules } from '../../../models/downloadableRules.model';

import {
  ReportService,
  SolrService,
  ModalService
} from '../../../services';
import {HttpClient} from "@angular/common/http";
import {Subject} from "rxjs";
import {Something} from "../../../models/export";


@Component({
  selector: 'app-smui-admin-rules-collection-list',
  templateUrl: './rules-collection-list.component.html'
})
export class RulesCollectionListComponent implements OnInit, OnChanges {

  @Output() openDeleteConfirmModal: EventEmitter<any> = new EventEmitter();
  @Output() showErrorMsg: EventEmitter<string> = new EventEmitter();
  @Output() showSuccessMsg: EventEmitter<string> = new EventEmitter();
  @Output() solrIndicesChange: EventEmitter<string> = new EventEmitter();
  rulesReport?: RulesReport;
  downloadableRules?: DownloadableRules;
  //savedOutput?: ListItem; //CJM
  private readonly baseUrl = 'api/v1';

  currentSolrIndexId = '-1';
  currentSolrIndexIdSubject: Subject<string> = new Subject<string>();

  constructor(
    private solrService: SolrService,
    private toasterService: ToasterService,
    private reportService: ReportService,
    private http: HttpClient
  ) {

    this.currentSolrIndexIdSubject.subscribe(
      value => (this.currentSolrIndexId = value)
    );

  }

  getSolrIndices() {
    return this.solrService.solrIndices
  }

  ngOnInit() {
    console.log('In RulesCollectionListComponent :: ngOnInit');
  }

  ngOnChanges(changes: SimpleChanges): void {
    console.log('In RulesCollectionListComponent :: ngOnChanges');
  }

  getSuggestedFields(solrIndex: SolrIndex) {
    var suggestedFields1 = null;
    console.log("solrIndex.id is: " + solrIndex.id);
    this.solrService.getSuggestedFields(solrIndex.id)
      .then(suggestedFields => {
        console.log('got here');
        suggestedFields1 = suggestedFields;
        console.log(suggestedFields1);
      })
      .catch(error => this.showErrorMsg.emit(error));
  }

  // getSuggestedFields2(solrIndex: SolrIndex) {
  //   //console.log("solrIndex.id is: " + solrIndex.id);
  //   this.solrService.getSolrIndex(solrIndex.id)
  //     .then(idx => {
  //       //console.log('got here 2');
  //       this.getRulesReport(solrIndex.id);
  //     })
  //     .catch(error => this.showErrorMsg.emit(error));
  // }

  // downloadRulesCollection(id: string, event: Event) {
  //   this.solrService.getSolrIndex(id)
  //     .then(solrIndex => this.getSuggestedFields2(solrIndex))
  //   this.toasterService.pop('success', '', 'Downloaded:' + id);
  // }

  // getSomething(solrIndexId: string): Promise<void> {
  //   return this.http
  //     //GET     /api/v1/solr-index/:solrIndexId                         controllers.ApiController.getSolrIndex(solrIndexId: String)
  //     .get<Something>(`${this.baseUrl}/solr-index/${solrIndexId}/something`)
  //     .toPromise()
  //     .then(returnValue => {
  //       //this.savedOutput = output;
  //       this.downloadFile(solrIndexId, JSON.stringify(returnValue)); //this.savedOutput));
  //       //this.downloadFile(solrIndexId + "json.txt", JSON.stringify(this.savedOutput));
  //     });
  // }

  // private getRulesReport(solrIndexId: string) {
  //   this.getSomething(solrIndexId);
  //   //this.solrService.listAllSolrIndices(solrIndexId).then();
  //   ///api/v1/:solrIndexId/rules-and-spellings
  //   // this.reportService
  //   //   .getRulesReport(solrIndexId)
  //   //   .then(retReport => {
  //   //     this.rulesReport = retReport;
  //   //     this.downloadableRules = new DownloadableRules();
  //   //     this.downloadableRules.id = solrIndexId;
  //   //     this.downloadableRules.rules = new Array<DownloadableRule>();
  //   //
  //   //     this.rulesReport.items.forEach(
  //   //        (element) => {
  //   //          var x = new DownloadableRule();
  //   //          x.inputId = element.inputId;
  //   //          x.inputTerm = element.inputTerm;
  //   //          x.details = element.details;
  //   //          x.term = element.term;
  //   //          x.isActive = element.isActive;
  //   //          x.inputModified = element.inputModified;
  //   //          x.inputTags = element.inputTags;
  //   //          x.modified = element.modified;
  //   //          this.downloadableRules?.rules?.push(x);
  //   //        }
  //   //     );
  //   //     this.downloadFile(solrIndexId + ".json.txt", JSON.stringify(this.downloadableRules));
  //   //
  //   //   })
  //   //   .catch(error => {
  //   //     // unpack and emit error message
  //   //     var errorMsg = 'Unknown error'
  //   //     if ('error' in error) {
  //   //       errorMsg = error.error.message;
  //   //     }
  //   //     this.showErrorMsg.emit(error)
  //   //   });
  // }

  // private downloadFile(filename: string, text: string) {
  //   var element = document.createElement('a');
  //   element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(text));
  //   element.setAttribute('download', filename);
  //   element.style.display = 'none';
  //   document.body.appendChild(element);
  //   element.click();
  //   document.body.removeChild(element);
  // }

  downloadRulesCollectionExport(id:String, event: Event) {
    console.log("download()");
    this.solrService.getExportWithId(id).then(
      result => {
        var str = JSON.stringify(result);
        this.downloadStringAsFile(
          "file.json.txt",
          str);
        this.showSuccessMsg.emit("Download: OK")
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

  deleteRulesCollection(id: string, event: Event) {
    event.stopPropagation();
    const deleteCallback = () =>
      this.solrService
        .deleteSolrIndex(id)
        .then(() => this.solrService.listAllSolrIndices())
        .then(() => this.solrIndicesChange.emit(id))
        .then(() => this.solrService.emitRulesCollectionChangeEvent(""))
        .then(() => this.showSuccessMsg.emit("Rule collection successfully deleted."))
        .catch(error => {
          // unpack and emit error message
          var errorMsg = 'Unknown error'
          if( 'error' in error ) {
            errorMsg = error.error.message
          }
          this.showErrorMsg.emit(errorMsg)
        });

    this.openDeleteConfirmModal.emit({ deleteCallback });
  }

}
