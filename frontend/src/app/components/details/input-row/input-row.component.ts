import { Component, Input, Output, EventEmitter } from '@angular/core';

@Component({
  selector: 'smui-input-row',
  templateUrl: './input-row.component.html',
  styleUrls: ['./input-row.component.css']
})
export class DetailInputRow {
  @Input() iconClass: string = '';
  @Input() label: string = '';
  @Input() placeholder: string = 'Please enter...';
  @Input() term: string = '';
  @Input() disabled: boolean = false;
  @Input() active: boolean = true;

  @Output() termChange = new EventEmitter();
  @Output() activeChange = new EventEmitter();
  @Output() handleSave = new EventEmitter();
  @Output() handleDeleteRow = new EventEmitter();
}
