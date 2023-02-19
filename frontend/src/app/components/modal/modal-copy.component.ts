import {
  Component,
  Input,
  OnInit,
  OnDestroy,
  ElementRef,
  ViewChild
} from '@angular/core';
import { ModalService, SolrService } from '../../services';
import { SolrIndex } from '../../models';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { NgbModalOptions } from '@ng-bootstrap/ng-bootstrap/modal/modal-config';

class Deferred<T> {
  promise: Promise<T>;
  resolve: (value?: T | PromiseLike<T>) => void;
  reject: (reason?: any) => void;

  constructor() {
    this.promise = new Promise<T>((resolve, reject) => {
      this.resolve = resolve;
      this.reject = reject;
    });
  }
}

@Component({
  selector: 'app-smui-copy-modal',
  templateUrl: './modal-copy.component.html'
})
export class ModalCopyComponent implements OnInit, OnDestroy {
  @ViewChild('content') content: ElementRef;
  @Input() id: string;
  @Input() title: string;
  @Input() description: string;
  @Input() okLabel: string;
  @Input() cancelLabel: string;

  selectedSolrIndexId?: string;
  solrIndices: SolrIndex[];

  modalReference: any;
  modalConfirmInputDeferred: Deferred<any>;
  private element: any;

  constructor(
    public solrService: SolrService,
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

  selectedChanged(event: any) {
    //console.log('selectedChanged ' + this.selectedSolrIndexId);
  }

  open(options?: NgbModalOptions): Deferred<any> {
    this.selectedSolrIndexId = this.solrService.currentSolrIndexId;
    this.solrIndices = this.solrService.solrIndices;

    this.modalConfirmInputDeferred = new Deferred<any>();
    this.modalReference = this.ngbModalService.open(this.content, options);
    return this.modalConfirmInputDeferred;
  }

  close(): void {
    this.modalReference?.close();
  }

  ok() {
    this.modalConfirmInputDeferred?.resolve(this.selectedSolrIndexId);
  }

  cancel() {
    this.modalConfirmInputDeferred?.resolve(false);
    this.modalReference?.close();
  }
}
