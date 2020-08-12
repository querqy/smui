import { Component, OnInit, ViewChild, SimpleChanges } from '@angular/core';
import { ToasterService, ToasterConfig } from 'angular2-toaster';

import { HeaderNavComponent } from './header-nav/index';
import { RuleManagementComponent, SpellingsComponent, ActivityLogComponent } from './details/index';
import { RulesListComponent, RulesSearchComponent } from './rules-panel/index';
import { InputTag, ListItem, SuggestedSolrField } from '../models/index';
import { FeatureToggleService, RuleManagementService, SolrService, TagsService } from '../services/index';

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

  private spellingDetailComponent: SpellingsComponent = null;
  private listItems: ListItem[] = [];
  private searchInputTerm = '';
  private selectedListItem: ListItem = null;
  private allTags = [];
  private appliedTagFilter: InputTag = null;

  get self(): AppComponent {
    return this;
  }

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
    private tagsService: TagsService
  ) {}

  ngOnInit() {
    console.log('In AppComponent :: ngOnInit')
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

  private showLongErrorMessage(errorMessage: string) {
    this.errorMessageModalText = errorMessage;
    $('#errorMessageModal').modal('show');
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

