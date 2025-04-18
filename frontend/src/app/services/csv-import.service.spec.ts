import { CSVImportService } from './csv-import.service';
import {RuleManagementService} from './rule-management.service'

describe('CSVImportService', () => {
  let csvImportService: CSVImportService;

  it('saves rule items', (done) => {
    const csv = "term,synonym,comment" as unknown as File;

    csvImportService.import(csv, 'indexId', () => {}).then(x => {
      expect(addRuleItem).toHaveBeenCalledOnceWith('indexId', 'term', []);

      const searchInput = updateSearchInput.calls.first().args[0];
      expect(searchInput.id).toEqual('123');
      expect(searchInput.term).toEqual('term');
      expect(searchInput.synonymRules.length).toEqual(1);
      expect(searchInput.synonymRules[0].term).toEqual('synonym');
      expect(searchInput.synonymRules[0].isActive).toEqual(true);
      expect(searchInput.synonymRules[0].synonymType).toEqual(0);
      expect(searchInput.isActive).toEqual(true);
      expect(searchInput.comment).toEqual('comment');
      done();
    });
  });

  it('calls progress for each saved rule item', (done) => {
    const ruleManagementService = {
      addNewRuleItem() {},
      updateSearchInput() {}
    } as unknown as RuleManagementService;
    const ctx = {progress: (percentage: number) => {}}
    const progress = spyOn(ctx, 'progress');
    const addRuleItem = spyOn(
      ruleManagementService, 'addNewRuleItem'
    ).and.returnValue(
      Promise.resolve({returnId: '123', message: '', result: ''})
    );
    const updateSearchInput = spyOn(
      ruleManagementService, 'updateSearchInput'
    ).and.returnValue(
      Promise.resolve({result: '', message: '', returnId: ''})
    );
    csvImportService = new CSVImportService(ruleManagementService);
    const csv = "term,synonym,comment\nterm2,synonym2,comment2" as unknown as File;

    csvImportService.import(csv, 'indexId', ctx.progress).then(x => {
      expect(progress).toHaveBeenCalledTimes(2);
      expect(progress).toHaveBeenCalledWith(50);
      expect(progress).toHaveBeenCalledWith(100);
      done();
    });
  });

});
