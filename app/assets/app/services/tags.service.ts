import { Injectable } from '@angular/core';
import { Http } from '@angular/http';
import 'rxjs/add/operator/toPromise';

import {InputTag, ListItem} from '../models/index';

@Injectable()
export class TagsService {

  private readonly baseUrl = 'api/v1';
  private readonly inputTagsApiPath: string = 'inputTags';

  constructor(private http: Http) { }

  listAllInputTags(): Promise<Array<InputTag>> {
    return this.http
      .get(`${this.baseUrl}/${this.inputTagsApiPath}`)
      .toPromise()
      .then(res => res.json() as InputTag[])
  }

  getAllTagsFromListItems(listItems: ListItem[]): InputTag[] {
    const tags = new Map<string, InputTag>();
    for (const i of listItems) {
      for (const t of i.tags) {
        tags.set(t.displayValue, t);
      }
    }

    return Array.from(tags.values()).sort((a, b) => a.displayValue.localeCompare(b.displayValue));
  }

}
