import { Component, OnInit, Input } from '@angular/core';

import { ToasterService } from 'angular2-toaster';

import { SolrIndex } from '../../models';

import {
   SolrService,
   ModalService
 } from '../../services';

@Component({
  selector: 'app-smui-import',
  templateUrl: './import.component.html'
})
export class ImportComponent implements OnInit {

  constructor(
    private modalService: ModalService,
    private toasterService: ToasterService,
    private solrService: SolrService
  ) {

  }

  solrIndices: SolrIndex[];

  ngOnInit() {
    console.log('In ImportComponent :: ngOnInit');
    this.solrIndices = this.solrService.solrIndices;
  }

  public showSuccessMsg(msgText: string) {
    this.toasterService.pop('success', '', msgText);
  }

  public showErrorMsg(msgText: string) {
    this.toasterService.pop('error', '', msgText);
  }

  // @ts-ignore
  public openDeleteConfirmModal({ deleteCallback }) {
    const deferred = this.modalService.open('confirm-delete');
    deferred.promise.then((isOk: boolean) => {
      if (isOk) { deleteCallback(); }
      this.modalService.close('confirm-delete');
    });
  }

  public solrIndicesChange(id: string){
    console.log("ImportComponent :: solrIndicesChange :: id = " + id)
    this.solrIndices = this.solrService.solrIndices;
  }

}
