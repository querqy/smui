import { Injectable } from '@angular/core';
import {HttpClient} from '@angular/common/http';

import {InputTag, ListItem} from '../models';

@Injectable({
  providedIn: 'root'
})
export class TagsService {

  private readonly baseUrl = 'api/v1';
  private readonly inputTagsApiPath: string = 'inputTags';

  constructor(private http: HttpClient) { }

  listAllInputTags(): Promise<Array<InputTag>> {
    return this.http
      .get<InputTag[]>(`${this.baseUrl}/${this.inputTagsApiPath}`)
      .toPromise();
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
