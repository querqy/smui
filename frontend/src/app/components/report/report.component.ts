import { Component, OnInit, Input, ViewChild } from '@angular/core';
import { ToasterService } from 'angular2-toaster';

import { ReportSettingsBarComponent } from './report-settingsbar/report-settingsbar.component';
import { ReportService, SolrService } from '../../services';
import {ActivityReport, RulesReport, RulesUsageReport} from '../../models';
import {DataTableDirective} from "angular-datatables";

@Component({
  selector: 'app-smui-report',
  styleUrls: ['./report.component.css'],
  templateUrl: './report.component.html'
})
export class ReportComponent implements OnInit {
  @Input() currentSolrIndexId?: string;

  @ViewChild('smuiReportSettingsBar')
  settingsBarComponent: ReportSettingsBarComponent;

  // for now: datatable used for rule usage report
  @ViewChild(DataTableDirective)
  datatableElement: DataTableDirective;

  generateBtnDisabled = false;
  activityReport?: ActivityReport;
  rulesReport?: RulesReport;
  rulesUsageReport?: RulesUsageReport;

  // view/filter flags for the rule usage report
  ruleUsageReportShowUsed: boolean = true;
  ruleUsageReportShowUnused: boolean = true;
  ruleUsageReportShowFound: boolean = true;
  ruleUsageReportShowNotFound: boolean = true;

  dtOptions: DataTables.Settings = {};

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
      } else if (configReport === 'rules-usage-report') {
        this.getRulesUsageReport(this.currentSolrIndexId);
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

  private getRulesUsageReport(solrIndexId: string) {
    this.reportService
      .getRulesUsageReport(solrIndexId)
      .then(retReport => {
        console.log(':: getRulesUsageReport :: retReport received');
        this.rulesUsageReport = retReport;
        this.dtOptions = {
          columnDefs: [
            { orderData: [0, 1, 2], targets: 0 },
            { orderData: [1, 0, 2], targets: 1 },
            { orderData: [2, 0, 1], targets: 2 }
          ],
          pageLength: 25
        }
        $.fn['dataTable'].ext.search.push(this.filterRuleUsageReportByFlags);
        console.log(`Length of search ext after push: ${$.fn['dataTable'].ext.search.length}`)
        this.generateBtnDisabled = false;
      })
      .catch(error => this.showErrorMsg(error));
  }

  filterRuleUsageReportByFlags = (settings: string, data: any[], index: number) => {
    // filtering based on data tables String values
    const isUsed = parseInt(data[2]) > 0;
    const isFound: boolean = !data[0].trim().startsWith("not found");
    return (
      ((isUsed && this.ruleUsageReportShowUsed) || (!isUsed && this.ruleUsageReportShowUnused)) &&
      ((isFound && this.ruleUsageReportShowFound) || (!isFound && this.ruleUsageReportShowNotFound))
    );
  }

  ruleUsageReportFilter(state: 'used' | 'unused' | 'found' | 'notfound') {
    if (state === "found") {
      this.ruleUsageReportShowFound = !this.ruleUsageReportShowFound;
      if (!this.ruleUsageReportShowFound && !this.ruleUsageReportShowNotFound) {
        this.ruleUsageReportShowNotFound = true;
      }
    } else if (state === "notfound") {
      this.ruleUsageReportShowNotFound = !this.ruleUsageReportShowNotFound;
      if (!this.ruleUsageReportShowFound && !this.ruleUsageReportShowNotFound) {
        this.ruleUsageReportShowFound = true;
      }
    } else if (state == "used") {
      this.ruleUsageReportShowUsed = !this.ruleUsageReportShowUsed;
      if (!this.ruleUsageReportShowUsed && !this.ruleUsageReportShowUnused) {
        this.ruleUsageReportShowUnused = true;
      }
    } else if (state == "unused") {
      this.ruleUsageReportShowUnused = !this.ruleUsageReportShowUnused;
      if (!this.ruleUsageReportShowUsed && !this.ruleUsageReportShowUnused) {
        this.ruleUsageReportShowUsed = true;
      }
    }
    this.redrawRules();
  }

  redrawRules(): void {
    this.datatableElement.dtInstance.then((dtInstance: DataTables.Api) => {
      dtInstance.draw();
    });
  }

  private resetReports() {
    this.activityReport = undefined;
    this.rulesReport = undefined;
    this.rulesUsageReport = undefined;
    if ($.fn['dataTable']) {
      $.fn['dataTable'].ext.search.pop();
      console.log(`Length of search ext after pop: ${$.fn['dataTable'].ext.search.length}`)
    }
  }
}
