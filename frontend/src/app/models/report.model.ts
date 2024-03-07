export class RulesReportItem {
  inputId: string;
  term: string;
  details: string;
  isActive: boolean;
  modified: string;
  inputTerm: string;
  inputModified: string;
  inputTags: Array<string>;
}

export class RulesReport {
  items: Array<RulesReportItem>;
}

export class ActivityReportEntry {
  modificationTime: string;
  user?: string;
  inputTerm: string;
  entity: string;
  eventType: string;
  before?: string;
  after?: string;
}

export class ActivityReport {
  items: Array<ActivityReportEntry>;
}

export class RulesUsageReportEntry {
  searchInputId: string;
  searchInputTerm?: string;
  keywords: string;
  frequency: number;
}

export class RulesUsageReport {
  items: Array<RulesUsageReportEntry>;
}
