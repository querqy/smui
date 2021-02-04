import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';

import { ApiResult, SearchInput } from '../models';
import { FeatureToggleService } from './feature-toggle.service';

const httpOptions = {
  headers: new HttpHeaders({
    'Content-Type':  'application/json'
  })
};

@Injectable({
  providedIn: 'root'
})
export class RuleManagementService {
  public readonly upDownDropdownDefinitionMappings = this.featureToggleService
    .getSyncToggleCustomUpDownDropdownMappings();

  private readonly baseUrl = 'api/v1';
  private readonly searchInputApiPath: string = 'search-input';

  /*
  TODO consider providing a super-super fallback, if backend delivered values are invalid?
  [
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
  ]
  */

  constructor(
    public featureToggleService: FeatureToggleService,
    private http: HttpClient
  ) { }

  listAllSearchInputsInclSynonyms(solrIndexId: string): Promise<Array<SearchInput>> {
    return this.http
      .get<SearchInput[]>(`${this.baseUrl}/${solrIndexId}/${this.searchInputApiPath}`)
      .toPromise();
  }

  addNewRuleItem(solrIndexId: string, searchInputTerm?: string, tags: string[] = []): Promise<ApiResult> {
    const body = JSON.stringify( { term: searchInputTerm || '', tags });

    return this.http
      .put<ApiResult>(`${this.baseUrl}/${solrIndexId}/${this.searchInputApiPath}`, body, httpOptions)
      .toPromise();
  }

  getDetailedSearchInput(searchInputId: string): Promise<SearchInput> {
    return this.http
      .get<SearchInput>(`${this.baseUrl}/${this.searchInputApiPath}/${searchInputId}`)
      .toPromise();
  }

  updateSearchInput(searchInput: SearchInput): Promise<ApiResult> {
    return this.http
      .post<ApiResult>(`${this.baseUrl}/${this.searchInputApiPath}/${searchInput.id}`, searchInput, httpOptions)
      .toPromise();
  }

  deleteSearchInput(searchInputId: string): Promise<ApiResult> {
    return this.http
      .delete<ApiResult>(`${this.baseUrl}/${this.searchInputApiPath}/${searchInputId}`)
      .toPromise();
  }

}
