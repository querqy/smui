import { Injectable } from '@angular/core';
import { Http } from '@angular/http';
import 'rxjs/add/operator/toPromise';

import { RulesReport } from '../models/index';

@Injectable()
export class ReportService {
  private readonly baseUrl = 'api/v1';

  constructor(public http: Http) { }

  getReport(solrIndexId: string, reportType: string): Promise<RulesReport> {
    return this.http
      .get(this.baseUrl + '/report/' + reportType + '/' + solrIndexId)
      .toPromise()
      .then(res => {
        return res.json() as RulesReport;
      })
  }
}
