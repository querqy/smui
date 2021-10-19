import { Component, OnInit, Input, ViewChild } from '@angular/core';

import {
  ModalService
} from '../../services';

@Component({
  selector: 'app-smui-admin',
  templateUrl: './admin.component.html'
})
export class AdminComponent implements OnInit {

  constructor(
    private modalService: ModalService
  ) {

  }

  ngOnInit() {
    console.log('In AdminComponent :: ngOnInit');
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
