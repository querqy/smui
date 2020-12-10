import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

import { ListItem } from '../models';

@Injectable({
  providedIn: 'root'
})
export class ListItemsService {
  private readonly baseUrl = 'api/v1';
  private readonly listItemsPath: string = 'rules-and-spellings';

  constructor(private http: HttpClient) { }

  getAllItemsForInputList(solrIndexId: string): Promise<Array<ListItem>> {
    return this.http
      .get<ListItem[]>(`${this.baseUrl}/${solrIndexId}/${this.listItemsPath}`)
      .toPromise();
  }
}
