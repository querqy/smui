import {parse} from 'papaparse';
import { Injectable } from '@angular/core';
import {ApiResult} from '../models';
import {rowsToSearchInputs} from '../lib/csv';
import {RuleManagementService} from './rule-management.service'
import {ModalService} from './modal.service';

const fileImportModal = 'file-import';
@Injectable({
  providedIn: 'root'
})
export class CSVImportService {
  constructor(private ruleManagementService: RuleManagementService, private modalService: ModalService) {}

  import(file: File, indexId: string, progress: (percentage: number) => void): Promise<ApiResult | null> {
    return new Promise((resolve, reject) => {
      parse(file, {
        complete: (results: {data: string[][]}) => {
          const searchInputs = rowsToSearchInputs(results.data);
          let i = 1;
          const ruleCreations: Promise<ApiResult | null> = searchInputs
            .reduce((chain: Promise<ApiResult | null>, searchInput): Promise<null | ApiResult> => {
              return chain
                .then(() => {
                  return this.ruleManagementService.addNewRuleItem(indexId, searchInput.term, [])
                    .then(inputId => {
                      const onePercent = 100 / (searchInputs.length);

                      progress(onePercent * i);
                      i ++;
                      searchInput.id = inputId.returnId;
                      return this.ruleManagementService.updateSearchInput(searchInput)
                    });
                });
            }, Promise.resolve(null));
            resolve(ruleCreations);
        },
        error: (err: Error) => {
          reject(err);
        }
      });
    });
  }
}
