import { Component, OnInit, OnChanges, Input, ViewChild, SimpleChanges } from '@angular/core'
import { ToasterService } from 'angular2-toaster'

import { ModalDialogComponent } from '../modal-dialog/index'
import { RuleManagementComponent, SpellingsComponent, ActivityLogComponent } from '../details/index'
import { RulesListComponent, RulesSearchComponent } from '../rules-panel/index'
import { InputTag, ListItem, SuggestedSolrField } from '../../models/index'
import { FeatureToggleService, RuleManagementService, SolrService, TagsService } from '../../services/index'

@Component({
  selector: 'smui-search-management',
  templateUrl: './search-management.component.html',
  styleUrls: ['./search-management.component.css']
})
export class SearchManagementComponent implements OnInit, OnChanges {

  // TODO consider using an more abstract component-communication model (e.g. message-service, events, etc.)
  @ViewChild('rulesSearchComponent') rulesSearchComponent: RulesSearchComponent;
  @ViewChild('rulesListComponent') rulesListComponent: RulesListComponent;
  @ViewChild('ruleManagementComponent') ruleManagementComponent: RuleManagementComponent;
  @ViewChild('spellingDetailComponent') set ft(component: SpellingsComponent) {
    this.spellingDetailComponent = component
  };
  @ViewChild('activityLogComponent') activityLogComponent: ActivityLogComponent;

  @Input() currentSolrIndexId: string = null
  @Input() smuiModalDialog: ModalDialogComponent = null

  private spellingDetailComponent: SpellingsComponent = null;
  private listItems: ListItem[] = [];
  private searchInputTerm = '';
  private selectedListItem: ListItem = null;
  private suggestedSolrFieldNames = null;
  private allTags = [];
  private appliedTagFilter: InputTag = null;

  get self(): SearchManagementComponent {
    return this;
  }

  constructor(
    public featureToggleService: FeatureToggleService,
    private toasterService: ToasterService,
    private ruleManagementService: RuleManagementService,
    private solrService: SolrService,
    private tagsService: TagsService
  ) {}

  ngOnInit() {
    console.log('In SearchManagementComponent :: ngOnInit')
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes.currentSolrIndexId) {
      console.log('In SearchManagementComponent :: ngOnChanges :: currentSolrIndexId = ' + changes.currentSolrIndexId)
      this.selectedSolrIndexChange(changes.currentSolrIndexId.currentValue)
    }
  }

  public showSuccessMsg(msgText: string) {
    this.toasterService.pop('success', '', msgText);
  }

  public showErrorMsg(msgText: string) {
    this.toasterService.pop('error', '', msgText);
  }

  public selectedSolrIndexChange(solrIndexId) {
    console.log('In SearchManagementComponent :: selectedSolrIndexChange :: solrIndexId = ' + JSON.stringify(solrIndexId))
    this.solrService.listAllSuggestedSolrFields(solrIndexId)
      .then(suggestedSolrFieldNames => {
        this.suggestedSolrFieldNames = suggestedSolrFieldNames
      })
      .catch(error => this.showErrorMsg(error))

    this.tagsService.listAllInputTags()
      .then(allTags => {
        this.allTags = allTags
      })
      .catch(error => this.showErrorMsg(error))

    this.selectedListItem = null;
    this.searchInputTerm = '';
    this.appliedTagFilter = null;
  }

  public isDirty(): boolean {
    return (this.spellingDetailComponent ? this.spellingDetailComponent.isDirty() : false)
      || (this.ruleManagementComponent ? this.ruleManagementComponent.isDirty() : false)
  }

  public executeWithChangeCheck({ executeFnOk, executeFnCancel }) {
    console.log('In AppComponent :: executeWithChangeCheck');

    if (this.isDirty()) {
      const modalConfirmDeferred = this.smuiModalDialog.openModalConfirm(
        'Confirm to discard unsaved input',
        'You have unsaved input! Do you really want to Cancel Editing of Search Input or Continue with it?',
        'Yes, Cancel Editing', 'No, Continue Editing');

      modalConfirmDeferred.promise
        .then(isOk => isOk ? executeFnOk() : executeFnCancel ? executeFnCancel() : () => ({}));
    } else {
      executeFnOk()
    }
  }

  public openDeleteConfirmModal({itemType, deleteCallback}) {
    const modalConfirmDeferred = this.smuiModalDialog.openModalConfirm(
      `Confirm deletion of ${itemType}`,
      `Are you sure deleting the ${itemType}?`,
      'Yes', 'No');

    modalConfirmDeferred.promise
      .then(isOk => isOk && deleteCallback())
  }
}
