import { Component, OnInit } from '@angular/core';
import { ToasterService } from 'angular2-toaster';
import {Router} from '@angular/router';

import { DeploymentLogInfo, SmuiVersionInfo, SolrIndex } from '../../models';
import {
  FeatureToggleService,
  SolrService,
  ConfigService,
  ModalService
} from '../../services';

@Component({
  selector: 'app-smui-header-nav',
  templateUrl: './header-nav.component.html',
  styleUrls: ['./header-nav.component.css']
})
export class HeaderNavComponent implements OnInit {
  solrIndices: SolrIndex[];
  currentSolrIndexId?: string;
  versionInfo?: SmuiVersionInfo;
  deploymentRunningForStage?: string;
  hideDeploymentLogInfo = true;
  deploymentLogInfo = 'Loading info ...';

  constructor(
    private toasterService: ToasterService,
    public featureToggleService: FeatureToggleService,
    private solrService: SolrService,
    private configService: ConfigService,
    public router: Router,
    public modalService: ModalService
  ) {
    this.solrService.currentSolrIndexIdSubject.subscribe(value => {
      this.currentSolrIndexId = value;
    });
  }

  ngOnInit() {
    this.solrIndices = this.solrService.solrIndices;
    this.versionInfo = this.configService.versionInfo;
    this.currentSolrIndexId = this.solrService.currentSolrIndexId;
  }

  // TODO showSuccess/ErrorMsg repetitive implementation
  showSuccessMsg(msgText: string) {
    this.toasterService.pop('success', '', msgText);
  }

  showErrorMsg(msgText: string) {
    this.toasterService.pop('error', '', msgText);
  }

  openHelpModal() {
    const options = { size: 'xl', centered: true, scrollable: true };
    this.modalService.open('help-modal', options);
  }

  changeSolrIndexId(event: any) {
    this.solrService.changeCurrentSolrIndexId(event.target.value);
  }

  requestPublishRulesTxtToSolr(targetPlatform: string) {
    if (this.currentSolrIndexId) {
      this.deploymentRunningForStage = targetPlatform;
      this.solrService
        .updateRulesTxtForSolrIndex(this.currentSolrIndexId, targetPlatform)
        .then(apiResult => {
          this.deploymentRunningForStage = undefined;
          this.showSuccessMsg(apiResult.message);
        })
        .catch(error => {
          this.deploymentRunningForStage = undefined;
          this.showErrorMsg(error.error.message);
        });
    } // TODO handle else-case, if no currentSolrIndexId selected
  }

  public publishToPreliveButtonText(): string {
    return this.deploymentRunningForStage === 'PRELIVE'
      ? 'Pushing to Solr...'
      : 'Push Config to Solr';
  }

  public publishToLiveButtonText(): string {
    return this.deploymentRunningForStage === 'LIVE'
      ? 'Publishing to LIVE...'
      : 'Publish to LIVE';
  }

  public publishSolrConfig() {
    console.log('In AppComponent :: publishSolrConfig');
    this.requestPublishRulesTxtToSolr('PRELIVE');
  }

  public callSimpleLogoutUrl() {
    console.log('In AppComponent :: callSimpleLogoutUrl');
    console.log("Logout target:" + this.featureToggleService.getSimpleLogoutButtonTargetUrl());
    var logoutTargetUrl = this.featureToggleService.getSimpleLogoutButtonTargetUrl();
    if (logoutTargetUrl.toLowerCase().startsWith("http")){
      // TODO redirect in a more "Angular-way" to target URL
      window.location.href = logoutTargetUrl;
    }
    else {
      window.location.pathname = logoutTargetUrl;
    }
  }

  public loadAndShowDeploymentLogInfo(targetPlatform: string) {
    console.log('In AppComponent :: loadAndShowDeploymentLog');

    if (this.currentSolrIndexId) {
      this.hideDeploymentLogInfo = false;
      this.deploymentLogInfo = 'Loading info for ' + targetPlatform + ' ...';

      this.solrService
        .lastDeploymentLogInfo(this.currentSolrIndexId, targetPlatform)
        .then(retApiResult => {
          this.deploymentLogInfo = retApiResult.msg;
        })
        .catch(error => this.showErrorMsg(error));
    }
  }
}
