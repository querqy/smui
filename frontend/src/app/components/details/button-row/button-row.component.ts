import { Component, Input, Output, EventEmitter } from '@angular/core';

@Component({
  selector: 'app-smui-button-row',
  templateUrl: './button-row.component.html',
  styleUrls: ['./button-row.component.css']
})
export class ButtonRowComponent {
  @Input() addLabel = '';
  @Input() saveLabel = '';
  @Input() disabled = false;

  @Output() handleAdd = new EventEmitter();
  @Output() handleSave = new EventEmitter();
}
