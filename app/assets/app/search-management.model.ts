export class SolrIndex {
  id: number;
  name: string;
  description: string;
}

export class SynonymRule {
  id: number;
  synonymType: number;
  term: string;
}

export class UpDownRule {
  id: number;
  upDownType?: number;
  boostMalusValue?: number;
  term: string;
  upDownDropdownDefinitionMapping?: number;
  suggestedSolrFieldName?: string;
}

// TODO consider other persistence solution (e.g. REST)
export const upDownDropdownDefinitionMappings = [
  { displayName: 'UP(+++++)', upDownType: 0, boostMalusValue: 500 },
  { displayName: 'UP(++++)', upDownType: 0, boostMalusValue: 100 },
  { displayName: 'UP(+++)', upDownType: 0, boostMalusValue: 50 },
  { displayName: 'UP(++)', upDownType: 0, boostMalusValue: 10 },
  { displayName: 'UP(+)', upDownType: 0, boostMalusValue: 5 },
  { displayName: 'DOWN(-)', upDownType: 1, boostMalusValue: 5 },
  { displayName: 'DOWN(--)', upDownType: 1, boostMalusValue: 10 },
  { displayName: 'DOWN(---)', upDownType: 1, boostMalusValue: 50 },
  { displayName: 'DOWN(----)', upDownType: 1, boostMalusValue: 100 },
  { displayName: 'DOWN(-----)', upDownType: 1, boostMalusValue: 500 }
];

export class FilterRule {
  id: number;
  term: string;
  suggestedSolrFieldName?: string;
}

export class DeleteRule {
  id: number;
  term: string;
}

export class SearchInput {
  id: number;
  term: string;
  synonymRules: Array<SynonymRule>;
  upDownRules: Array<UpDownRule>;
  filterRules: Array<FilterRule>;
  deleteRules: Array<DeleteRule>;
}

export class SuggestedSolrField {
  id: number;
  name: string;
}
