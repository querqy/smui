// angular modules
import { NgModule } from '@angular/core'
import { BrowserModule } from '@angular/platform-browser'
import { AppRoutingModule } from './app-routing.module'
import { FormsModule } from '@angular/forms'
import { HttpClientModule } from '@angular/common/http'
import { NgbModule } from '@ng-bootstrap/ng-bootstrap'
import { ToasterModule } from 'angular2-toaster'
import { BrowserAnimationsModule } from '@angular/platform-browser/animations'
// import { AngularMultiSelectModule } from 'angular2-multiselect-dropdown';
// import { Http, XHRBackend, RequestOptions } from '@angular/http';
// import { Router } from '@angular/router';

import {
  FeatureToggleService,
  ListItemsService,
  RuleManagementService,
  SpellingsService,
  SolrService,
  TagsService,
  ActivityLogService,
  ReportService,
  ConfigService,
  CommonsService
} from './services'

// interceptors
// import {
//   AuthInterceptor
// } from './interceptors';

// // components
import { AppComponent } from './components/app.component'

import { ModalComponent, ModalConfirmComponent } from './components/modal'

import { HeaderNavComponent } from './components/header-nav'

import { SearchManagementComponent } from './components/search-management'

import {
  ReportComponent,
  ReportSettingsBarComponent
} from './components/report'

import {
  ButtonRowComponent,
  CardComponent,
  CommentComponent,
  ErrorComponent,
  DetailHeaderComponent,
  InputRowContainerComponent,
  DetailInputRow,
  SpellingsComponent,
  RuleManagementComponent,
  ActivityLogComponent
} from './components/details'

import {
  RulesListComponent,
  RulesSearchComponent
} from './components/rules-panel'

// !!!! vllt sowas fürs intercepten
// import { NgModule } from '@angular/core';
// import { HTTP_INTERCEPTORS } from '@angular/common/http';
// import { HttpErrorInterceptor } from './path/http-error.interceptor';
//
// @NgModule({
//   ...
//     providers: [{
//   provide: HTTP_INTERCEPTORS,
//   useClass: HttpErrorInterceptor,
//   multi: true,
// }],
// ...
// })
// export class AppModule {}

@NgModule({
  imports: [
    BrowserModule,
    FormsModule,
    HttpClientModule,
    AppRoutingModule,
    ToasterModule,
    BrowserAnimationsModule,
    // AngularMultiSelectModule,
    NgbModule
  ],
  declarations: [
    AppComponent,
    ModalComponent,
    ModalConfirmComponent,
    HeaderNavComponent,
    ButtonRowComponent,
    CardComponent,
    CommentComponent,
    ErrorComponent,
    DetailHeaderComponent,
    InputRowContainerComponent,
    DetailInputRow,
    SpellingsComponent,
    RuleManagementComponent,
    RulesListComponent,
    RulesSearchComponent,
    ActivityLogComponent,
    SearchManagementComponent,
    ReportSettingsBarComponent,
    ReportComponent
  ],
  providers: [
    CommonsService,
    FeatureToggleService,
    ListItemsService,
    RuleManagementService,
    SpellingsService,
    SolrService,
    TagsService,
    // // {
    // //   provide: Http,
    // //   useFactory:
    // //     (xhrBackend: XHRBackend, requestOptions: RequestOptions, router: Router) =>
    // //       new HttpAuthInterceptor(xhrBackend, requestOptions, router),
    // //   deps: [XHRBackend, RequestOptions, Router]
    // // },
    ActivityLogService,
    ReportService,
    ConfigService
  ],
  bootstrap: [AppComponent]
})
export class AppModule {}
