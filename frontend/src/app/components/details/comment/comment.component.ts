import { Component, Input, Output, EventEmitter } from '@angular/core';

@Component({
  selector: 'smui-comment',
  templateUrl: './comment.component.html',
  styleUrls: ['./comment.component.css']
})
export class CommentComponent {
  @Input() comment: string = '';
  @Input() label: string = '';
  @Input() placeholder: string = 'Please enter...';
  @Input() disabled: boolean = false;

  @Output() commentChange = new EventEmitter();
  @Output() handleSave = new EventEmitter();
}
