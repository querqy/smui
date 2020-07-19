import { Injectable } from '@angular/core';
import { Http } from '@angular/http';
import 'rxjs/add/operator/toPromise';

import { ActivityLog } from '../models/index';

@Injectable()
export class ActivityLogService {
  private readonly baseUrl = 'api/v1';

  constructor(public http: Http) { }

  getActivityLog(inputId: string): Promise<ActivityLog> {
    return this.http
      .get(this.baseUrl + '/log/rule-activity-log?inputId=' + inputId)
      .toPromise()
      .then(res => {
        return res.json() as ActivityLog;
      })
  }
}
