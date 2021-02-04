import { Injectable } from '@angular/core';
import { NgbModalOptions } from '@ng-bootstrap/ng-bootstrap/modal/modal-config';

@Injectable({
  providedIn: 'root'
})
export class ModalService {
  modals: any[] = [];

  constructor() {}

  add(modal: any) {
    this.modals.push(modal);
  }

  remove(id: string) {
    this.modals = this.modals.filter(x => x.id !== id);
  }

  open(id: string, options?: NgbModalOptions) {
    const modal: any = this.modals.find(x => x.id === id);
    return modal ? modal.open(options) : undefined;
  }

  close(id: string) {
    this.modals.filter(x => x.id === id).forEach(modal => modal.close());
  }
}
