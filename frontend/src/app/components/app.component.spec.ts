import {
  ComponentFixture,
  fakeAsync,
  TestBed,
  tick
} from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { AppComponent } from './app.component';
import { ConfigService, FeatureToggleService, SolrService } from '../services';
import Spy = jasmine.Spy;

describe('AppComponent', () => {
  let appComponent: AppComponent;
  let fixture: ComponentFixture<AppComponent>;

  let solrService: SolrService;
  let featureToggleService: FeatureToggleService;
  let configService: ConfigService;

  beforeEach(async () => {
    solrService = jasmine.createSpyObj('SolrService', ['listAllSolrIndices']);
    featureToggleService = jasmine.createSpyObj('FeatureToggleService', [
      'getFeatureToggles'
    ]);
    configService = jasmine.createSpyObj('ConfigService', [
      'getLatestVersionInfo', 'getTargetEnvironment'
    ]);

    await TestBed.configureTestingModule({
      imports: [RouterTestingModule, HttpClientTestingModule],
      declarations: [AppComponent],
      providers: [
        { provide: SolrService, useValue: solrService },
        { provide: FeatureToggleService, useValue: featureToggleService },
        { provide: ConfigService, useValue: configService }
      ]
    }).compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(AppComponent);
    appComponent = fixture.componentInstance;
  });

  it(`should initialize the application'`, fakeAsync(() => {
    (solrService.listAllSolrIndices as Spy).and.resolveTo();
    (featureToggleService.getFeatureToggles as Spy).and.resolveTo();
    (configService.getLatestVersionInfo as Spy).and.resolveTo();
    (configService.getTargetEnvironment as Spy).and.resolveTo();

    expect(appComponent.isInitialized).toBeFalse();

    appComponent.ngOnInit();
    tick();

    expect(appComponent).toBeTruthy();
    expect(appComponent.isInitialized).toBeTrue();
    expect(appComponent.errors).toEqual([]);
  }));

  it(`should show an error if the Solr Indices could not be fetched`, fakeAsync(() => {
    (solrService.listAllSolrIndices as Spy).and.rejectWith();
    (featureToggleService.getFeatureToggles as Spy).and.resolveTo();
    (configService.getLatestVersionInfo as Spy).and.resolveTo();
    (configService.getTargetEnvironment as Spy).and.resolveTo();

    appComponent.ngOnInit();
    tick();

    expect(appComponent).toBeTruthy();
    expect(appComponent.isInitialized).toBeFalse();
    expect(appComponent.errors).toEqual([
      'Could not fetch Solr configuration from back-end'
    ]);
  }));

  it(`should show an error if the feature toggles could not be fetched`, fakeAsync(() => {
    (solrService.listAllSolrIndices as Spy).and.resolveTo();
    (featureToggleService.getFeatureToggles as Spy).and.rejectWith();
    (configService.getLatestVersionInfo as Spy).and.resolveTo();
    (configService.getTargetEnvironment as Spy).and.resolveTo();

    appComponent.ngOnInit();
    tick();

    expect(appComponent).toBeTruthy();
    expect(appComponent.isInitialized).toBeFalse();
    expect(appComponent.errors).toEqual([
      'Could not fetch app configuration from back-end'
    ]);
  }));

  it(`should show an error if the version info could not be fetched`, fakeAsync(() => {
    (solrService.listAllSolrIndices as Spy).and.resolveTo();
    (featureToggleService.getFeatureToggles as Spy).and.resolveTo();
    (configService.getLatestVersionInfo as Spy).and.rejectWith();
    (configService.getTargetEnvironment as Spy).and.resolveTo();

    appComponent.ngOnInit();
    tick();

    expect(appComponent).toBeTruthy();
    expect(appComponent.isInitialized).toBeFalse();
    expect(appComponent.errors).toEqual([
      'Could not fetch version info from back-end'
    ]);
  }));

  it(`should show an error if the target environment could not be fetched`, fakeAsync(() => {
    (solrService.listAllSolrIndices as Spy).and.resolveTo();
    (featureToggleService.getFeatureToggles as Spy).and.resolveTo();
    (configService.getLatestVersionInfo as Spy).and.resolveTo();
    (configService.getTargetEnvironment as Spy).and.rejectWith();

    appComponent.ngOnInit();
    tick();

    expect(appComponent).toBeTruthy();
    expect(appComponent.isInitialized).toBeFalse();
    expect(appComponent.errors).toEqual([
      'Could not fetch target environment from back-end'
    ]);
  }));

  it(`should render a error message if the app count not be initiated`, fakeAsync(() => {
    (solrService.listAllSolrIndices as Spy).and.resolveTo();
    (featureToggleService.getFeatureToggles as Spy).and.resolveTo();
    (configService.getLatestVersionInfo as Spy).and.rejectWith();
    (configService.getTargetEnvironment as Spy).and.resolveTo();

    appComponent.ngOnInit();
    tick();

    fixture.detectChanges();
    expect(
      fixture.nativeElement.querySelector('ngb-alert').textContent.trim()
    ).toEqual('Error! Could not fetch version info from back-end');
  }));
});
