export class RulesReportItem {
  term: string
  details: string
  isActive: boolean
  modified: string
  inputTerm: string
  inputModified: string
}

export class RulesReport {
  items: Array<RulesReportItem>
}
