import { Component, Input, Output, EventEmitter } from '@angular/core';

@Component({
  selector: 'app-smui-comment',
  templateUrl: './comment.component.html',
  styleUrls: ['./comment.component.css']
})
export class CommentComponent {
  @Input() comment = '';
  @Input() label = '';
  @Input() placeholder = 'Please enter...';
  @Input() disabled = false;

  @Output() commentChange = new EventEmitter();
  @Output() handleSave = new EventEmitter();
}
