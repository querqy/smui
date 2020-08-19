import { Component, Input, Output, EventEmitter } from '@angular/core';

declare var $: any // TODO include @types/jquery properly, make this workaround unnecessary

class Deferred<T> {
  promise: Promise<T>
  resolve: (value?: T | PromiseLike<T>) => void
  reject:  (reason?: any) => void

  constructor() {
    this.promise = new Promise<T>((resolve, reject) => {
      this.resolve = resolve
      this.reject  = reject
    })
  }
}

@Component({
  selector: 'smui-modal-dialog',
  templateUrl: './modal-dialog.component.html',
  styleUrls: ['./modal-dialog.component.css']
})
export class ModalDialogComponent {

  public confirmTitle = ''
  public confirmBodyText = ''
  public cancelText = ''
  public okText = ''
  public modalConfirmDeferred: Deferred<boolean>
  public errorMessageModalText = ''

  // bridge angular2-to-jquery for opening the bootstrap confirmModal and map to a Promise<boolean> (modalConfirmPromise)
  // TODO consider outsourcing modal confirmation implementation to component, service or directive ...

  public openModalConfirm(title, bodyText, okText, cancelText) {
    console.log('In AppComponent :: openModalConfirm');

    this.confirmTitle = title
    this.confirmBodyText = bodyText
    this.okText = okText
    this.cancelText = cancelText

    $('#confirmModal').modal('show')
    this.modalConfirmDeferred = new Deferred<boolean>()
    return this.modalConfirmDeferred
  }

  confirmModalCancel() {
    console.log('In AppComponent :: confirmModalCancel')
    this.modalConfirmDeferred.resolve(false)
  }

  confirmModalOk() {
    console.log('In AppComponent :: confirmModalOk')
    this.modalConfirmDeferred.resolve(true)
  }

  public showLongErrorMessage(errorMessage: string) {
    this.errorMessageModalText = errorMessage
    $('#errorMessageModal').modal('show')
  }

}
