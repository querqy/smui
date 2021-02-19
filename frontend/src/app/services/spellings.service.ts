import { Injectable } from '@angular/core';
import {HttpClient, HttpHeaders} from '@angular/common/http';

import { ApiResult, CanonicalSpelling } from '../models';

const httpOptions = {
  headers: new HttpHeaders({
    'Content-Type':  'application/json'
  })
};

@Injectable({
  providedIn: 'root'
})
export class SpellingsService {
  private readonly baseUrl = 'api/v1';
  private readonly spellingsApiPath: string = 'spelling';

  constructor(private http: HttpClient) { }

  getDetailedSpelling(spellingId: string): Promise<CanonicalSpelling> {
    return this.http
      .get<CanonicalSpelling>(`${this.baseUrl}/${this.spellingsApiPath}/${spellingId}`)
      .toPromise();
  }

  addNewSpelling(solrIndexId: string, term?: string): Promise<ApiResult> {
    const body = JSON.stringify( { term: term || '' });

    return this.http
      .put<ApiResult>(`${this.baseUrl}/${solrIndexId}/${this.spellingsApiPath}`, body, httpOptions)
      .toPromise();
  }

  updateSpellingItem(solrIndexId: string, canonicalSpelling: CanonicalSpelling): Promise<ApiResult> {
    const body = JSON.stringify(canonicalSpelling);

    return this.http
      .post<ApiResult>(`${this.baseUrl}/${solrIndexId}/${this.spellingsApiPath}/${canonicalSpelling.id}`, body, httpOptions)
      .toPromise();
  }

  deleteSpelling(spellingId: string): Promise<ApiResult> {
    return this.http
      .delete<ApiResult>(`${this.baseUrl}/${this.spellingsApiPath}/${spellingId}`)
      .toPromise();
  }
}
