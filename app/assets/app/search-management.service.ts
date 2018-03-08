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
  returnId: number;
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

  listAllSearchInputsInclSynonyms(solrIndexId: number): Promise<Array<smm.SearchInput>> {
    return this.http
      .get(SEARCH_MANAGEMENT_API_BASE_URL + '/' + solrIndexId + '/' + SEARCH_INPUT_API_URL)
      .toPromise()
      .then(res => {
        return res.json() as smm.SearchInput[];
      })
      .catch(this.handleError);
  }

  addNewSearchInput(solrIndexId: number, searchInputTerm: string): Promise<SearchManagementServiceResult> {
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

  getDetailedSearchInput(searchInputId: number): Promise<smm.SearchInput> {
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

  updateRulesTxtForSolrIndex(solrIndexId: number): Promise<SearchManagementServiceResult> {
    const headers = new Headers({
      'Content-Type': 'application/json'
    });

    return this.http
      .post(
        SEARCH_MANAGEMENT_API_BASE_URL + '/' + solrIndexId + '/' + RULES_TXT_API_URL,
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

  listAllSuggestedSolrFields(solrIndexId: number): Promise<Array<smm.SuggestedSolrField>> {
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



/*
  getHeroes(): Promise<Array<Hero>> {
    return this.http
      .get(this.heroesUrl)
      .toPromise()
      .then((response) => {
        return response.json().data as Hero[];
      })
      .catch(this.handleError);
  }

  getHero(id: number): Promise<Hero> {
    return this.getHeroes()
      .then(heroes => heroes.find(hero => hero.id === id));
  }

  save(hero: Hero): Promise<Hero> {
    if (hero.id) {
      return this.put(hero);
    }
    return this.post(hero);
  }

  delete(hero: Hero): Promise<Response> {
    const headers = new Headers();
    headers.append('Content-Type', 'application/json');

    const url = `${this.heroesUrl}/${hero.id}`;

    return this.http
      .delete(url, { headers: headers })
      .toPromise()
      .catch(this.handleError);
  }

  // Add new Hero
  private post(hero: Hero): Promise<Hero> {
    const headers = new Headers({
      'Content-Type': 'application/json'
    });

    return this.http
      .post(this.heroesUrl, JSON.stringify(hero), { headers: headers })
      .toPromise()
      .then(res => res.json().data)
      .catch(this.handleError);
  }

  // Update existing Hero
  private put(hero: Hero): Promise<Hero> {
    const headers = new Headers();
    headers.append('Content-Type', 'application/json');

    const url = `${this.heroesUrl}/${hero.id}`;

    return this.http
      .put(url, JSON.stringify(hero), { headers: headers })
      .toPromise()
      .then(() => hero)
      .catch(this.handleError);
  }

  private handleError(error: any): Promise<any> {
    console.error('An error occurred', error);
    return Promise.reject(error.message || error);
  }
*/



}
