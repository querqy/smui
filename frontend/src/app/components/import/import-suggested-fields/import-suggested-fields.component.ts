import {Component, OnInit, Input, Output, EventEmitter, SimpleChanges} from '@angular/core';
import { ActivatedRoute, Params, ParamMap }   from '@angular/router';
//import 'rxjs/add/operator/switchMap';

import { switchMap } from 'rxjs/operators';

import { ToasterService } from 'angular2-toaster';

import {SolrIndex, SuggestedSolrField} from '../../../models';
import {
  SolrService,
  ModalService
} from '../../../services';


@Component({
  selector: 'app-smui-import-suggested-fields',
  templateUrl: './import-suggested-fields.component.html'
})
export class ImportSuggestedFieldsComponent implements OnInit {

  //@Input() solrIndex: SolrIndex;
  //@Output() showErrorMsg: EventEmitter<string> = new EventEmitter();
  importSuggestedFields: Array<SuggestedSolrField>;

  constructor(
    private route: ActivatedRoute,
    private modalService: ModalService,
    private solrService: SolrService,
    private toasterService: ToasterService,
  ) {

  }

  solrIndex: SolrIndex;


  ngOnInit() {
    console.log('In ImportSuggestedFieldsComponent :: ngOnInit');


    this.route.paramMap.subscribe((params: ParamMap) => {
      console.log(params);
      console.log(params.get("solrIndexId")!.toLowerCase());
      //console.log(this.solrService.getSolrIndex(params.get("id")))
      this.solrService.getSolrIndex(params.get("solrIndexId")!.toLowerCase())
      .then(solrIndex =>
        this.solrIndex = solrIndex
      )
          .then(() => this.lookupImportSuggestedFields())
      .catch(error => this.showErrorMsg(error));


    })

  }

  ngOnChanges(changes: SimpleChanges): void {
    console.log('In ImportSuggestedFieldsComponent :: ngOnChanges');
    this.lookupImportSuggestedFields();
  }


  lookupImportSuggestedFields() {
    console.log('In ImportSuggestedFieldsListComponent :: lookupImportSuggestedFields');
    console.log("Solr id?" + this.solrIndex.id)
    this.solrService.getSuggestedFields(this.solrIndex.id)
        .then(suggestedFields => {
          this.importSuggestedFields = suggestedFields;
        })
        .catch(error => this.showErrorMsg(error));

  }

  public showSuccessMsg(msgText: string) {
    this.toasterService.pop('success', '', msgText);
  }

  public showErrorMsg(msgText: string) {
    this.toasterService.pop('error', '', msgText);
  }

  public importSuggestedFieldsChange( id: string){
    console.log("ImportSuggestedFieldsComponent::importSuggestedFieldsChange")
    this.lookupImportSuggestedFields();

  }

  // @ts-ignore
  public openDeleteConfirmModal({ deleteCallback }) {
    const deferred = this.modalService.open('confirm-delete');
    deferred.promise.then((isOk: boolean) => {
      if (isOk) { deleteCallback(); }
      this.modalService.close('confirm-delete');
    });
  }



}
