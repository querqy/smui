export class RulesReportItem {
  rule: string;
  isActive: boolean;
  input: string;
}

export class RulesReport {
  items: Array<RulesReportItem>;
}
