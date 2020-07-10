import { Injectable } from '@angular/core';
import { Headers, Http } from '@angular/http';
import 'rxjs/add/operator/toPromise';

import { ApiResult, SearchInput } from '../models/index';

@Injectable()
export class RuleManagementService {

  private readonly baseUrl = 'api/v1';
  private readonly searchInputApiPath: string = 'search-input';

  private readonly jsonHeader = new Headers({'Content-Type': 'application/json'});

  // TODO consider other persistence solution (e.g. REST)
  public  readonly upDownDropdownDefinitionMappings = [
    { displayName: 'UP(+++++)', upDownType: 0, boostMalusValue: 500 },
    { displayName: 'UP(++++)', upDownType: 0, boostMalusValue: 100 },
    { displayName: 'UP(+++)', upDownType: 0, boostMalusValue: 50 },
    { displayName: 'UP(++)', upDownType: 0, boostMalusValue: 10 },
    { displayName: 'UP(+)', upDownType: 0, boostMalusValue: 5 },
    { displayName: 'DOWN(-)', upDownType: 1, boostMalusValue: 5 },
    { displayName: 'DOWN(--)', upDownType: 1, boostMalusValue: 10 },
    { displayName: 'DOWN(---)', upDownType: 1, boostMalusValue: 50 },
    { displayName: 'DOWN(----)', upDownType: 1, boostMalusValue: 100 },
    { displayName: 'DOWN(-----)', upDownType: 1, boostMalusValue: 500 }
  ];

  constructor(private http: Http) { }

  listAllSearchInputsInclSynonyms(solrIndexId: string): Promise<Array<SearchInput>> {
    return this.http
      .get(`${this.baseUrl}/${solrIndexId}/${this.searchInputApiPath}`)
      .toPromise()
      .then(res => res.json() as SearchInput[])
  }

  addNewRuleItem(solrIndexId: string, searchInputTerm: string, tags: string[] = []): Promise<string> {
    const headers = { headers: this.jsonHeader };
    const body = JSON.stringify( { term: searchInputTerm, tags: tags });

    return this.http
      .put(`${this.baseUrl}/${solrIndexId}/${this.searchInputApiPath}`, body, headers)
      .toPromise()
      .then(res => res.json().returnId)
  }

  getDetailedSearchInput(searchInputId: string): Promise<SearchInput> {
    return this.http
      .get(`${this.baseUrl}/${this.searchInputApiPath}/${searchInputId}`)
      .toPromise()
      .then(res => res.json() as SearchInput)
  }

  updateSearchInput(searchInput: SearchInput): Promise<string> {
    const headers = { headers: this.jsonHeader };
    const body = JSON.stringify(searchInput);

    return this.http
      .post(`${this.baseUrl}/${this.searchInputApiPath}/${searchInput.id}`, body, headers)
      .toPromise()
      .then(res => res.json().returnId)
  }

  deleteSearchInput(searchInputId: string): Promise<ApiResult> {
    return this.http
      .delete(`${this.baseUrl}/${this.searchInputApiPath}/${searchInputId}`)
      .toPromise()
      .then(res => res.json() as ApiResult)
  }

}
