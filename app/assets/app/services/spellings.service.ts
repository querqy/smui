import { Injectable } from '@angular/core';
import { Headers, Http } from '@angular/http';
import 'rxjs/add/operator/toPromise';

import { ApiResult, CanonicalSpelling } from '../models/index';

@Injectable()
export class SpellingsService {
  private readonly baseUrl = 'api/v1';
  private readonly spellingsApiPath: string = 'spelling';

  private readonly jsonHeader = new Headers({'Content-Type': 'application/json'});

  constructor(public http: Http) { }

  getDetailedSpelling(spellingId: string): Promise<CanonicalSpelling> {
    return this.http
      .get(`${this.baseUrl}/${this.spellingsApiPath}/${spellingId}`)
      .toPromise()
      .then(res => res.json() as CanonicalSpelling)
  }

  addNewSpelling(solrIndexId: string, term: string): Promise<string> {
    const headers = { headers: this.jsonHeader };
    const body = JSON.stringify( { term });

    return this.http
      .put(`${this.baseUrl}/${solrIndexId}/${this.spellingsApiPath}`, body, headers)
      .toPromise()
      .then(res => res.json().returnId)
  }

  updateSpellingItem(solrIndexId: string, canonicalSpelling: CanonicalSpelling): Promise<string> {
    const headers = { headers: this.jsonHeader };
    const body = JSON.stringify(canonicalSpelling);

    return this.http
      .post(`${this.baseUrl}/${solrIndexId}/${this.spellingsApiPath}/${canonicalSpelling.id}`, body, headers)
      .toPromise()
      .then(res => res.json().returnId)
  }

  deleteSpelling(spellingId: string): Promise<ApiResult> {
    return this.http
      .delete(`${this.baseUrl}/${this.spellingsApiPath}/${spellingId}`)
      .toPromise()
      .then(res => res.json() as ApiResult)
  }
}
