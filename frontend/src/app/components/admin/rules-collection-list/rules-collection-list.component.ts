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
  collectionName: String;

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

  downloadRulesCollectionExport(id:String, event: Event) {
    console.log("download()");
    this.solrService.getSolrIndex(id.toString()).then(solrIndex => this.collectionName = solrIndex.name);
    this.solrService.getExportWithId(id).then(
      result => {
        var str = JSON.stringify(result);
        this.downloadStringAsFile(
          this.collectionName + ".json.txt",
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
