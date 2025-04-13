import { Injectable, SimpleChanges } from '@angular/core';
import {randomUUID} from '../lib/uuid';

@Injectable()
export class CommonsService {

  generateUUID(): string {
    return randomUUID();
  }

  isDirty(obj: any, origObj: string): boolean {
    return obj ? JSON.stringify(obj) !== origObj : false;
  }

  hasChanged(changes: SimpleChanges, field: string): boolean {
    return changes[field] && changes[field].previousValue !== changes[field].currentValue;
  }

  removeQuotes(term: string): string {
    return term.replace(/['"]+/g, '');
  }

}
