// TODO app-routing is currently not used, and just present for future extension

import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import {RuleManagementComponent} from './components/details/index';

// import { DashboardComponent } from './dashboard.component';
// import { HeroesComponent } from './heroes.component';

const routes: Routes = [
//  { path: '', redirectTo: 'detail/4711', pathMatch: 'full' },
//  { path: 'dashboard', component: DashboardComponent },
  { path: 'detail/:id', component: RuleManagementComponent },
//  { path: 'heroes', component: HeroesComponent }
];

@NgModule({
  imports: [
    RouterModule.forRoot(routes)
  ],
  exports: [
    RouterModule
  ]
})
export class AppRoutingModule { }
