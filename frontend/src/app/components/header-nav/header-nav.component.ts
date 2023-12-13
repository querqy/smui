import { Component, OnInit } from '@angular/core';
import { ToasterService } from 'angular2-toaster';
import { Router } from '@angular/router';

import { DeploymentDetailedInfo, SmuiVersionInfo, SolrIndex } from '../../models';
import {
  FeatureToggleService,
  SolrService,
  ConfigService,
  ModalService,
  DeploymentDetailedInfoService
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
  deploymentLogInfo: DeploymentDetailedInfo[] = [];

  constructor(
    private toasterService: ToasterService,
    public featureToggleService: FeatureToggleService,
    private solrService: SolrService,
    private configService: ConfigService,
    public router: Router,
    public modalService: ModalService,
    public deploymentDetailedInfoService: DeploymentDetailedInfoService
  ) {
    this.solrService.currentSolrIndexIdSubject.subscribe(value => {
      this.currentSolrIndexId = value
      this.loadLatestDeploymentLogInfo()
    });
  }

  ngOnInit() {
    this.solrIndices = this.solrService.solrIndices;
    this.versionInfo = this.configService.versionInfo;
    this.currentSolrIndexId = this.solrService.currentSolrIndexId;
    this.solrService.rulesCollectionChangeEventListener().subscribe(info =>{
      console.log("HeaderNav: rulesCollectionChangeEventListener fired");
      this.solrIndices = this.solrService.solrIndices;
    });
    this.loadLatestDeploymentLogInfo()
  }

  loadLatestDeploymentLogInfo() {
    if(this.currentSolrIndexId !== undefined) {
      this.deploymentDetailedInfoService
        .get(this.currentSolrIndexId)
        .then(apiDeploymentInfo => {
          this.deploymentLogInfo = apiDeploymentInfo
        })
        // Ignore errors
    }
  }

  hideSolrIndexSelector() {
    return (!this.currentSolrIndexId) || (this.currentSolrIndexId === '-1') || (this.solrService.solrIndices.length < 1)
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
          this.modalService.close('confirm-publish-live')
          this.deploymentRunningForStage = undefined
          this.showSuccessMsg(apiResult.message)
          this.loadLatestDeploymentLogInfo()
        })
        .catch(error => {
          this.modalService.close('confirm-publish-live')
          this.deploymentRunningForStage = undefined
          this.showErrorMsg(error.error.message)
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

}
