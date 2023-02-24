import {
  Component,
  Input,
  Output,
  EventEmitter
} from '@angular/core'

import {
  SearchInput,
} from '../../../models';

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

  warnForExactMatchingSyntax(): boolean {
    if(!this.exactMatchWarn) {
      return false
    } else {
      return (
        SearchInput.isTermExact(this.term)
        || SearchInput.isTermLeftExact(this.term)
        || SearchInput.isTermRightExact(this.term)
      )
    }
  }
}
