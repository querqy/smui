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
}
