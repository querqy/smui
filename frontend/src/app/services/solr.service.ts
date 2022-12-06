import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { BehaviorSubject } from 'rxjs';

import {
  DeploymentLogInfo,
  SolrIndex,
  SuggestedSolrField,
  ApiResult
} from '../models';
import { Subject } from 'rxjs';

const httpOptions = {
  headers: new HttpHeaders({
    'Content-Type':  'application/json'
  })
};

@Injectable({
  providedIn: 'root'
})
export class SolrService {
  currentSolrIndexId = '-1';
  currentSolrIndexIdSubject: Subject<string> = new Subject<string>();
  solrIndices: SolrIndex[];

  private readonly baseUrl = 'api/v1';
  private readonly solrRulesApiPath: string = 'rules-txt';
  private readonly solrFieldsApiPath: string = 'suggested-solr-field';
  private readonly solrIndexApiPath: string = 'solr-index';
  private readonly jsonHeader = new Headers({
    'Content-Type': 'application/json'
  });

  private rulesCollectionChangeEvent = new BehaviorSubject<string>('');

  emitRulesCollectionChangeEvent(msg: string){
    this.rulesCollectionChangeEvent.next(msg);
  }

  rulesCollectionChangeEventListener(){
    return this.rulesCollectionChangeEvent.asObservable();
  }

  constructor(private http: HttpClient) {
    this.currentSolrIndexIdSubject.subscribe(
      value => (this.currentSolrIndexId = value)
    );
  }

  listAllSolrIndices(): Promise<void> {
    return this.http
      .get<SolrIndex[]>(`${this.baseUrl}/${this.solrIndexApiPath}`)
      .toPromise()
      .then(solrIndices => {
        this.solrIndices = solrIndices;
        if (solrIndices.length > 0) {
          this.currentSolrIndexIdSubject.next(solrIndices[0].id);
        } else {
          this.currentSolrIndexIdSubject.next("-1");
        }
      });
  }

  changeCurrentSolrIndexId(solrIndexId: string) {
    this.currentSolrIndexIdSubject.next(solrIndexId);
  }

  updateRulesTxtForSolrIndex(
    solrIndexId: string,
    targetPlatform: string
  ): Promise<ApiResult> {
    const headers = { headers: this.jsonHeader };

    return this.http
      .post<ApiResult>(
        `${this.baseUrl}/${solrIndexId}/${
          this.solrRulesApiPath
        }/${targetPlatform}`,
        headers
      )
      .toPromise();
  }

  listAllSuggestedSolrFields(
    solrIndexId: string
  ): Promise<Array<string>> {
    return this.http
      .get<SuggestedSolrField[]>(
        `${this.baseUrl}/${solrIndexId}/${this.solrFieldsApiPath}`
      )
      .toPromise()
      .then(solrFieldNames =>
        solrFieldNames.reduce(
          (r: any[], s: SuggestedSolrField) => r.concat(s.name, '-' + s.name),
          []
        )
      );
  }

  getSuggestedFields(solrIndexId: string): Promise<Array<SuggestedSolrField>> {
    return this.http
      .get<SuggestedSolrField[]>(
        `${this.baseUrl}/${solrIndexId}/${this.solrFieldsApiPath}`
      )
      .toPromise();
  }

  deleteSuggestedField(solrIndexId: string, suggestedFieldId: string): Promise<ApiResult> {
    return this.http
      .delete<ApiResult>(`${this.baseUrl}/${solrIndexId}/${this.solrFieldsApiPath}/${suggestedFieldId}`)
      .toPromise();
  }

  createSuggestedField(solrIndexId: string, name: string): Promise<ApiResult> {
    const headers = { headers: this.jsonHeader };
    const body = JSON.stringify( { name: name});
    return this.http
      .put<ApiResult>(`${this.baseUrl}/${solrIndexId}/${this.solrFieldsApiPath}`, body, httpOptions)
      .toPromise();
  }

  lastDeploymentLogInfo(
    solrIndexId: string,
    targetSystem: string,
    raw: boolean = false
  ): Promise<DeploymentLogInfo> {
    const options = {
      params: {
        solrIndexId,
        targetSystem,
        raw: raw.toString()
      }
    };

    return this.http
      .get<DeploymentLogInfo>(`${this.baseUrl}/log/deployment-info`, options)
      .toPromise();
  }

  getSolrIndex(solrIndexId: string): Promise<SolrIndex> {
    return this.http
      .get<SolrIndex>(`${this.baseUrl}/${this.solrIndexApiPath}/${solrIndexId}`)
      .toPromise();
  }

  deleteSolrIndex(solrIndexId: string): Promise<ApiResult> {
    return this.http
      .delete<ApiResult>(`${this.baseUrl}/${this.solrIndexApiPath}/${solrIndexId}`)
      .toPromise();
  }

  createSolrIndex(name: string, description: string): Promise<ApiResult> {
    const headers = { headers: this.jsonHeader };
    const body = JSON.stringify( { name: name, description: description});
    return this.http
      .put<ApiResult>(`${this.baseUrl}/${this.solrIndexApiPath}`, body, httpOptions)
      .toPromise();
  }

  putSomething2(thingName: string) {
    const headers = { headers: this.jsonHeader };
    const body = JSON.stringify( { thingName: thingName });
    return this.http
      .put<ApiResult>(`${this.baseUrl}/put-something`, body, httpOptions)
      .toPromise();
  }

  putty() {
    const headers = { headers: this.jsonHeader };
    const body = JSON.stringify( { });
    return this.http
      .put<ApiResult>(`${this.baseUrl}/putty`, body, httpOptions)
      .toPromise();
  }

  getExport() {
    const headers = { headers: this.jsonHeader };
    const body = {};
    return this.http
      .get<ApiResult>(`${this.baseUrl}/get-export`, body)
      .toPromise();
  }

  getExportWithId(id:String ) {
    const headers = { headers: this.jsonHeader };
    const body = {};
    return this.http
      .get<ApiResult>(`${this.baseUrl}/get-export-with-id/${id}`, body)
      .toPromise();
  }

}
