import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

import { SearchManagementComponent } from './components/search-management';
import { ReportComponent } from './components/report';

const routes: Routes = [
  { path: '', redirectTo: 'rules', pathMatch: 'full' },
  { path: 'rules', component: SearchManagementComponent },
  { path: 'report', component: ReportComponent }
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
