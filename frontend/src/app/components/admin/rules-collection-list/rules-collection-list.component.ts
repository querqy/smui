import { Component, OnInit, Input, ViewChild } from '@angular/core';

@Component({
  selector: 'app-smui-admin-rules-collection-list',
  templateUrl: './rules-collection-list.component.html'
})
export class RulesCollectionListComponent implements OnInit {

  ngOnInit() {
    console.log('In RulesCollectionListComponent :: ngOnInit');
  }

}
