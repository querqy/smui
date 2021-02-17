import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import {
  DeploymentLogInfo,
  SolrIndex,
  SuggestedSolrField,
  ApiResult
} from '../models';
import { Subject } from 'rxjs';

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
        if (solrIndices.length > 0) {
          this.solrIndices = solrIndices;
          this.currentSolrIndexIdSubject.next(solrIndices[0].id);
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
}
