import { Component, OnInit, OnChanges, Input, SimpleChanges } from '@angular/core'
import { ToasterService } from 'angular2-toaster'

import { RulesReport } from '../../models/index';
import { ReportService } from '../../services/index'

@Component({
  selector: 'smui-report',
  templateUrl: './report.component.html',
  styleUrls: ['./report.component.css']
})
export class ReportComponent implements OnInit, OnChanges {

  @Input() currentSolrIndexId: string = null

  // TODO not nice, initialised twice (see ../report-settingsbar/report-settingsbar.component.ts)
  private currentReportType = 'rules-report'
  private currentReport = null

  constructor(
    private toasterService: ToasterService,
    private reportService: ReportService
  ) {}

  ngOnInit() {
    console.log('In ReportComponent :: ngOnInit')
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes.currentSolrIndexId) {
      console.log('In ReportComponent :: ngOnChanges :: currentSolrIndexId = ' + changes.currentSolrIndexId)
      this.currentReport = null
    }
  }

  public showSuccessMsg(msgText: string) {
    this.toasterService.pop('success', '', msgText);
  }

  public showErrorMsg(msgText: string) {
    this.toasterService.pop('error', '', msgText);
  }

  selectReport(reportType: string) {
    console.log('In ReportComponent :: selectReport :: reportType = ' + reportType)
    this.currentReportType = reportType
  }

  generateReport(reportReq: any) {
    console.log('In ReportComponent :: selectReport :: reportReq = ' + JSON.stringify(reportReq))

    this.reportService.getReport(this.currentSolrIndexId, this.currentReportType)
      .then(retReport => {
        console.log(':: retReport received')
        this.currentReport = retReport
      })
      .catch(error => this.showErrorMsg(error))
  }

}
