import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { ToasterService } from 'angular2-toaster'

import { ModalDialogComponent } from '../modal-dialog/index'
import { InputTag, ListItem, SolrIndex, SuggestedSolrField } from '../../models/index';
import { FeatureToggleService, SolrService, ConfigService } from '../../services/index';

@Component({
  selector: 'smui-header-nav',
  templateUrl: './header-nav.component.html',
  styleUrls: ['./header-nav.component.css']
})
export class HeaderNavComponent implements OnInit {

  @Input() currentSolrIndexId: string = null
  @Output() currentSolrIndexIdChange = new EventEmitter<string>()
  @Input() mainComponentDirty = false
  @Input() smuiModalDialog: ModalDialogComponent = null

  public deploymentRunningForStage = null
  public hideDeploymentLogInfo = true
  public deploymentLogInfo = 'Loading info ...'

  public listSolrIndeces: SolrIndex[]
  // TODO avoid to not separately keep currentSolrIndexId and according select-option model solrIndexSelectOptionModel
  public solrIndexSelectOptionModel: string = null

  public smuiVersionInfo = null

  constructor(
    private toasterService: ToasterService,
    public featureToggleService: FeatureToggleService,
    private solrService: SolrService,
    private configService: ConfigService
  ) {
    console.log('In HeaderNavComponent :: constructor')
  }

  ngOnInit() {
    this.loadSolrIndices()
    this.configService
      .getLatestVersionInfo()
      .then(retVer => {
        console.log(':: configService :: getLatestVersionInfo :: retVer = ' + JSON.stringify(retVer))
        this.smuiVersionInfo = retVer
      })
      .catch(error => this.showErrorMsg(error))
  }

  // TODO showSuccess/ErrorMsg repetitive implementation
  public showSuccessMsg(msgText: string) {
    this.toasterService.pop('success', '', msgText);
  }

  public showErrorMsg(msgText: string) {
    this.toasterService.pop('error', '', msgText);
  }

  // TODO consider moving this as part of the init routine to app.component
  loadSolrIndices() {
    this.solrService
      .listAllSolrIndeces()
      .then(solrIndices => {
        if (solrIndices.length > 0) {
          this.listSolrIndeces = solrIndices
          this.solrIndexSelectOptionModel = this.listSolrIndeces[0].id
          this.currentSolrIndexIdChange.emit(this.listSolrIndeces[0].id)
        }
      })
      .catch(error => this.showErrorMsg(error))
  }

  public selectSolrIndex(newSolrIndexId: string) {
    console.log('In AppComponent :: selectSolrIndex :: newSolrIndexId = ' + JSON.stringify(newSolrIndexId));

    // ask for user acceptance eventually (modal confirmation) when changing solrIndex and reloading list, if detail's state is dirty
    const _this = this;
    function executeSelectSolrIndexOk() {
      console.log('In AppComponent :: selectSolrIndex :: executeSelectSolrIndexOk :: newSolrIndexId = ' + JSON.stringify(newSolrIndexId));
      console.log('_this.currentSolrIndexId = ' + JSON.stringify(_this.currentSolrIndexId));
      console.log('_this.solrIndexSelectOptionModel = ' + JSON.stringify(_this.solrIndexSelectOptionModel));

      _this.currentSolrIndexIdChange.emit(newSolrIndexId)
    }

    function executeSelectSolrIndexCancel() {
      console.log('In AppComponent :: selectSolrIndex :: executeSelectSolrIndexCancel');
      // reset the select-option model to keep in sync with currentSolrIndexId
      _this.solrIndexSelectOptionModel = _this.currentSolrIndexId;
    }

    if (this.mainComponentDirty) {
      // TODO merge implementation with search-management.component :: executeWithChangeCheck
      const modalConfirmDeferred = this.smuiModalDialog.openModalConfirm(
        'Confirm to discard unsaved input',
        'You have unsaved input! Do you really want to Cancel Editing of Search Input or Continue with it?',
        'Yes, Cancel Editing', 'No, Continue Editing');

      modalConfirmDeferred.promise
        .then(isOk => isOk ? executeSelectSolrIndexOk() : executeSelectSolrIndexCancel());
    } else {
      executeSelectSolrIndexOk()
    }
  }

  private requestPublishRulesTxtToSolr(targetPlatform: string) {

    if (this.currentSolrIndexId !== null) {
      this.deploymentRunningForStage = targetPlatform;
      this.solrService
        .updateRulesTxtForSolrIndex(this.currentSolrIndexId, targetPlatform)
        .then(retApiResult => {
          this.deploymentRunningForStage = null;
          this.showSuccessMsg( retApiResult.message );
        })
        .catch(error => {
          this.deploymentRunningForStage = null;
          this.smuiModalDialog.showLongErrorMessage(error.json().message)
        });
    } // TODO handle else-case, if no currentSolrIndexId selected
  }

  public publishToPreliveButtonText(): string {
    if (this.deploymentRunningForStage === 'PRELIVE') {
      return 'Pushing to Solr...';
    } else {
      return 'Push Config to Solr';
    }
  }

  public publishToLiveButtonText(): string {
    if (this.deploymentRunningForStage === 'LIVE') {
      return 'Publishing to LIVE...';
    } else {
      return 'Publish to LIVE';
    }
  }

  public publishSolrConfig() {
    console.log('In AppComponent :: publishSolrConfig');
    this.requestPublishRulesTxtToSolr('PRELIVE');
  }

  public publishToLIVE() {
    console.log('In AppComponent :: publishToLIVE');

    const modalConfirmDeferred = this.smuiModalDialog.openModalConfirm(
      'Confirm publish to LIVE',
      'Are you sure to publish current Search Rules to LIVE?',
      'Yes, publish to LIVE', 'No, cancel publish');

    modalConfirmDeferred.promise
      .then(isOk => isOk ? this.requestPublishRulesTxtToSolr('LIVE') : () => ({}))
  }

  public callSimpleLogoutUrl() {
    console.log('In AppComponent :: callSimpleLogoutUrl');

    // TODO redirect in a more "Angular-way" to target URL
    window.location.href = this.featureToggleService.getSimpleLogoutButtonTargetUrl();
  }

  public loadAndShowDeploymentLogInfo(targetPlatform: string) {
    console.log('In AppComponent :: loadAndShowDeploymentLog')

    this.hideDeploymentLogInfo = false
    this.deploymentLogInfo = 'Loading info for ' + targetPlatform + ' ...'

    this.solrService
      .lastDeploymentLogInfo(this.currentSolrIndexId, targetPlatform)
      .then(retApiResult => {
        this.deploymentLogInfo = retApiResult.msg
      })
      .catch(error => this.showErrorMsg(error))
  }

}
