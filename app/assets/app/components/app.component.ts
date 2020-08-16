import { Component, OnInit } from '@angular/core';
import { ToasterConfig } from 'angular2-toaster'

import { HeaderNavComponent } from './header-nav/index';
import { SearchManagementComponent } from './search-management/index';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {

  public currentSolrIndexId: string = null

  public toasterConfig: ToasterConfig =
    new ToasterConfig({
      showCloseButton: false,
      tapToDismiss: true,
      timeout: 5000,
      positionClass: 'toast-bottom-right'
    })

  constructor(
  ) {
    console.log('In AppComponent :: constructor')
  }

  ngOnInit() {
    console.log('In AppComponent :: ngOnInit')
  }

}
