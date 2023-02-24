import { InputTag } from './tags.model';

export class SynonymRule {
  id: string;
  synonymType: number;
  term: string;
  isActive: boolean;
}

export class UpDownRule {
  id: string;
  upDownType?: number;
  boostMalusValue?: number;
  term: string;
  upDownDropdownDefinitionMapping?: number;
  suggestedSolrFieldName?: string;
  isActive: boolean;
}

export class FilterRule {
  id: string;
  term: string;
  suggestedSolrFieldName?: string;
  isActive: boolean;
}

export class DeleteRule {
  id: string;
  term: string;
  isActive: boolean;
}

export class RedirectRule {
  id: string;
  target: string;
  isActive: boolean;
}

export class SearchInput {
  id: string;
  term: string;
  synonymRules: Array<SynonymRule>;
  upDownRules: Array<UpDownRule>;
  filterRules: Array<FilterRule>;
  deleteRules: Array<DeleteRule>;
  redirectRules: Array<RedirectRule>;
  tags: Array<InputTag>;
  isActive: boolean;
  comment: string;

  // Dealing with querqy exact matching syntax for input terms

  public static isTermLeftExact(term: string): boolean {
    const trimmedTerm = term.trim()
    return trimmedTerm.startsWith('"')
  }

  public static isTermRightExact(term: string): boolean {
    const trimmedTerm = term.trim()
    return trimmedTerm.endsWith('"')
  }

  public static isTermExact(term: string): boolean {
    return (
      SearchInput.isTermLeftExact(term)
      && SearchInput.isTermRightExact(term)
    )
  }

  public static stripExactMatchingSyntax(rawInputTerm: string): string {
    const trimmedTerm = rawInputTerm.trim()
    const startIdx = trimmedTerm.startsWith('"') ? 1 : 0
    const endIdx = trimmedTerm.endsWith('"') ? (trimmedTerm.length-1) : trimmedTerm.length
//    console.log(
//      'In SearchInput :: stripExactMatchingSyntax :: trimmedTerm = "' + trimmedTerm
//      + '" startIdx = ' + startIdx
//      + ' endIdx = ' + endIdx
//    )
    return trimmedTerm.substring(startIdx, endIdx)
  }

}
