import { Component, Input, Output, EventEmitter } from '@angular/core';

@Component({
  selector: 'smui-detail-header',
  templateUrl: './detail-header.component.html',
  styleUrls: ['./detail-header.component.css']
})
export class DetailHeaderComponent {
  @Input() title: string = '';
  @Input() placeholder: string = 'Please enter...';
  @Input() term: string = '';
  @Input() disabled: boolean = false;
  @Input() active: boolean = true;

  @Output() termChange = new EventEmitter();
  @Output() activeChange = new EventEmitter();
  @Output() handleSave = new EventEmitter();
  @Output() handleDelete = new EventEmitter();
}
