import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-smui-error',
  templateUrl: './error.component.html',
  styleUrls: ['./error.component.css']
})
export class ErrorComponent {
  @Input() title = '';
  @Input() errors: string[] = [];
}
