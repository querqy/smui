import { Injectable } from '@angular/core';
import { Headers, Http } from '@angular/http';
import 'rxjs/add/operator/toPromise';

import { DeploymentLogInfo, SolrIndex, SuggestedSolrField } from '../models/solr.model';
import { ApiResult } from '../models/results.model';


@Injectable()
export class SolrService {
  private readonly baseUrl = 'api/v1';
  private readonly solrRulesApiPath: string = 'rules-txt';
  private readonly solrFieldsApiPath: string = 'suggested-solr-field';
  private readonly solrIndexApiPath: string = 'solr-index';

  private readonly jsonHeader = new Headers({'Content-Type': 'application/json'});

  constructor(public http: Http) { }

  listAllSolrIndeces(): Promise<Array<SolrIndex>> {
    return this.http
      .get(`${this.baseUrl}/${this.solrIndexApiPath}`)
      .toPromise()
      .then(res => res.json() as SolrIndex[])
  }

  updateRulesTxtForSolrIndex(solrIndexId: string, targetPlatform: string): Promise<ApiResult> {
    const headers = { headers: this.jsonHeader}

    return this.http
      .post(`${this.baseUrl}/${solrIndexId}/${this.solrRulesApiPath}/${targetPlatform}`, headers)
      .toPromise()
      .then(res => res.json() as ApiResult)
  }

  listAllSuggestedSolrFields(solrIndexId: string): Promise<Array<SuggestedSolrField>> {
    return this.http
      .get(`${this.baseUrl}/${solrIndexId}/${this.solrFieldsApiPath}`)
      .toPromise()
      .then(res => res.json() as SuggestedSolrField[])
      .then(solrFieldNames =>
        solrFieldNames.reduce((r, s) => r.concat(s.name, '-' + s.name), [])
      )
  }

  lastDeploymentLogInfo(solrIndexId: string, targetSystem: string, raw: boolean = false): Promise<DeploymentLogInfo> {
    const reqPrms = {
      solrIndexId: solrIndexId,
      targetSystem: targetSystem
    }
    if (raw) {
      reqPrms['raw'] = true
    }
    return this.http
      .get(`${this.baseUrl}/log/deployment-info`, { params: reqPrms })
      .toPromise()
      .then(res =>  res.json() as DeploymentLogInfo)
  }
}
