import { Injectable } from '@angular/core';
import { Headers, Http, Response } from '@angular/http';

import * as smm from './search-management.model';

import 'rxjs/add/operator/toPromise';

const SEARCH_MANAGEMENT_API_BASE_URL = 'api/v1';
const SOLR_INDEX_API_URL = 'solr-index';
const SEARCH_INPUT_API_URL = 'search-input';
const RULES_TXT_API_URL = 'rules-txt';
const SUGGESTED_SOLR_FIELD_API_URL = 'suggested-solr-field';

// TODO should this become part of the .model.ts? Is it even being referenced at (or can it be removed)?
export class SearchManagementServiceResult {
  result: string;
  message: string;
  returnId: string;
}

@Injectable()
export class SearchManagementService {

  constructor(private http: Http) {
  }

  listAllSolrIndeces(): Promise<Array<smm.SolrIndex>> {
    return this.http
      .get(SEARCH_MANAGEMENT_API_BASE_URL + '/' + SOLR_INDEX_API_URL)
      .toPromise()
      .then(res => {
        return res.json() as smm.SolrIndex[];
      })
      .catch(this.handleError);
  }

  listAllInputTags(): Promise<Array<smm.InputTag>> {
    return this.http
      .get(SEARCH_MANAGEMENT_API_BASE_URL + '/inputTags')
      .toPromise()
      .then(res => {
        return res.json() as smm.InputTag[];
      })
      .catch(this.handleError);
  }

  listAllSearchInputsInclSynonyms(solrIndexId: string): Promise<Array<smm.SearchInput>> {
    return this.http
      .get(SEARCH_MANAGEMENT_API_BASE_URL + '/' + solrIndexId + '/' + SEARCH_INPUT_API_URL)
      .toPromise()
      .then(res => {
        return res.json() as smm.SearchInput[];
      })
      .catch(this.handleError);
  }

  addNewSearchInput(solrIndexId: string, searchInputTerm: string): Promise<SearchManagementServiceResult> {
    const headers = new Headers({
      'Content-Type': 'application/json'
    });

    return this.http
      .put(
        SEARCH_MANAGEMENT_API_BASE_URL + '/' + solrIndexId + '/' + SEARCH_INPUT_API_URL,
        JSON.stringify( { term: searchInputTerm }),
        { headers: headers })
      .toPromise()
      .then(res => {
        console.log('In SearchManagementService :: addNewSearchInput :: put :: then :: res.json() = ' + JSON.stringify(res.json()));
        if (res.status !== 200) {
          // TODO error Handling mit anzeige
        }
        const searchManagementServiceResult = res.json() as SearchManagementServiceResult;
        if (searchManagementServiceResult.result !== 'OK') {
          // TODO error Handling mit anzeige
        }
        return searchManagementServiceResult;
      })
      .catch(this.handleError); // TODO error handling mit anzeige einer nachricht
  }

  getDetailedSearchInput(searchInputId: string): Promise<smm.SearchInput> {
    return this.http
      .get(SEARCH_MANAGEMENT_API_BASE_URL + '/' + SEARCH_INPUT_API_URL + '/' + searchInputId)
      .toPromise()
      .then(res => {
        return res.json() as smm.SearchInput;
      })
      .catch(this.handleError);
  }

  updateSearchInput(searchInput: smm.SearchInput): Promise<SearchManagementServiceResult> {
    const headers = new Headers({
      'Content-Type': 'application/json'
    });

    return this.http
      .post(
        SEARCH_MANAGEMENT_API_BASE_URL + '/' + SEARCH_INPUT_API_URL + '/' + searchInput.id,
        JSON.stringify( searchInput ),
        { headers: headers })
      .toPromise()
      .then(res => {
        console.log('In SearchManagementService :: updateSearchInput :: post :: then :: res.json() = ' + JSON.stringify(res.json()));
        if (res.status !== 200) {
          // TODO error Handling mit anzeige
        }
        const searchManagementServiceResult = res.json() as SearchManagementServiceResult;
        if (searchManagementServiceResult.result !== 'OK') {
          // TODO error Handling mit anzeige
        }
        return searchManagementServiceResult;
      })
      .catch(this.handleError); // TODO error handling mit anzeige einer nachricht
  }

  deleteSearchInput(searchInputId: number): Promise<SearchManagementServiceResult> {
    return this.http
      .delete(SEARCH_MANAGEMENT_API_BASE_URL + '/' + SEARCH_INPUT_API_URL + '/' + searchInputId)
      .toPromise()
      .then(res => {
        console.log('In SearchManagementService :: deleteSearchInput :: delete :: then :: res.json() = ' + JSON.stringify(res.json()));
        if (res.status !== 200) {
          // TODO error Handling mit anzeige
        }
        const searchManagementServiceResult = res.json() as SearchManagementServiceResult;
        if (searchManagementServiceResult.result !== 'OK') {
          // TODO error Handling mit anzeige
        }
        return searchManagementServiceResult;
      })
      .catch(this.handleError);
  }

  updateRulesTxtForSolrIndex(solrIndexId: string, targetPlatform: string): Promise<SearchManagementServiceResult> {
    const headers = new Headers({
      'Content-Type': 'application/json'
    });

    return this.http
      .post(
        SEARCH_MANAGEMENT_API_BASE_URL
        + '/' + solrIndexId
        + '/' + RULES_TXT_API_URL
        + '/' + targetPlatform,
        { headers: headers })
      .toPromise()
      .then(res => {
        console.log('In SearchManagementService :: updateRulesTxtForSolrIndex' +
          ':: post :: then :: res.json() = ' + JSON.stringify(res.json()));
        if (res.status !== 200) {
          // TODO error Handling mit anzeige
        }
        const searchManagementServiceResult = res.json() as SearchManagementServiceResult;
        if (searchManagementServiceResult.result !== 'OK') {
          // TODO error Handling mit anzeige
        }
        return searchManagementServiceResult;
      })
      .catch(this.handleError); // TODO error handling mit anzeige einer nachricht
  }

  listAllSuggestedSolrFields(solrIndexId: string): Promise<Array<smm.SuggestedSolrField>> {
    return this.http
      .get(SEARCH_MANAGEMENT_API_BASE_URL + '/' + solrIndexId + '/' + SUGGESTED_SOLR_FIELD_API_URL)
      .toPromise()
      .then(res => {
        return res.json() as smm.SuggestedSolrField[];
      })
      .catch(this.handleError);
  }

  private handleError(error: any): Promise<any> {
    console.error('An error occurred', error);
    return Promise.reject(error.message || error);
  }

}
