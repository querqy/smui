// angular modules
import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { AppRoutingModule } from './app-routing.module';
import { FormsModule } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { ToasterModule } from 'angular2-toaster';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { AngularMultiSelectModule } from 'angular2-multiselect-dropdown';
import { httpInterceptorProviders } from './interceptors';

// services
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
  CommonsService,
  PreviewLinkService
} from './services';

// // components
import { AppComponent } from './components/app.component';
import { ModalComponent, ModalConfirmComponent, ModalCopyComponent } from './components/modal';
import { HeaderNavComponent } from './components/header-nav';
import { SearchManagementComponent } from './components/search-management';
import {
  ReportComponent,
  ReportSettingsBarComponent
} from './components/report';
import {
  AdminComponent,
  RulesCollectionListComponent,
  RulesCollectionCreateComponent
} from './components/admin';
import {
  SuggestedFieldsComponent,
  SuggestedFieldsCreateComponent,
  SuggestedFieldsListComponent
} from './components/admin/suggested-fields';
import {
  ButtonRowComponent,
  CardComponent,
  CommentComponent,
  ErrorComponent,
  DetailHeaderComponent,
  InputRowContainerComponent,
  InputRowComponent,
  SpellingsComponent,
  RuleManagementComponent,
  ActivityLogComponent,
  PreviewLinkComponent
} from './components/details';
import {
  RulesListComponent,
  RulesSearchComponent
} from './components/rules-panel';

@NgModule({
  imports: [
    BrowserModule,
    FormsModule,
    HttpClientModule,
    AppRoutingModule,
    ToasterModule,
    BrowserAnimationsModule,
    AngularMultiSelectModule,
    NgbModule
  ],
  declarations: [
    AppComponent,
    ModalComponent,
    ModalConfirmComponent,
    ModalCopyComponent,
    HeaderNavComponent,
    ButtonRowComponent,
    CardComponent,
    CommentComponent,
    ErrorComponent,
    DetailHeaderComponent,
    InputRowContainerComponent,
    InputRowComponent,
    SpellingsComponent,
    RuleManagementComponent,
    RulesListComponent,
    RulesSearchComponent,
    ActivityLogComponent,
    SearchManagementComponent,
    ReportSettingsBarComponent,
    ReportComponent,
    AdminComponent,
    RulesCollectionListComponent,
    RulesCollectionCreateComponent,
    SuggestedFieldsComponent,
    SuggestedFieldsCreateComponent,
    SuggestedFieldsListComponent,
    PreviewLinkComponent
  ],
  providers: [
    CommonsService,
    FeatureToggleService,
    ListItemsService,
    RuleManagementService,
    SpellingsService,
    SolrService,
    TagsService,
    ActivityLogService,
    ReportService,
    ConfigService,
    PreviewLinkService,
    httpInterceptorProviders
  ],
  bootstrap: [AppComponent]
})
export class AppModule {}
