import { Component, Input, Output, EventEmitter } from '@angular/core';

@Component({
  selector: 'app-smui-detail-header',
  templateUrl: './detail-header.component.html',
  styleUrls: ['./detail-header.component.css']
})
export class DetailHeaderComponent {
  @Input() title = '';
  @Input() exactMatchWarn = false;
  @Input() placeholder = 'Please enter...';
  @Input() term = '';
  @Input() disabled = false;
  @Input() active = true;

  @Output() termChange = new EventEmitter();
  @Output() activeChange = new EventEmitter();
  @Output() handleSave = new EventEmitter();
  @Output() handleDelete = new EventEmitter();

  warn_for_exactmatch(): boolean {
    if(!this.exactMatchWarn) {
      return false
    } else {
      const trimmedTerm = this.term.trim()
      return trimmedTerm.startsWith('"') && trimmedTerm.endsWith('"')
    }
  }
}
