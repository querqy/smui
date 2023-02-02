export class AlternativeSpelling {
  id: string;
  canonicalSpellingId: string;
  term: string;
  isActive: boolean;
}

export class CanonicalSpelling {
  id: string;
  solrIndexId: string;
  term: string;
  isActive: boolean;
  alternativeSpellings: Array<AlternativeSpelling>;
  comment: string;
}

export class AssociatedSpelling {
  id: string;
  term: string;
  exists: boolean;
  alternatives: Array<string>;

  constructor(id: string, term: string, exists: boolean, alternatives: Array<string>) {
    this.id = id;
    this.term = term;
    this.exists = exists;
    this.alternatives = alternatives
  }
}
