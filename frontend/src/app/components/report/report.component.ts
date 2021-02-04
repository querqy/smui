import { Component, OnInit, Input, ViewChild } from '@angular/core';
import { ToasterService } from 'angular2-toaster';

import { ReportSettingsBarComponent } from './report-settingsbar/report-settingsbar.component';
import { ReportService, SolrService } from '../../services';
import { ActivityReport, RulesReport } from '../../models';

@Component({
  selector: 'app-smui-report',
  templateUrl: './report.component.html'
})
export class ReportComponent implements OnInit {
  @Input() currentSolrIndexId?: string;

  @ViewChild('smuiReportSettingsBar')
  settingsBarComponent: ReportSettingsBarComponent;

  generateBtnDisabled = false;
  activityReport?: ActivityReport;
  rulesReport?: RulesReport;

  constructor(
    private toasterService: ToasterService,
    private reportService: ReportService,
    private solrService: SolrService
  ) {
    this.solrService.currentSolrIndexIdSubject.subscribe(value => {
      this.currentSolrIndexId = value;
      this.resetReports();
    });
  }

  ngOnInit() {
    console.log('In ReportComponent :: ngOnInit');
    this.currentSolrIndexId = this.solrService.currentSolrIndexId;
    if (this.currentSolrIndexId) {
      this.resetReports();
    }
  }

  showSuccessMsg(msgText: string) {
    this.toasterService.pop('success', '', msgText);
  }

  showErrorMsg(msgText: string) {
    this.toasterService.pop('error', '', msgText);
  }

  changeReport() {
    console.log('In ReportComponent :: changeReport');
    this.resetReports();
  }

  generateReport() {
    console.log('In ReportComponent :: generateReport');
    console.log(
      ':: settingsBarComponent.configReport = ' +
        this.settingsBarComponent.configReport
    );
    console.log(
      ':: settingsBarComponent.configDateFrom = ' +
        this.settingsBarComponent.configDateFrom
    );
    console.log(
      ':: settingsBarComponent.configDateTo = ' +
        this.settingsBarComponent.configDateTo
    );

    // reset currentReport (and disable button) for the time, the report is loading to avoid confusion
    this.resetReports();
    this.generateBtnDisabled = true;

    const { configReport } = this.settingsBarComponent;
    if (this.currentSolrIndexId) {
      if (configReport === 'rules-report') {
        this.getRulesReport(this.currentSolrIndexId);
      } else if (configReport === 'activity-report') {
        const { configDateFrom, configDateTo } = this.settingsBarComponent;
        this.getActivityReport(
          this.currentSolrIndexId,
          configDateFrom,
          configDateTo
        );
      }
    }
  }

  private getRulesReport(solrIndexId: string) {
    this.reportService
      .getRulesReport(solrIndexId)
      .then(retReport => {
        console.log(':: getRulesReport :: retReport received');
        this.generateBtnDisabled = false;
        this.rulesReport = retReport;
      })
      .catch(error => this.showErrorMsg(error));
  }

  private getActivityReport(solrIndexId: string, from?: string, to?: string) {
    this.reportService
      .getActivityReport(solrIndexId, from, to)
      .then(retReport => {
        console.log(':: getActivityReport :: retReport received');
        this.generateBtnDisabled = false;
        this.activityReport = retReport;
      })
      .catch(error => this.showErrorMsg(error));
  }

  private resetReports() {
    this.activityReport = undefined;
    this.rulesReport = undefined;
  }
}
