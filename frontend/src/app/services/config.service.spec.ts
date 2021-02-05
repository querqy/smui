import { Type } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController
} from '@angular/common/http/testing';

import { ConfigService } from './config.service';

describe('ConfigService', () => {
  let configService: ConfigService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ConfigService]
    });
    configService = TestBed.inject(ConfigService);
    httpMock = TestBed.inject(HttpTestingController as Type<
      HttpTestingController
    >);
  });

  it('should be created', () => {
    expect(configService).toBeTruthy();
  });

  it('should get the latest smui version', () => {
    const testVersionInfo = {
      latestMarketStandard: '3.11.15',
      current: '3.12.0',
      infoType: 'INFO',
      msgHtml: 'SMUI is up-to-date!'
    };

    configService.getLatestVersionInfo().then(() => {
      expect(configService.versionInfo).toBe(testVersionInfo);
    });

    httpMock.expectOne({}).flush(testVersionInfo);
  });
});
