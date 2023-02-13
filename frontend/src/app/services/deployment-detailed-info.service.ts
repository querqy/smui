import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { DeploymentDetailedInfo } from '../models';

@Injectable({
  providedIn: 'root'
})
export class DeploymentDetailedInfoService {
  private readonly baseUrl = 'api/v2';

  constructor(private http: HttpClient) { }

  get(
    solrIndexId: string
  ): Promise<DeploymentDetailedInfo[]> {
    const options = {
      params: {
        'solrIndexId': solrIndexId
      }
    };

    return this.http
      .get<DeploymentDetailedInfo[]>(this.baseUrl + '/log/deployment-info', options)
      .toPromise();
  }

}

  