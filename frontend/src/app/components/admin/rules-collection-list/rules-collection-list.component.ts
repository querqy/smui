import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnChanges,
  OnInit,
  SimpleChanges
} from '@angular/core';

import { DeploymentLogInfo, SmuiVersionInfo, SolrIndex } from '../../../models';
import {
  FeatureToggleService,
  SolrService,
  ConfigService,
  ModalService
} from '../../../services';

@Component({
  selector: 'app-smui-admin-rules-collection-list',
  templateUrl: './rules-collection-list.component.html'
})
export class RulesCollectionListComponent implements OnInit, OnChanges {

  @Output() openDeleteConfirmModal: EventEmitter<any> = new EventEmitter();
  @Output() showErrorMsg: EventEmitter<string> = new EventEmitter();

  solrIndices: SolrIndex[];

  constructor(
    private solrService: SolrService,
  ) {

  }
  ngOnInit() {
    console.log('In RulesCollectionListComponent :: ngOnInit');
    this.solrIndices = this.solrService.solrIndices;
  }

  ngOnChanges(changes: SimpleChanges): void {
    console.log('In RulesCollectionListComponent :: ngOnChanges');
    //if (this.commonService.hasChanged(changes, 'currentSolrIndexId')) {
      //this.refreshItemsInList().catch(error => this.showErrorMsg.emit(error));
    //}
  }

  refreshSolrIndicies() {
    return this.solrService.listAllSolrIndices;
    //  : Promise.reject('No selected Solr index');
  }


  deleteRulesCollection(id: string, event: Event) {
    console.log("I am in deleteRulesCollection");
    event.stopPropagation();
    const deleteCallback = () =>
      this.solrService
        .deleteSolrIndex(id)
        .then(() => this.refreshSolrIndicies())
        .catch(error => this.showErrorMsg.emit(error));

        //.then(() => this.refreshItemsInList())
        ////.then(() => this.selectListItem(undefined))

    this.openDeleteConfirmModal.emit({ deleteCallback });
  }


}
