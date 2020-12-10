import { TestBed } from '@angular/core/testing';

import { CommonsService } from './commons.service';

describe('CommonsService', () => {
  let service: CommonsService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(CommonsService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
