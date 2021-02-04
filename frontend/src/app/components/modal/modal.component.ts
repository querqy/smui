import {
  Component,
  Input,
  OnInit,
  OnDestroy,
  ElementRef,
  ViewChild
} from '@angular/core';
import { ModalService } from '../../services';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { NgbModalOptions } from '@ng-bootstrap/ng-bootstrap/modal/modal-config';

@Component({
  selector: 'app-smui-modal',
  templateUrl: './modal.component.html'
})
export class ModalComponent implements OnInit, OnDestroy {
  @ViewChild('content') content: ElementRef;
  @Input() id: string;
  @Input() title: string;

  modalReference: any;
  private element: any;

  constructor(
    private modalService: ModalService,
    private ngbModalService: NgbModal,
    private el: ElementRef
  ) {
    this.element = el.nativeElement;
  }

  ngOnInit(): void {
    if (!this.id) {
      console.error('The modal must have an id');
      return;
    }

    this.modalService.add(this);
  }

  ngOnDestroy(): void {
    this.modalService.remove(this.id);
    this.element?.remove();
  }

  open(options?: NgbModalOptions): void {
    this.modalReference = this.ngbModalService.open(this.content, options);
  }

  close(): void {
    this.modalReference?.close();
  }
}
