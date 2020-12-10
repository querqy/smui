import { TestBed } from '@angular/core/testing';

import { RuleManagementService } from './rule-management.service';

describe('RuleManagementService', () => {
  let service: RuleManagementService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(RuleManagementService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
