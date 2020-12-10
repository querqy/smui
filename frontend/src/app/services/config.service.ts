import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { SmuiVersionInfo } from '../models';

@Injectable({
  providedIn: 'root'
})
export class ConfigService {
  private readonly baseUrl = 'api/v1';
  versionInfo?: SmuiVersionInfo;

  constructor(private http: HttpClient) { }

  getLatestVersionInfo(): Promise<void> {
    return this.http
      .get<SmuiVersionInfo>(this.baseUrl + '/version/latest-info')
      .toPromise()
      .then(versionInfo => {
        this.versionInfo = versionInfo;
      })
  }
}
