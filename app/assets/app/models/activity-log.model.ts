export class DiffSummary {
  entity: string;
  eventType: string;
  before?: string;
  after?: string;
}

export class ActivityLogEntry {
  formattedDateTime: string;
  userInfo?: string;
  diffSummary: Array<DiffSummary>;
}

export class ActivityLog {
  items: Array<ActivityLogEntry>;
}
