import { Component, Input, Output, EventEmitter } from '@angular/core';

@Component({
  selector: 'app-smui-input-row',
  templateUrl: './input-row.component.html',
  styleUrls: ['./input-row.component.css']
})
export class InputRowComponent {
  @Input() iconClass = '';
  @Input() label = '';
  @Input() placeholder = 'Please enter...';
  @Input() term = '';
  @Input() editDistance = Number.NaN;
  @Input() disabled = false;
  @Input() active = true;

  @Output() termChange = new EventEmitter();
  @Output() activeChange = new EventEmitter();
  @Output() handleSave = new EventEmitter();
  @Output() handleDeleteRow = new EventEmitter();

  // TODO consider to refactor this into a style / progress width map

  editDistanceToBootstrapWarnClass(): string {
    if(this.editDistance <= 3) {
      return "bg-success"
    } else if(this.editDistance <= 6) {
      return "bg-warning"
    } else {
      return "bg-danger"
    }
  }

  editDistanceToProgressValue(): number {
    if(this.editDistance <= 3) {
      return Math.floor(this.editDistance * (50.0/3.0))
    } else if(this.editDistance <= 6) {
      return Math.floor(60.0 + ((this.editDistance-3.0) * (35.0/3.0)))
    } else {
      return 100
    }
  }

}
