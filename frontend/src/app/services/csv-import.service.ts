import {parse} from 'papaparse';
import { Injectable } from '@angular/core';
import {ApiResult} from '../models';
import {rowsToSearchInputs} from '../lib/csv';
import {RuleManagementService, ModalService} from '.';

const fileImportModal = 'file-import';
@Injectable({
  providedIn: 'root'
})
export class CSVImportService {
  constructor(private ruleManagementService: RuleManagementService, private modalService: ModalService) {}

  import(file: File, indexId: string): Promise<ApiResult | null> {
    return new Promise((resolve, reject) => {
      parse(file, {
        complete: (results: {data: string[][]}) => {
          const searchInputs = rowsToSearchInputs(results.data);
          const ruleCreations: Promise<ApiResult | null> = searchInputs
            .reduce((chain: Promise<ApiResult | null>, searchInput): Promise<null | ApiResult> => {
              return chain
                .then(() => {
                  return this.ruleManagementService.addNewRuleItem(indexId, searchInput.term, [])
                    .then(inputId => {
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
