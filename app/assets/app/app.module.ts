import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpModule } from '@angular/http';

import { NgbModule } from '@ng-bootstrap/ng-bootstrap';

// import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ToasterModule } from 'angular2-toaster';

import { AppComponent } from './app.component';
import { AppRoutingModule } from './app-routing.module';

import { SearchManagementService } from './search-management.service';
import { FeatureToggleService } from './feature-toggle.service';

import { SearchInputListComponent } from './search-input-list.component';
import { SearchInputDetailComponent } from './search-input-detail.component';

import { Http, XHRBackend, RequestOptions } from '@angular/http';
import { Router } from '@angular/router';
import { HttpAuthInterceptor } from './http-auth-interceptor'

@NgModule({
  imports: [
    BrowserModule,
    FormsModule,
    AppRoutingModule,
    HttpModule,
//     BrowserAnimationsModule,
    ToasterModule,
    NgbModule.forRoot()
  ],
  declarations: [
    AppComponent,
    SearchInputListComponent,
    SearchInputDetailComponent
  ],
  providers: [
    SearchManagementService,
    FeatureToggleService,
    {
      provide: Http,
      useFactory:
        (xhrBackend: XHRBackend, requestOptions: RequestOptions, router: Router) =>
          new HttpAuthInterceptor(xhrBackend, requestOptions, router),
      deps: [XHRBackend, RequestOptions, Router]
    }
  ],
  bootstrap: [
    AppComponent
  ]
})
export class AppModule { }
