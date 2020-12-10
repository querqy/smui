import { TestBed } from '@angular/core/testing';

import { ListItemsService } from './list-items.service';

describe('ListItemsService', () => {
  let service: ListItemsService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ListItemsService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
