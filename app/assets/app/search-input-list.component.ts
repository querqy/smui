import { Component, Input, OnInit } from '@angular/core';
import { Router } from '@angular/router';

import { AppComponent } from './app.component';
import { SearchInputDetailComponent } from './search-input-detail.component';

import * as smm from './search-management.model';
import { SearchManagementService } from './search-management.service';
import { FeatureToggleService } from './feature-toggle.service';

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

declare var $: any; // TODO include @types/jquery properly, make this workaround unnecessary

@Component({
  selector: 'smui-search-input-list',
  templateUrl: './search-input-list.component.html',
  styleUrls: ['./search-input-list.component.css'],
  providers: [FeatureToggleService]
})
export class SearchInputListComponent implements OnInit {

  // TODO consider using an more abstract component-communication model (e.g. message-service, events, etc.)
  @Input() detailComponent: SearchInputDetailComponent;
  @Input() parentComponent: AppComponent;

  public searchInputs: smm.SearchInput[];
  public selectedSearchInputId: number = null;
  public searchInputTerm = '';
  // TODO consider outsourcing confirmation modal dialog to separate component, directive ...
  public confirmTitle = '';
  public confirmBodyText = '';
  public cancelText = '';
  public okText = '';
  private currentSolrIndexId = -1; // TODO maybe take parentComponent's currentSolrIndexId instead of local copy
  private modalConfirmDeferred: Deferred<boolean>;

  constructor(
    private searchManagementService: SearchManagementService,
    public featureToggleService: FeatureToggleService /*, TODO use or remove "route" DI
    private route: ActivatedRoute */) {
  }

  ngOnInit() {
    console.log('In SearchInputSearchComponent :: ngOnInit');
  }

  handleError(error: any) {
    console.log('In SearchInputDetailComponent :: handleError');
    console.log(':: error = ' + error);
    this.parentComponent
      .showErrorMsg('An error occurred.'); // TODO Do a more detaillied error description
  }

  public getTypeaheadFilteredSearchInputs(): smm.SearchInput[] {
    if (this.searchInputTerm.trim().length > 0) {
      return this.searchInputs.filter(i => {
        // if term contains the searchInputTerm, we can go for this SearchInput
        if (i.term.toLowerCase().indexOf(this.searchInputTerm.toLowerCase()) !== -1) {
          return true;
        }
        // otherwise, we have a chance in the synonyms ...
        // TODO evaluate to check for undirected synonyms (synonymType) only
        for (const s of i.synonymRules) {
          if (s.term.toLowerCase().indexOf(this.searchInputTerm.toLowerCase()) !== -1) {
            return true;
          }
        }
        return false;
      });
    } else {
      return this.searchInputs;
    }
  }

  public addNewSearchInput() {
    console.log('In SearchInputSearchComponent :: addNewSearchInput :: this.searchInputTerm = ' + JSON.stringify(this.searchInputTerm));

    // ensure searchInputDetailComponent is not dirty, so that with reselection of new entry, potential user input data is not lost
    const _this = this;
    function executeAddNewSearchInput() {

      _this.searchManagementService.addNewSearchInput(_this.currentSolrIndexId, _this.searchInputTerm)
        .then(res => {
          console.log('In SearchInputSearchComponent :: executeAddNewSearchInput :: then :: res = ' + JSON.stringify(res));

          if (res.result === 'OK') {
            _this.updateSearchInputListAndSelectNewlyAddedItemWithId(res.returnId);
            _this.searchInputTerm = '';
            _this.parentComponent
              .showSuccessMsg('Adding new Search Input successful.');
          }
        })
        .catch(error => _this.handleError(error));
    }
    this.safeDirtyCheckAndEvtlConfirmModalExecute( executeAddNewSearchInput, null );
  }

  public updateSearchInputListAndSelectNewlyAddedItemWithId(selectSearchInputId: number) {
    console.log('In SearchInputListComponent :: updateSearchInputListAndSelectNewlyAddedItemWithId :: ' +
      'selectSearchInputId = ' + JSON.stringify(selectSearchInputId));

    this.searchManagementService
      .listAllSearchInputsInclSynonyms(this.currentSolrIndexId)
      .then(retSearchInputs => {
        this.searchInputs = retSearchInputs

        /*
        TODO alten Code, der über den Index ging, entfernen

        // lookup index of newly created SearchInput and set selectedSearchInputIndex accordingly
        const toSelectIdx = this.searchInputs.findIndex(s => s.id === selectSearchInputId);
        console.log('In SearchInputListComponent :: updateSearchInputListAndSelectNewlyAddedItemWithId :: then :: ' +
          'toSelectIdx = ' + JSON.stringify(toSelectIdx));
        this.selectedSearchInputIndex = toSelectIdx;
        */

        this.selectedSearchInputId = selectSearchInputId;
        this.detailComponent
          .showDetailsForSearchInputWithId(this.selectedSearchInputId);
      })
      .catch(error => this.handleError(error));
  }

  public loadSearchInputListForSolrIndexWithId(solrIndexId: number) {
    console.log('In SearchInputListComponent :: loadSearchInputListForSolrIndexWithId :: solrIndexId = ' + JSON.stringify(solrIndexId));
    this.currentSolrIndexId = solrIndexId;
    this.searchManagementService
      .listAllSearchInputsInclSynonyms(this.currentSolrIndexId)
      .then(retSearchInputs => {
        this.searchInputs = retSearchInputs;
        this.searchInputTerm = '';
        this.selectedSearchInputId = null;
        this.detailComponent
          .showDetailsForSearchInputWithId(null);
      })
      .catch(error => this.handleError(error));
  }

  public reloadSearchInputListAfterDetailUpdate() {
    console.log('In SearchInputListComponent :: reloadSearchInputListAfterDetailUpdate');
    this.searchManagementService
      .listAllSearchInputsInclSynonyms(this.currentSolrIndexId)
      .then(retSearchInputs => {
        this.searchInputs = retSearchInputs
      })
      .catch(error => this.handleError(error));
  }

  // bridge angular2-to-jquery for opening the bootstrap confirmModal and map to a Promise<boolean> (modalConfirmPromise)
  // TODO consider outsourcing modal confirmation implementation to component, service or directive ...

  openModalConfirm(title, bodyText, okText, cancelText) {
    console.log('In SearchInputListComponent :: openModalConfirm');

    this.confirmTitle = title;
    this.confirmBodyText = bodyText;
    this.okText = okText;
    this.cancelText = cancelText;

    $('#confirmModal').modal('show');
    this.modalConfirmDeferred = new Deferred<boolean>();
  }

  confirmModalCancel() {
    console.log('In SearchInputListComponent :: confirmModalCancel');
    this.modalConfirmDeferred.resolve(false);
  }

  confirmModalOk() {
    console.log('In SearchInputListComponent :: confirmModalOk');
    this.modalConfirmDeferred.resolve(true);
  }

  // TODO consider dirty check for details being part of the details component instead of list
  public safeDirtyCheckAndEvtlConfirmModalExecute(executeFnOk: Function, executeFnCancel: Function) {
    console.log('In safeDirtyCheckAndEvtlConfirmModalExecute');

    const detailComponentsIsDirty = this.detailComponent.isDirty();
    console.log('detailComponentsIsDirty = ' + detailComponentsIsDirty);

    // show confirmation dialog, if detail component is dirty
    if (detailComponentsIsDirty) {
      this.openModalConfirm(
        'Confirmation',
        'You have unsaved input! Do you really want to Cancel Editing of Search Input or Continue with it?',
        'Yes, Cancel Editing', 'No, Continue Editing');
      this.modalConfirmDeferred.promise
        .then(isOk => {
//          console.log('In SearchInputListComponent :: safeDirtyCheckAndEvtlConfirmModalExecute' +
//            ' :: then :: isOk = ' + isOk + ' -- this = ' + this);
          if (isOk) {
            executeFnOk();
          } else {
            if (executeFnCancel !== null) {
              executeFnCancel();
            }
          }
        });
    } else {
      executeFnOk();
    }
  }

  public selectSearchInputWidthId(searchInputId: number) {
    console.log('In SearchInputListComponent :: clickSearchInput :: searchInputId = ' + searchInputId);

    // preserve this and outsource functional implementation of selectSearchInput into execure... method
    const _this = this;
    function executeSelectSearchInput() {
      // Deselect, if same index was selected twice
      if (_this.selectedSearchInputId === searchInputId) {
        _this.selectedSearchInputId = null;

        _this.detailComponent
          .showDetailsForSearchInputWithId(null);
      } else {
        // In any case else, select current line and check/inform detailComponent
        _this.selectedSearchInputId = searchInputId;

        _this.detailComponent
          .showDetailsForSearchInputWithId(_this.selectedSearchInputId);
      }
    };

    this.safeDirtyCheckAndEvtlConfirmModalExecute( executeSelectSearchInput, null );
  }

  public deleteSearchInputWithId(searchInputId: number) {
    console.log('In SearchInputListComponent :: deleteSearchInput :: searchInputId = ' + searchInputId);

    // TODO maybe before even starting the deletion process, check if details are dirty and ask to cancel editing eventually

    // ask for delete confirmation
    this.openModalConfirm(
      'Confirmation',
      'Are you sure deleting the Search Input?',
      'Yes', 'No');
    const _this = this;
    function executeDeleteSearchInput() {
      // if user accepts deletion, proceed deleting the entry
      _this.searchManagementService
        .deleteSearchInput(searchInputId)
        .then(retApiResult => {

          // Reload list and potentially re-handle selected SearchInput
          _this.searchManagementService
            .listAllSearchInputsInclSynonyms(_this.currentSolrIndexId)
            .then(retSearchInputs => {
              _this.searchInputs = retSearchInputs

              /*
              TODO reselect selected index, if deleted entry was the selected one
              TODO reselect selected index, if deleted entry was the first one
              */

              _this.selectedSearchInputId = null;
              _this.detailComponent
                .showDetailsForSearchInputWithId(null);
            })
            .catch(error => _this.handleError(error));
        })
        .catch(error => _this.handleError(error));
    }
    this.modalConfirmDeferred.promise
      .then(isOk => {
//        console.log('In SearchInputListComponent :: deleteSearchInput' +
//          ' :: then :: isOk = ' + isOk + ' -- this = ' + this);
        if (isOk) {
          executeDeleteSearchInput();
        }});
  }
}
