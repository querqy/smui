import { Component, OnInit } from '@angular/core'
import { ToasterConfig } from 'angular2-toaster'
import { Router, NavigationEnd } from '@angular/router'

import { HeaderNavComponent } from './header-nav/index'
import { SearchManagementComponent } from './search-management/index'

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

  isRouteReport = false

  constructor(
    private router: Router
  ) {
    console.log('In AppComponent :: constructor')

    // TODO very hacky and works around the actual router. use <router-outlet> instead!
    this.router.events.subscribe(e => {
      if (e instanceof NavigationEnd) {
        console.log(':: router.events.subscribe :: e.url = ' + e.url)
        this.isRouteReport = e.url === '/report'
      }
    })
  }

  ngOnInit() {
    console.log('In AppComponent :: ngOnInit')
  }

}
