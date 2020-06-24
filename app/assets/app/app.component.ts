import { Component, OnInit, ViewChild } from '@angular/core';

import { SearchInputListComponent } from './search-input-list.component';
import { SearchInputDetailComponent } from './search-input-detail.component';
import { SpellingDetailComponent } from './spelling-detail.component';

import { ToasterService, ToasterConfig } from 'angular2-toaster';

import * as smm from './search-management.model';
import { SearchManagementService } from './search-management.service';
import { FeatureToggleService } from './feature-toggle.service';
import { ListItemType } from './search-management.model';

declare var $: any; // TODO include @types/jquery properly, make this workaround unnecessary

// TODO consider outsourcing confirmation modal dialog to separate component, directive ...
class Deferred<T> {
  promise: Promise<T>;
  resolve: (value?: T | PromiseLike<T>) => void;
  reject:  (reason?: any) => void;

  constructor() {
    this.promise = new Promise<T>((resolve, reject) => {
      this.resolve = resolve;
      this.reject  = reject;
    });
  }
}

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
  @ViewChild('spellingDetailComponent') set ft(component: SpellingDetailComponent) {
    this.spellingComponent = component
  };

  // TODO consider outsourcing confirmation modal dialog to separate component, directive ...
  public confirmTitle = '';
  public confirmBodyText = '';
  public cancelText = '';
  public okText = '';
  public modalConfirmDeferred: Deferred<boolean>;
  public errorMessageModalText = '';
  public deploymentRunningForStage = null;

  public hideDeploymentLogInfo = true;
  public deploymentLogInfo = 'Loading info ...';

  public selectedListItem = null;
  private spellingComponent: SpellingDetailComponent = null;

  get self(): AppComponent {
    return this;
  }

  public listSolrIndeces: smm.SolrIndex[];
  public allInputTags: smm.InputTag[];
  // TODO avoid to not separately keep currentSolrIndexId and according select-option model solrIndexSelectOptionModel
  public currentSolrIndexId: string = null;
  public solrIndexSelectOptionModel: string = null;

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
        this.searchInputDetailComponent
          .loadSuggestedSolrFieldsForSolrIndexWithId(this.currentSolrIndexId);
      }).then(() =>
        this.searchInputListComponent.refreshItemsInList(this.currentSolrIndexId)
          .then(() => {
            this.searchInputListComponent.selectListItem(null);
          })
      )
      .catch(error => this.handleError(error));

    this.searchManagementService.listAllInputTags().then(tags => {
      this.allInputTags = tags;
    })
  }

  handleError(error: any) {
    console.log('In AppComponent :: handleError');
    console.log(':: error = ', error);
    this.showErrorMsg('An error occurred.'); // TODO Do a more detaillied error description
  }

  public showSuccessMsg(msgText: string) {
    this.toasterService.pop('success', '', msgText);
  }

  public showErrorMsg(msgText: string) {
    this.toasterService.pop('error', '', msgText);
  }

  // bridge angular2-to-jquery for opening the bootstrap confirmModal and map to a Promise<boolean> (modalConfirmPromise)
  // TODO consider outsourcing modal confirmation implementation to component, service or directive ...

  public openModalConfirm(title, bodyText, okText, cancelText) {
    console.log('In AppComponent :: openModalConfirm');

    this.confirmTitle = title;
    this.confirmBodyText = bodyText;
    this.okText = okText;
    this.cancelText = cancelText;

    $('#confirmModal').modal('show');
    this.modalConfirmDeferred = new Deferred<boolean>();
  }

  confirmModalCancel() {
    console.log('In AppComponent :: confirmModalCancel');
    this.modalConfirmDeferred.resolve(false);
  }

  confirmModalOk() {
    console.log('In AppComponent :: confirmModalOk');
    this.modalConfirmDeferred.resolve(true);
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
      _this.searchInputListComponent.refreshItemsInList(newSolrIndexId)
        .then(() => {
          _this.searchInputListComponent.selectListItem(null);
        });
      _this.searchInputDetailComponent
        .loadSuggestedSolrFieldsForSolrIndexWithId(_this.currentSolrIndexId);
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
      this.searchManagementService
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

  private showLongErrorMessage(errorMessage: string) {
    this.errorMessageModalText = errorMessage;
    $('#errorMessageModal').modal('show');
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

    this.searchManagementService
      .lastDeploymentLogInfo(this.currentSolrIndexId, targetPlatform)
      .then(retApiResult => {
        this.deploymentLogInfo = retApiResult.msg;
      })
      .catch(error => {
        this.showLongErrorMessage(error.json().message)
      });
  }

  public executeWithChangeCheck({executeFnOk, executeFnCancel}) {
    console.log('In AppComponent :: executeWithChangeCheck');
    const hasChanged =
      (this.spellingComponent ? this.spellingComponent.isDirty() : false) ||
      (this.searchInputDetailComponent ? this.searchInputDetailComponent.isDirty() : false);

    if (hasChanged) {
      this.openModalConfirm(
        'Confirm to discard unsaved input',
        'You have unsaved input! Do you really want to Cancel Editing of Search Input or Continue with it?',
        'Yes, Cancel Editing', 'No, Continue Editing');

      this.modalConfirmDeferred.promise
        .then(isOk => isOk ? executeFnOk() : executeFnCancel ? executeFnCancel() : () => ({}));
    } else {
      executeFnOk()
    }
  }

  public createItem({itemType, apiCall}) {
    console.log(`In SearchInputSearchComponent :: createItem :: ${ListItemType[itemType]}`);

    this.executeWithChangeCheck({
      executeFnOk: () => apiCall()
        .then(res => {
          console.log('In SearchInputSearchComponent :: createItemByType :: then :: res = ' + JSON.stringify(res));
          this.searchInputListComponent.refreshItemsInList(this.currentSolrIndexId)
            .then(() => this.searchInputListComponent.selectListItemById(res.returnId))
            .then(() => this.showSuccessMsg(`Adding new ${ListItemType[itemType]} successful.`))
        })
        .catch(error => this.handleError(error)),
      executeFnCancel: () => ({})
    });
  }

  public deleteItemByType({itemType, id}) {
    console.log(`In SearchInputListComponent :: deleteItemByType :: id = ${id}; type = ${itemType}`);

    this.openModalConfirm(
      `Confirm deletion of ${itemType}`,
      `Are you sure deleting the ${itemType}?`,
      'Yes', 'No');

    const executeDeleteItem = () => {
      // if user accepts deletion, proceed deleting the entry
      this.searchManagementService
        .deleteItem(smm.ListItemType[itemType as string], id)
        .then(() =>
          this.searchInputListComponent.refreshItemsInList(this.currentSolrIndexId)
            .then(() => this.searchInputListComponent.selectListItemById(id))
        )
        .catch(error => this.handleError(error));
    };

    this.modalConfirmDeferred.promise.then(isOk => isOk && executeDeleteItem())
  }
}

