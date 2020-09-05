import { Component, OnInit, OnChanges, Input, SimpleChanges, ViewChild } from '@angular/core'
import { ToasterService } from 'angular2-toaster'

import { ReportSettingsBarComponent } from './report-settingsbar/report-settingsbar.component'

import { RulesReport } from '../../models/index';
import { ReportService } from '../../services/index'

@Component({
  selector: 'smui-report',
  templateUrl: './report.component.html',
  styleUrls: ['./report.component.css']
})
export class ReportComponent implements OnInit, OnChanges {

  @Input() currentSolrIndexId: string = null

  @ViewChild('smuiReportSettingsBar') settingsBarComponent: ReportSettingsBarComponent

  generateBtnDisabled = false
  currentReport = null

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

  changeReport() {
    console.log('In ReportComponent :: changeReport')
    this.currentReport = null
  }

  generateReport() {
    console.log('In ReportComponent :: generateReport')
    console.log(':: settingsBarComponent.configReport = ' + this.settingsBarComponent.configReport)
    console.log(':: settingsBarComponent.configDateFrom = ' + this.settingsBarComponent.configDateFrom)
    console.log(':: settingsBarComponent.configDateTo = ' + this.settingsBarComponent.configDateTo)

    // reset currentReport (and disable button) for the time, the report is loading to avoid confusion
    this.currentReport = null
    this.generateBtnDisabled = true

    if (this.settingsBarComponent.configReport === 'rules-report') {
      this.reportService.getRulesReport(this.currentSolrIndexId)
        .then(retReport => {
          console.log(':: getRulesReport :: retReport received')
          this.generateBtnDisabled = false
          this.currentReport = retReport
        })
        .catch(error => this.showErrorMsg(error))
    } else if (this.settingsBarComponent.configReport === 'activity-report') {
      this.reportService.getActivityReport(
        this.currentSolrIndexId,
        this.settingsBarComponent.configDateFrom,
        this.settingsBarComponent.configDateTo
      )
        .then(retReport => {
          console.log(':: getActivityReport :: retReport received')
          this.generateBtnDisabled = false
          this.currentReport = retReport
        })
        .catch(error => this.showErrorMsg(error))
    }
  }

}
