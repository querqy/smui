import { Injectable, SimpleChanges } from '@angular/core';

@Injectable()
export class CommonsService {

  generateUUID(): string {
    /* eslint-disable */
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
      const r = Math.random() * 16 | 0, v = c == 'x' ? r : (r & 0x3 | 0x8);
      return v.toString(16);
    });
    /* eslint-enable */
  }

  isDirty(obj, origObj: string): boolean {
    return obj ? JSON.stringify(obj) !== origObj : false;
  }

  hasChanged(changes: SimpleChanges, field: string): boolean {
    return changes[field] && changes[field].previousValue !== changes[field].currentValue;
  }

  removeQuotes(term: string): string {
    return term.replace(/['"]+/g, '');
  }

}
