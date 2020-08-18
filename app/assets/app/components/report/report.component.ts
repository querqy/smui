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
    }
  }

  public showSuccessMsg(msgText: string) {
    this.toasterService.pop('success', '', msgText);
  }

  public showErrorMsg(msgText: string) {
    this.toasterService.pop('error', '', msgText);
  }

  selectReport(report: string) {
    console.log('In ReportComponent :: selectReport :: report = ' + report)
  }

  generateReport(reportReq: any) {
    console.log('In ReportComponent :: selectReport :: reportReq = ' + JSON.stringify(reportReq))

    this.reportService.getActivityLog(this.currentSolrIndexId)
      .then(retReport => {
        console.log(':: retReport received')
        this.currentReport = retReport
      })
      .catch(error => this.showErrorMsg(error))
  }

}
