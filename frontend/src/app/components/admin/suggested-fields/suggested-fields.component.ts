import { Component, OnInit, Input, Output,   EventEmitter } from '@angular/core';
import { ActivatedRoute, Params, ParamMap }   from '@angular/router';
//import 'rxjs/add/operator/switchMap';

import { switchMap } from 'rxjs/operators';

import { ToasterService } from 'angular2-toaster';

import { SolrIndex } from '../../../models';
import {
  SolrService,
  ModalService
} from '../../../services';


@Component({
  selector: 'app-smui-admin-suggested-fields',
  templateUrl: './suggested-fields.component.html'
})
export class SuggestedFieldsComponent implements OnInit {

  //@Input() solrIndex: SolrIndex;
  @Output() showErrorMsg: EventEmitter<string> = new EventEmitter();

  constructor(
    private route: ActivatedRoute,
    private solrService: SolrService,
    private toasterService: ToasterService,
  ) {

  }

  solrIndex: SolrIndex;


  ngOnInit() {
    console.log('In SuggestedFieldsComponent :: ngOnInit');


    this.route.paramMap.subscribe((params: ParamMap) => {
      console.log(params);
      console.log(params.get("solrIndexId")!.toLowerCase());
      //console.log(this.solrService.getSolrIndex(params.get("id")))
      this.solrService.getSolrIndex(params.get("solrIndexId")!.toLowerCase())
      .then(solrIndex =>
        this.solrIndex = solrIndex
      )
      .catch(error => this.showErrorMsg.emit(error));
    })

  }




}
