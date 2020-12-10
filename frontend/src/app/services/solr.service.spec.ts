import { TestBed } from '@angular/core/testing';

import { SolrService } from './solr.service';

describe('SolrService', () => {
  let service: SolrService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(SolrService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
