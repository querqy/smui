import {rowsToSearchInputs} from './csv';

describe('rowsToSearchInputs', () => {
  it('parses no inputs when there are no rows', () => {
    expect(rowsToSearchInputs([])).toHaveSize(0);
  });

  it('ignores rows with less than 3 columns', () => {
    expect(rowsToSearchInputs([['abc', '123']])).toHaveSize(0);
  });

  it('ignores rows with more than 3 columns', () => {
    expect(rowsToSearchInputs([['abc', '123', 'def', '456']])).toHaveSize(0);
  });

  it('parses rows with 3 columns', () => {
    const inputs = rowsToSearchInputs([
      ['term', 'synonym', 'comment']
    ]);
    expect(inputs).toHaveSize(1);
    expect(inputs[0].id).toBeInstanceOf(String);
    expect(inputs[0].term).toEqual('term');
    expect(inputs[0].redirectRules).toEqual([]);
    expect(inputs[0].filterRules).toEqual([]);
    expect(inputs[0].tags).toEqual([]);
    expect(inputs[0].upDownRules).toEqual([]);
    expect(inputs[0].synonymRules).toHaveSize(1);
    expect(inputs[0].synonymRules[0].term).toEqual('synonym');
    expect(inputs[0].synonymRules[0].isActive).toEqual(true);
    expect(inputs[0].synonymRules[0].synonymType).toEqual(0);
    expect(inputs[0].synonymRules[0].id).toBeInstanceOf(String);
  });

  it('groups rows with the same search term', () => {
    const inputs = rowsToSearchInputs([
      ['term', 'synonym', 'comment'],
      ['term', 'another synonym', 'comment'],
    ]);
    expect(inputs).toHaveSize(1);
    expect(inputs[0].synonymRules).toHaveSize(2);
  });

  it('creates one SearchInput per term', () => {
    const inputs = rowsToSearchInputs([
      ['term', 'synonym', 'comment'],
      ['term2', 'another synonym', 'comment'],
    ]);
    expect(inputs).toHaveSize(2);
  });
});
