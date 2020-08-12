import { Component, OnInit, ViewChild, SimpleChanges } from '@angular/core';

import { InputTag, ListItem, SolrIndex, SuggestedSolrField } from '../../models/index';
import { FeatureToggleService, SolrService } from '../../services/index';

@Component({
  selector: 'smui-header-nav',
  templateUrl: './header-nav.component.html',
  styleUrls: ['./header-nav.component.css']
})
export class HeaderNavComponent implements OnInit {

  public deploymentRunningForStage = null;
  public hideDeploymentLogInfo = true;
  public deploymentLogInfo = 'Loading info ...';

  public listSolrIndeces: SolrIndex[];
  // TODO avoid to not separately keep currentSolrIndexId and according select-option model solrIndexSelectOptionModel
  public currentSolrIndexId: string = null;
  public solrIndexSelectOptionModel: string = null;
  private suggestedSolrFieldNames = null;

  constructor(
    public featureToggleService: FeatureToggleService,
    private solrService: SolrService
  ) {}

  ngOnInit() {
    this.loadSolrIndices();
  }

  loadSolrIndices() {
    this.solrService
      .listAllSolrIndeces()
      .then(solrIndices => {
        if (solrIndices.length > 0) {
          this.listSolrIndeces = solrIndices;
          this.currentSolrIndexId = this.listSolrIndeces[0].id;
          this.solrIndexSelectOptionModel = this.currentSolrIndexId;
          this.selectedListItem = null;

          this.solrService.listAllSuggestedSolrFields(this.currentSolrIndexId)
            .then(suggestedSolrFieldNames => {
              this.suggestedSolrFieldNames = suggestedSolrFieldNames
            })
            .catch(error => this.showErrorMsg(error));

          this.tagsService.listAllInputTags()
            .then(allTags => {
              this.allTags = allTags
            })
            .catch(error => this.showErrorMsg(error));
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

      _this.currentSolrIndexId = newSolrIndexId;
      _this.selectedListItem = null;
      _this.searchInputTerm = '';
      _this.appliedTagFilter = null;

      _this.solrService.listAllSuggestedSolrFields(_this.currentSolrIndexId)
        .then(suggestedSolrFieldNames => _this.suggestedSolrFieldNames = suggestedSolrFieldNames)
        .catch(error => _this.showErrorMsg(error));
    }

    function executeSelectSolrIndexCancel() {
      console.log('In AppComponent :: selectSolrIndex :: executeSelectSolrIndexCancel');
      // reset the select-option model to keep in sync with currentSolrIndexId
      _this.solrIndexSelectOptionModel = _this.currentSolrIndexId;
    }

    this.executeWithChangeCheck({
      executeFnOk: executeSelectSolrIndexOk,
      executeFnCancel: executeSelectSolrIndexCancel
    });
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
          this.showLongErrorMessage(error.json().message)
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

    this.openModalConfirm(
      'Confirm publish to LIVE',
      'Are you sure to publish current Search Rules to LIVE?',
      'Yes, publish to LIVE', 'No, cancel publish');
      this.modalConfirmDeferred.promise
      .then(isOk => {
        if (isOk) {
          this.requestPublishRulesTxtToSolr('LIVE');
        }
      });
  }

  public callSimpleLogoutUrl() {
    console.log('In AppComponent :: callSimpleLogoutUrl');

    // TODO redirect in a more "Angular-way" to target URL
    window.location.href = this.featureToggleService.getSimpleLogoutButtonTargetUrl();
  }

  public loadAndShowDeploymentLogInfo(targetPlatform: string) {
    console.log('In AppComponent :: loadAndShowDeploymentLog');

    this.hideDeploymentLogInfo = false;
    this.deploymentLogInfo = 'Loading info for ' + targetPlatform + ' ...';

    this.solrService
      .lastDeploymentLogInfo(this.currentSolrIndexId, targetPlatform)
      .then(retApiResult => {
        this.deploymentLogInfo = retApiResult.msg;
      })
      .catch(error => {
        this.showLongErrorMessage(error.json().message)
      });
  }

}
