import {
  Component,
  OnInit,
  ViewChild,
} from '@angular/core'
import { ToasterService } from 'angular2-toaster'

import {
  RuleManagementComponent,
  SpellingsComponent,
  ActivityLogComponent
} from '../details'
import { RulesListComponent, RulesSearchComponent } from '../rules-panel'
import { InputTag, ListItem, SuggestedSolrField } from '../../models'
import {
  FeatureToggleService,
  ModalService,
  RuleManagementService,
  SolrService,
  TagsService
} from '../../services'

@Component({
  selector: 'smui-search-management',
  templateUrl: './search-management.component.html'
})
export class SearchManagementComponent implements OnInit {
  // TODO consider using an more abstract component-communication model (e.g. message-service, events, etc.)
  @ViewChild('rulesSearchComponent') rulesSearchComponent: RulesSearchComponent
  @ViewChild('rulesListComponent') rulesListComponent: RulesListComponent
  @ViewChild('ruleManagementComponent')
  ruleManagementComponent: RuleManagementComponent
  @ViewChild('spellingDetailComponent') set ft(component: SpellingsComponent) {
    this.spellingDetailComponent = component
  }
  @ViewChild('activityLogComponent') activityLogComponent: ActivityLogComponent

  currentSolrIndexId?: string
  spellingDetailComponent?: SpellingsComponent
  listItems: ListItem[] = []
  searchInputTerm?: string
  selectedListItem?: ListItem
  suggestedSolrFieldNames: SuggestedSolrField[] = []
  showTags: boolean = false
  allTags: InputTag[] = []
  appliedTagFilter?: InputTag

  constructor(
    public featureToggleService: FeatureToggleService,
    private toasterService: ToasterService,
    private ruleManagementService: RuleManagementService,
    private solrService: SolrService,
    private tagsService: TagsService,
    private modalService: ModalService
  ) {
    this.solrService.currentSolrIndexIdSubject.subscribe(value => {
      this.currentSolrIndexId = value
      this.selectedSolrIndexChange(value)
    })
  }

  ngOnInit() {
    console.log('In SearchManagementComponent :: ngOnInit')
    this.currentSolrIndexId = this.solrService.currentSolrIndexId
    this.showTags = this.featureToggleService.isRuleTaggingActive()

    if (this.currentSolrIndexId) {
      this.selectedSolrIndexChange(this.currentSolrIndexId)
    }
  }

  public showSuccessMsg(msgText: string) {
    this.toasterService.pop('success', '', msgText)
  }

  public showErrorMsg(msgText: string) {
    this.toasterService.pop('error', '', msgText)
  }

  public selectedSolrIndexChange(solrIndexId: string) {
    console.log(
      'In SearchManagementComponent :: selectedSolrIndexChange :: solrIndexId = ' +
        JSON.stringify(solrIndexId)
    )

    this.solrService
      .listAllSuggestedSolrFields(solrIndexId)
      .then(suggestedSolrFieldNames => {
        this.suggestedSolrFieldNames = suggestedSolrFieldNames
      })
      .catch(error => this.showErrorMsg(error))

    if (this.showTags) {
      this.tagsService
        .listAllInputTags()
        .then(allTags => {
          this.allTags = allTags
        })
        .catch(error => this.showErrorMsg(error))
    }

    this.selectedListItem = undefined
    this.searchInputTerm = undefined
    this.appliedTagFilter = undefined
  }

  public isDirty(): boolean {
    return (
      (this.spellingDetailComponent
        ? this.spellingDetailComponent.isDirty()
        : false) ||
      (this.ruleManagementComponent
        ? this.ruleManagementComponent.isDirty()
        : false)
    )
  }

  // @ts-ignore
  public executeWithChangeCheck({ executeFnOk }) {
    if (this.isDirty()) {
      const deferred = this.modalService.open('confirm-unsaved')
      deferred.promise.then((isOk: boolean) => {
        if (isOk) executeFnOk()
        this.modalService.close('confirm-unsaved')
      })
    } else {
      executeFnOk()
    }
  }

  // @ts-ignore
  public openDeleteConfirmModal({ deleteCallback }) {
    const deferred = this.modalService.open('confirm-delete')
    deferred.promise.then((isOk: boolean) => {
      if(isOk) deleteCallback()
      this.modalService.close('confirm-delete')
    })
  }
}
