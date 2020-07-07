import { Injectable } from '@angular/core';
import { Http } from '@angular/http';
import 'rxjs/add/operator/toPromise';

import { ListItem } from '../models/index';

@Injectable()
export class ListItemsService {
  private readonly baseUrl = 'api/v1';
  private readonly listItemsPath: string = 'rules-and-spellings';

  constructor(public http: Http) { }

  getAllItemsForInputList(solrIndexId: string): Promise<Array<ListItem>> {
    return this.http
      .get(`${this.baseUrl}/${solrIndexId}/${this.listItemsPath}`)
      .toPromise()
      .then(res => res.json() as ListItem[])
  }
}
