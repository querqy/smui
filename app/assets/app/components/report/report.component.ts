import { Component, OnInit, OnChanges, Input, SimpleChanges } from '@angular/core'
import { ToasterService } from 'angular2-toaster'

import { FeatureToggleService } from '../../services/index'

@Component({
  selector: 'smui-report',
  templateUrl: './report.component.html',
  styleUrls: ['./report.component.css']
})
export class ReportComponent implements OnInit, OnChanges {

  @Input() currentSolrIndexId: string = null

  constructor(
    public featureToggleService: FeatureToggleService,
    private toasterService: ToasterService
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

}
