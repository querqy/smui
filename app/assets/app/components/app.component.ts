import { Component, OnInit, ViewChild, SimpleChanges } from '@angular/core';
import { ToasterService, ToasterConfig } from 'angular2-toaster';

import { RuleManagementComponent, SpellingsComponent, ActivityLogComponent } from './details/index';
import { RulesListComponent, RulesSearchComponent } from './rules-panel/index';
import {InputTag, ListItem, SolrIndex, SuggestedSolrField} from '../models/index';
import {FeatureToggleService, RuleManagementService, SolrService, TagsService} from '../services/index';

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
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {

  // TODO consider using an more abstract component-communication model (e.g. message-service, events, etc.)
  @ViewChild('rulesSearchComponent') rulesSearchComponent: RulesSearchComponent;
  @ViewChild('rulesListComponent') rulesListComponent: RulesListComponent;
  @ViewChild('ruleManagementComponent') ruleManagementComponent: RuleManagementComponent;
  @ViewChild('spellingDetailComponent') set ft(component: SpellingsComponent) {
    this.spellingDetailComponent = component
  };
  @ViewChild('activityLogComponent') activityLogComponent: ActivityLogComponent;

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

  private spellingDetailComponent: SpellingsComponent = null;
  private listItems: ListItem[] = [];
  private searchInputTerm = '';
  private selectedListItem: ListItem = null;
  private allTags = null;
  private appliedTagFilter: InputTag = null;

  get self(): AppComponent {
    return this;
  }

  public listSolrIndeces: SolrIndex[];
  // TODO avoid to not separately keep currentSolrIndexId and according select-option model solrIndexSelectOptionModel
  public currentSolrIndexId: string = null;
  public solrIndexSelectOptionModel: string = null;
  private suggestedSolrFieldNames = null;

  public toasterConfig: ToasterConfig =
    new ToasterConfig({
      showCloseButton: false,
      tapToDismiss: true,
      timeout: 5000,
      positionClass: 'toast-bottom-right'
  });

  constructor(
    public featureToggleService: FeatureToggleService,
    private toasterService: ToasterService,
    private ruleManagementService: RuleManagementService,
    private solrService: SolrService,
    private tagsService: TagsService
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
            .then(suggestedSolrFieldNames => this.suggestedSolrFieldNames = suggestedSolrFieldNames)
            .catch(error => this.showErrorMsg(error));

          this.allTags = this.tagsService.listAllInputTags()
            .then(allTags => this.allTags = allTags)
            .catch(error => this.showErrorMsg(error));
        }
      })
      .catch(error => this.showErrorMsg(error))
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
      _this.selectedListItem = null;

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

    this.solrService
      .lastDeploymentLogInfo(this.currentSolrIndexId, targetPlatform)
      .then(retApiResult => {
        this.deploymentLogInfo = retApiResult.msg;
      })
      .catch(error => {
        this.showLongErrorMessage(error.json().message)
      });
  }

  public executeWithChangeCheck({ executeFnOk, executeFnCancel }) {
    console.log('In AppComponent :: executeWithChangeCheck');
    const hasChanged =
      (this.spellingDetailComponent ? this.spellingDetailComponent.isDirty() : false) ||
      (this.ruleManagementComponent ? this.ruleManagementComponent.isDirty() : false);

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

  public openDeleteConfirmModal({itemType, deleteCallback}) {
    this.openModalConfirm(
      `Confirm deletion of ${itemType}`,
      `Are you sure deleting the ${itemType}?`,
      'Yes', 'No');

    this.modalConfirmDeferred.promise.then(isOk => isOk && deleteCallback())
  }
}

