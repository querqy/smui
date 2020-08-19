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

  @Output() selectReport: EventEmitter<string> = new EventEmitter()
  @Output() generateReport: EventEmitter<any> = new EventEmitter()

  // TODO make more elegant in just one dict
  reportSelectOptionModelKeys = [
    'rules-report',
    //'activity-report'
  ]
  // keys aligned with URL partial of /report route in /smui/conf/routes
  reportSelectOptionModel = {
    'rules-report': 'Most outdated rules (by modification date)',
    //'activity-report': 'Latest rule management activities'
  }
  reportSelectValue = this.reportSelectOptionModelKeys[0]

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

  clickSelectReport() {
    console.log('In ReportSettingsBarComponent :: clickSelectReport :: this.reportSelectValue = ' + this.reportSelectValue)
    this.selectReport.emit(this.reportSelectValue)
  }

  clickGenerateReport() {
    console.log('In ReportSettingsBarComponent :: clickGenerateReport')
    // TODO make configDateFrom/To a datetime type
    this.generateReport.emit({
      'report': this.reportSelectValue,
      'dateFrom': this.configDateFrom,
      'dateTo': this.configDateTo
    })
  }

}
