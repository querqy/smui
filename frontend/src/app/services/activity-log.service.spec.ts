import { TestBed } from '@angular/core/testing';

import { ActivityLogService } from './activity-log.service';

describe('ActivityLogService', () => {
  let service: ActivityLogService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ActivityLogService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
