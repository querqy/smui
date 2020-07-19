import { Injectable } from '@angular/core';
import { Http } from '@angular/http';
import 'rxjs/add/operator/toPromise';

import { ActivityLogEntry } from '../models/index';

@Injectable()
export class ActivityLogService {
  private readonly baseUrl = 'api/v1';

  constructor(public http: Http) { }

  getInputRuleActivityLog(inputId: string): Promise<Array<ActivityLogEntry>> {
    return this.http
      .get(this.baseUrl + '/log/rule-activity-log?inputId=' + inputId)
      .toPromise()
      .then(res => {
        return res.json() as ActivityLogEntry[];
      })
  }
}
