import { Injectable } from '@angular/core'
import { HttpClient } from '@angular/common/http'

import {
  SmuiVersionInfo,
  TargetEnvironmentInstance
} from '../models'

@Injectable({
  providedIn: 'root'
})
export class ConfigService {
  versionInfo?: SmuiVersionInfo
  targetEnvironment?: TargetEnvironmentInstance[]
  private readonly baseUrl = 'api/v1'

  constructor(private http: HttpClient) { }

  getLatestVersionInfo(): Promise<void> {
    return this.http
      .get<SmuiVersionInfo>(this.baseUrl + '/version/latest-info')
      .toPromise()
      .then(versionInfo => {
        this.versionInfo = versionInfo;
      });
  }

  getTargetEnvironment(): Promise<void> {
    return this.http
      .get<TargetEnvironmentInstance[]>(this.baseUrl + '/config/target-environment')
      .toPromise()
      .then(returnTargetEnv => {
        this.targetEnvironment = returnTargetEnv
      })
  }

}
