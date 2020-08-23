import { Component, OnInit, OnChanges, Input, Output, EventEmitter, SimpleChanges } from '@angular/core'
import { ToasterService } from 'angular2-toaster'

import { FeatureToggleService } from '../../../services/index'

@Component({
  selector: 'smui-report-settingsbar',
  templateUrl: './report-settingsbar.component.html',
  styleUrls: ['./report-settingsbar.component.css']
})
export class ReportSettingsBarComponent implements OnInit, OnChanges {

  @Input() currentSolrIndexId: string = null

  @Output() changeReport: EventEmitter<void> = new EventEmitter()
  @Output() generateReport: EventEmitter<void> = new EventEmitter()

  // TODO make more elegant in just one dict
  private reportSelectOptionModelKeys = [
    'rules-report',
    'activity-report'
  ]
  // keys aligned with URL partial of /report route in /smui/conf/routes
  private reportSelectOptionModel = {
    'rules-report': 'Most outdated rules (by modification date)',
    'activity-report': 'Latest rule management activities'
  }
  configReport = this.reportSelectOptionModelKeys[0]

  configDateFrom: string = null
  configDateTo: string = null

  constructor(
    public featureToggleService: FeatureToggleService,
    private toasterService: ToasterService
  ) {}

  ngOnInit() {
    console.log('In ReportSettingsBarComponent :: ngOnInit')
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes.currentSolrIndexId) {
      console.log('In ReportSettingsBarComponent :: ngOnChanges :: currentSolrIndexId = ' + changes.currentSolrIndexId)
    }
  }

  public showSuccessMsg(msgText: string) {
    this.toasterService.pop('success', '', msgText);
  }

  public showErrorMsg(msgText: string) {
    this.toasterService.pop('error', '', msgText);
  }

  clickChangeReport() {
    console.log('In ReportSettingsBarComponent :: clickChangeReport')
    this.changeReport.emit()
  }

  clickSetFromDate(deployInstance: string) {
    console.log('In ReportSettingsBarComponent :: clickSetFromDate :: deployInstance = ' + deployInstance)
  }

  clickSetToDate() {
    console.log('In ReportSettingsBarComponent :: clickSetToDate')

    // needed to convert Date
    const withLeadingZero = function (datePart: number): string {
      if (datePart < 10) {
        return '0' + datePart
      } else {
        return '' + datePart
      }
    }

    const now = new Date()
    console.log(':: now = ' + now.toString())
    this.configDateTo = now.getFullYear() + '-' + withLeadingZero(now.getMonth() + 1) + '-' + withLeadingZero(now.getDate())
  }

  clickGenerateReport() {
    console.log('In ReportSettingsBarComponent :: clickGenerateReport')
    // validate input for activity-report
    if (this.configReport === 'activity-report') {
      if (this.configDateFrom === null || this.configDateTo === null) {
        // TODO make validation violation a nicer UX
        this.showErrorMsg('Please select a from and to date for your rules activity report.')
      } else {
        this.generateReport.emit()
      }
    } else {
      this.generateReport.emit()
    }
  }

}
