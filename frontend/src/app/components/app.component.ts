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
      this.initVersionInfo(),
      this.initTargetEnvironment()
    ]).then(() => (this.isInitialized = this.errors.length === 0));
  }

  private initFeatureToggles(): Promise<void> {
    return this.featureToggleService.getFeatureToggles().catch(() => {
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

  private initTargetEnvironment(): Promise<void> {
    return this.configService.getTargetEnvironment().catch(() => {
      this.errors.push('Could not fetch target environment from back-end');
    })
  }

}
