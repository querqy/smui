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
  @Input() disabled = false;
  @Input() active = true;

  @Output() termChange = new EventEmitter();
  @Output() activeChange = new EventEmitter();
  @Output() handleSave = new EventEmitter();
  @Output() handleDeleteRow = new EventEmitter();
}
