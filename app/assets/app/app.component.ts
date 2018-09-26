import { Component, OnInit, ViewChild } from '@angular/core';

import { SearchInputListComponent } from './search-input-list.component';
import { SearchInputDetailComponent } from './search-input-detail.component';

import { ToasterService, ToasterConfig } from 'angular2-toaster';

import * as smm from './search-management.model';
import { SearchManagementService } from './search-management.service';
import { FeatureToggleService } from './feature-toggle.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
  providers: [FeatureToggleService]
})
export class AppComponent implements OnInit {

  // TODO consider using an more abstract component-communication model (e.g. message-service, events, etc.)
  @ViewChild('searchInputListComponent') searchInputListComponent: SearchInputListComponent;
  @ViewChild('searchInputDetailComponent') searchInputDetailComponent: SearchInputDetailComponent;

  get self(): AppComponent {
    return this;
  }

  public listSolrIndeces: smm.SolrIndex[];
  // TODO avoid to not separately keep currentSolrIndexId and according select-option model solrIndexSelectOptionModel
  public currentSolrIndexId: number = null;
  public solrIndexSelectOptionModel: number = null;

  public toasterConfig: ToasterConfig =
    new ToasterConfig({
      showCloseButton: false,
      tapToDismiss: true,
      timeout: 5000,
      positionClass: 'toast-bottom-right'
  });

  constructor(
    private searchManagementService: SearchManagementService,
    public featureToggleService: FeatureToggleService,
    private toasterService: ToasterService) {
  }

  ngOnInit() {
    console.log('In AppComponent :: ngOnInit');

    this.searchManagementService
      .listAllSolrIndeces()
      .then(retListSolrIndeces => {
        console.log('ngOnInit :: then :: retListSolrIndeces = ' + JSON.stringify(retListSolrIndeces));
        this.listSolrIndeces = retListSolrIndeces;
        // TODO ensure, that minimum 1 list item exists
        this.currentSolrIndexId = this.listSolrIndeces[0].id;
        this.solrIndexSelectOptionModel = this.currentSolrIndexId;
        this.searchInputListComponent
          .loadSearchInputListForSolrIndexWithId(this.currentSolrIndexId)
        this.searchInputDetailComponent
          .loadSuggestedSolrFieldsForSolrIndexWithId(this.currentSolrIndexId);
      })
      .catch(error => this.handleError(error));
  }

  handleError(error: any) {
    console.log('In AppComponent :: handleError');
    console.log(':: error = ' + error);
    this.showErrorMsg('An error occurred.'); // TODO Do a more detaillied error description
  }

  public showSuccessMsg(msgText: string) {
    this.toasterService.pop('success', '', msgText);
  }

  public showErrorMsg(msgText: string) {
    this.toasterService.pop('error', '', msgText);
  }

  public selectSolrIndex(newSolrIndexId: number) {
    console.log('In AppComponent :: selectSolrIndex :: newSolrIndexId = ' + JSON.stringify(newSolrIndexId));

    // ask for user acceptance eventually (modal confirmation) when changing solrIndex and reloading list, if detail's state is dirty
    const _this = this;
    function executeSelectSolrIndexOk() {
      console.log('In AppComponent :: selectSolrIndex :: executeSelectSolrIndexOk :: newSolrIndexId = ' + JSON.stringify(newSolrIndexId));
      console.log('_this.currentSolrIndexId = ' + JSON.stringify(_this.currentSolrIndexId));
      console.log('_this.solrIndexSelectOptionModel = ' + JSON.stringify(_this.solrIndexSelectOptionModel));

      _this.currentSolrIndexId = newSolrIndexId;
      _this.searchInputListComponent
        .loadSearchInputListForSolrIndexWithId(_this.currentSolrIndexId);
      _this.searchInputDetailComponent
        .loadSuggestedSolrFieldsForSolrIndexWithId(_this.currentSolrIndexId);
    }
    function executeSelectSolrIndexCancel() {
      console.log('In AppComponent :: selectSolrIndex :: executeSelectSolrIndexCancel');
      // reset the select-option model to keep in sync with currentSolrIndexId
      _this.solrIndexSelectOptionModel = _this.currentSolrIndexId;
    }
    this.searchInputListComponent
      .safeDirtyCheckAndEvtlConfirmModalExecute( executeSelectSolrIndexOk, executeSelectSolrIndexCancel );
  }

  private requestPublishRulesTxtToSolr(targetPlatform: string) {

    if (this.currentSolrIndexId !== null) {
      this.searchManagementService
        .updateRulesTxtForSolrIndex(this.currentSolrIndexId, targetPlatform)
        .then(retApiResult => {
          this.showSuccessMsg( retApiResult.message );
        })
        .catch(error => this.handleError(error));
    } // TODO handle else-case, if no currentSolrIndexId selected
  }

  public publishSolrConfig() {
    console.log('In AppComponent :: publishSolrConfig');
    this.requestPublishRulesTxtToSolr('PRELIVE');
  }

  public publishToLIVE() {
    console.log('In AppComponent :: publishToLIVE');
    this.requestPublishRulesTxtToSolr('LIVE');
  }
}
