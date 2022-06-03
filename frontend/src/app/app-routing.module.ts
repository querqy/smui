import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { SearchManagementComponent } from './components/search-management';
import { ReportComponent } from './components/report';
import { AdminComponent } from './components/admin';
import { ImportComponent } from './components/import';
import { SuggestedFieldsComponent } from './components/admin/suggested-fields';

const routes: Routes = [
  { path: '', redirectTo: 'rules', pathMatch: 'full' },
  { path: 'rules', component: SearchManagementComponent },
  { path: 'report', component: ReportComponent },
  { path: 'admin', component: AdminComponent },
  { path: 'import', component: ImportComponent },
  { path: 'admin/suggested-fields/:solrIndexId', component: SuggestedFieldsComponent }
];

@NgModule({
  imports: [
    RouterModule.forRoot(routes, {useHash: true})
  ],
  exports: [
    RouterModule
  ]
})
export class AppRoutingModule { }
