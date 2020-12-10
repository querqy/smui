import { TestBed } from '@angular/core/testing';

import { SpellingsService } from './spellings.service';

describe('SpellingsService', () => {
  let service: SpellingsService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(SpellingsService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
