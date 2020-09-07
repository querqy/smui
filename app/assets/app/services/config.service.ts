import { Injectable } from '@angular/core'
import { Http } from '@angular/http'
import 'rxjs/add/operator/toPromise'

import { SmuiVersionInfo } from '../models/index'

@Injectable()
export class ConfigService {
  private readonly baseUrl = 'api/v1'

  constructor(public http: Http) { }

  getLatestVersionInfo(): Promise<SmuiVersionInfo> {
    return this.http
      .get(this.baseUrl + '/version/latest-info')
      .toPromise()
      .then(res => {
        return res.json() as SmuiVersionInfo
      })
  }
}
