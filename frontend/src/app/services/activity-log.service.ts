import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { ActivityLog } from '../models';

@Injectable({
  providedIn: 'root'
})
export class ActivityLogService {
  private readonly baseUrl = 'api/v1';

  constructor(private http: HttpClient) { }

  getActivityLog(inputId: string): Promise<ActivityLog> {
    return this.http
      .get<ActivityLog>(this.baseUrl + '/log/rule-activity-log?inputId=' + inputId)
      .toPromise();
  }
}
