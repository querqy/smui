import { Component, OnInit, Input, ViewChild } from '@angular/core';

@Component({
  selector: 'app-smui-admin',
  templateUrl: './admin.component.html'
})
export class AdminComponent implements OnInit {

  ngOnInit() {
    console.log('In AdminComponent :: ngOnInit');  
  }

}
