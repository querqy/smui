import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map } from 'rxjs/operators';
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
      .get(`${this.baseUrl}/${solrIndexId}/${this.listItemsPath}`)
      .pipe(
        map((response: any) =>
          (response as []).map((item) => Object.assign(new ListItem, item))),
        map((listItems: ListItem[]) =>
          this.assignRuleUsageBuckets(listItems, 8)
        )
      ).toPromise();
  }

  assignRuleUsageBuckets(listItems: ListItem[], numBuckets: number): ListItem[] {
    if (listItems.length == 0) {
      return listItems;
    }
    // perform a min-max normalization of the log2 of the usage frequency and assign them to $numBuckets buckets
    const log2Values = listItems.map(listItem => Math.log2(listItem.usageFrequency || 1));
    const minLog2 = Math.min(...log2Values);
    const maxLog2 = Math.max(...log2Values);
    const originalRange = maxLog2 - minLog2;
    return listItems.map(listItem => {
      const log2Value = Math.log2(listItem.usageFrequency || 1);
      const bucket = Math.round(((log2Value - minLog2) / originalRange) * numBuckets);
      return Object.assign(listItem, {"usageFrequencyBucket": bucket})
    });
  }
}
