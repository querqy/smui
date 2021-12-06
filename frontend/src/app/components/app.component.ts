import { Component, OnInit } from '@angular/core';
import { ToasterConfig } from 'angular2-toaster';
import { ConfigService, FeatureToggleService, SolrService } from '../services';

const toasterOptions = {
  showCloseButton: false,
  tapToDismiss: true,
  timeout: 5000,
  positionClass: 'toast-bottom-right'
};

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  toasterConfig: ToasterConfig = new ToasterConfig(toasterOptions);
  isInitialized = false;
  loginRequired = false;
  loginUrl = null;
  errors: string[] = [];

  constructor(
    private solrService: SolrService,
    private featureToggleService: FeatureToggleService,
    private configService: ConfigService
  ) {
    console.log('In AppComponent :: constructor');
  }

  ngOnInit() {
    console.log('In AppComponent :: ngOnInit');
    Promise.all([
      this.initFeatureToggles(),
      this.initSolarIndices(),
      this.initVersionInfo()
    ]).then(() => (this.isInitialized = this.errors.length === 0));
  }

  // I am not sure why, with User auth which properly responds with a 303 and a redirect that
  // when we get to here it's listed as a 200.
  // error.ok = false, error.name=HttpErrorResponse, error.url=http://localhost:4200/login_or_register.

  //function checkIfLoginRequired(error: any) {
//
  //}

  private initFeatureToggles(): Promise<void> {


    return this.featureToggleService.getFeatureToggles().catch(error => {
      console.log("Error:" + error);
      this.errors.push('Could not fetch app configuration from back-end');

    });
  }

  private initSolarIndices(): Promise<void> {
    return this.solrService.listAllSolrIndices().catch(() => {
      this.errors.push('Could not fetch Solr configuration from back-end');
    });
  }

  private initVersionInfo(): Promise<void> {
    return this.configService.getLatestVersionInfo().catch(() => {
      this.errors.push('Could not fetch version info from back-end');
    });
  }
}
