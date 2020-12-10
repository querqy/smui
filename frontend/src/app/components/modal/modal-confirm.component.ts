import {
  Component,
  Input,
  OnInit,
  OnDestroy,
  ElementRef,
  ViewChild
} from '@angular/core'
import { ModalService } from '../../services'
import { NgbModal } from '@ng-bootstrap/ng-bootstrap'
import { NgbModalOptions } from '@ng-bootstrap/ng-bootstrap/modal/modal-config'

class Deferred<T> {
  promise: Promise<T>
  resolve: (value?: T | PromiseLike<T>) => void
  reject: (reason?: any) => void

  constructor() {
    this.promise = new Promise<T>((resolve, reject) => {
      this.resolve = resolve
      this.reject = reject
    })
  }
}

@Component({
  selector: 'smui-confirm-modal',
  templateUrl: './modal-confirm.component.html'
})
export class ModalConfirmComponent implements OnInit, OnDestroy {
  @Input() id: string
  @Input() title: string
  @Input() okLabel: string
  @Input() cancelLabel: string

  private element: any
  @ViewChild('content') content: ElementRef
  modalReference: any
  modalConfirmDeferred: Deferred<boolean>

  constructor(
    private modalService: ModalService,
    private ngbModalService: NgbModal,
    private el: ElementRef
  ) {
    this.element = el.nativeElement
  }

  ngOnInit(): void {
    if (!this.id) {
      console.error('The modal must have an id')
      return
    }

    this.modalService.add(this)
  }

  ngOnDestroy(): void {
    this.modalService.remove(this.id)
    this.element.remove()
  }

  open(options?: NgbModalOptions): Deferred<boolean> {
    this.modalConfirmDeferred = new Deferred<boolean>()
    this.modalReference = this.ngbModalService.open(this.content, options)
    return this.modalConfirmDeferred
  }

  close(): void {
    this.modalReference.close()
  }

  ok() {
    this.modalConfirmDeferred.resolve(true)
  }

  cancel() {
    this.modalConfirmDeferred.resolve(false)
    this.modalReference.close()
  }
}
