import {SynonymRule, SearchInput} from '../models';
import {randomUUID} from './uuid';

function makeActiveSynonymTerm(synonymTerm: string): SynonymRule {
  return {
    term: synonymTerm,
    isActive: true,
    synonymType: 0,
    id: randomUUID()
  };
}

export function rowsToSearchInputs(rows: string[][]): SearchInput[] {
  return rows
    .filter(row => row.length === 3)
    .reduce((searchInputs, row) => {
      console.log(row);
      const [term, synonymTerm, comment] = row;
      const searchInput = searchInputs.find(searchInput => searchInput.term === term);
      if (!searchInput) {
        searchInputs = searchInputs.concat({
          id: randomUUID(),
          term,
          synonymRules: [makeActiveSynonymTerm(synonymTerm)],
          isActive: true,
          redirectRules: [],
          deleteRules: [],
          filterRules: [],
          tags: [],
          upDownRules: [],
          comment,
        })
      } else {
        searchInput.synonymRules.push(makeActiveSynonymTerm(synonymTerm))
      }
      return searchInputs;
     }, [] as SearchInput[]);
}
