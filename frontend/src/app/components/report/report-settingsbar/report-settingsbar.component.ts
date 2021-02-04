import {
  Component,
  OnInit,
  OnChanges,
  Input,
  Output,
  EventEmitter,
  SimpleChanges
} from '@angular/core';
import { ToasterService } from 'angular2-toaster';

import { FeatureToggleService, SolrService } from '../../../services';
import { DeploymentLogInfo } from '../../../models';

interface ReportOption<ValueType> {
  [key: string]: ValueType;
}

@Component({
  selector: 'app-smui-report-settingsbar',
  templateUrl: './report-settingsbar.component.html',
  styleUrls: ['./report-settingsbar.component.css']
})
export class ReportSettingsBarComponent implements OnInit, OnChanges {
  @Input() currentSolrIndexId?: string;
  @Input() generateBtnDisabled = false;

  @Output() changeReport: EventEmitter<void> = new EventEmitter();
  @Output() generateReport: EventEmitter<void> = new EventEmitter();

  // TODO make more elegant in just one dict
  reportSelectOptionModelKeys = ['rules-report', 'activity-report'];
  // keys aligned with URL partial of /report route in /smui/conf/routes
  reportSelectOptionModel: ReportOption<string> = {};
  configReport: string = this.reportSelectOptionModelKeys[0];

  configDateFrom?: string;
  configDateTo?: string;

  constructor(
    public featureToggleService: FeatureToggleService,
    private toasterService: ToasterService,
    private solrService: SolrService
  ) {
    this.reportSelectOptionModel['rules-report'] = 'Oldest rules (by last_updated date)';
    this.reportSelectOptionModel['activity-report'] = 'Latest rule management activities';
  }

  ngOnInit() {
    console.log('In ReportSettingsBarComponent :: ngOnInit');
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes.currentSolrIndexId) {
      console.log(
        'In ReportSettingsBarComponent :: ngOnChanges :: currentSolrIndexId = ' +
          changes.currentSolrIndexId
      );
    }
  }

  showErrorMsg(msgText: string) {
    this.toasterService.pop('error', '', msgText);
  }

  clickChangeReport() {
    console.log('In ReportSettingsBarComponent :: clickChangeReport');
    this.changeReport.emit();
  }

  // TODO not too nice - maybe use UX component, that can handle Date directly in future ...
  dateToFrontendString(d: Date) {
    // needed to convert Date
    const withLeadingZero = (datePart: number) => {
      if (datePart < 10) {
        return '0' + datePart;
      } else {
        return '' + datePart;
      }
    };

    return (
      d.getFullYear() +
      '-' +
      withLeadingZero(d.getMonth() + 1) +
      '-' +
      withLeadingZero(d.getDate())
    );
  }

  clickSetFromDate(deployInstance: string) {
    console.log(
      'In ReportSettingsBarComponent :: clickSetFromDate :: deployInstance = ' +
        deployInstance
    );
    console.log(':: this.currentSolrIndexId = ' + this.currentSolrIndexId);
    if (this.currentSolrIndexId) {
      this.solrService
        .lastDeploymentLogInfo(this.currentSolrIndexId, deployInstance, true)
        .then(retDeplInfo => {
          console.log(
            ':: clickSetFromDate :: retDeplInfo = ' +
              JSON.stringify(retDeplInfo)
          );
          // TODO make date format backend/frontend definitions more robust
          // assume date to be in format, e.g.: 2020-02-16T23:59:12 (within msg field)
          if (retDeplInfo instanceof DeploymentLogInfo) {
            this.configDateFrom = this.dateToFrontendString(
              new Date(Date.parse(retDeplInfo.msg))
            );
          } else {
            this.configDateFrom = undefined;
          }
        })
        .catch(error => this.showErrorMsg(error));
    }
  }

  clickSetToDate() {
    console.log('In ReportSettingsBarComponent :: clickSetToDate');

    const now = new Date();
    console.log(':: now = ' + now.toString());
    this.configDateTo = this.dateToFrontendString(now);
  }

  clickGenerateReport() {
    console.log('In ReportSettingsBarComponent :: clickGenerateReport');
    // validate input for activity-report
    if (this.configReport === 'activity-report') {
      if (!this.configDateFrom || !this.configDateTo) {
        // TODO make validation violation a nicer UX
        this.showErrorMsg(
          'Please select a from and to date for your rules activity report.'
        );
      } else {
        this.generateReport.emit();
      }
    } else {
      this.generateReport.emit();
    }
  }
}
