import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { RulesReport, ActivityReport } from '../models';

@Injectable({
  providedIn: 'root'
})
export class ReportService {
  private readonly baseUrl = 'api/v1';

  constructor(private http: HttpClient) { }

  // TODO convert backend date to readable format: yyyy-MM-dd HH:mm:ss
  getRulesReport(solrIndexId: string): Promise<RulesReport> {
    return this.http
      .get<RulesReport>(this.baseUrl + '/report/rules-report/' + solrIndexId)
      .toPromise();
  }

  getActivityReport(solrIndexId: string, dateFrom?: string, dateTo?: string): Promise<ActivityReport> {
    const options = {
      params : {
        ...(dateFrom && {dateFrom}),
        ...(dateTo && {dateTo})
      }
    };
    return this.http
      .get<ActivityReport>(this.baseUrl + '/report/activity-report/' + solrIndexId, options)
      .toPromise();
  }
}
