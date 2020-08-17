import { Component, OnInit, OnChanges, Input, SimpleChanges } from '@angular/core'
import { ToasterService } from 'angular2-toaster'

import { FeatureToggleService } from '../../../services/index'

@Component({
  selector: 'smui-report-settingsbar',
  templateUrl: './report-settingsbar.component.html',
  styleUrls: ['./report-settingsbar.component.css']
})
export class ReportSettingsBarComponent implements OnInit, OnChanges {

  @Input() currentSolrIndexId: string = null

  // TODO make more elegant in just one dict
  reportSelectOptionModelKeys = ['report/rules', 'report/activity']
  reportSelectOptionModel = {
    'report/rules': 'Oldest modification of rules',
    'report/activity': 'Latest rule management activities'
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

  reselectReport() {
    console.log('In ReportSettingsBarComponent :: reselectReport :: this.reportSelectValue = ' + this.reportSelectValue)
  }

  generateReport() {
    console.log('In ReportSettingsBarComponent :: reselectReport')
  }

}
