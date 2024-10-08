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

import {
  FeatureToggleService,
  SolrService,
  DeploymentDetailedInfoService
} from '../../../services';
import {
  DeploymentDetailedInfo
} from '../../../models';

// keys for selectint the different reports, aligned with URL partial of /report route in /smui/conf/routes
export const KEY_OLDEST_RULES_REPORT = 'rules-report';
export const KEY_ACTIVITY_REPORT = 'activity-report';
export const KEY_RULES_USAGE_REPORT = 'rules-usage-report';

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

  private readonly OLDEST_RULES_REPORT: [string, string] = [KEY_OLDEST_RULES_REPORT, 'Oldest rules (by last_updated date)']
  private readonly ACTIVITY_REPORT: [string, string] = [KEY_ACTIVITY_REPORT, 'Latest rule management activities']
  private readonly RULES_USAGE_REPORT: [string, string] = [KEY_RULES_USAGE_REPORT, 'Rules usage']

  configReport: string = KEY_OLDEST_RULES_REPORT;

  configDateFrom?: string;
  configDateTo?: string;

  constructor(
    public featureToggleService: FeatureToggleService,
    private toasterService: ToasterService,
    public deploymentDetailedInfoService: DeploymentDetailedInfoService
  ) {}

  availableReports() {
    return [
      this.OLDEST_RULES_REPORT,
      this.ACTIVITY_REPORT,
      ...(this.featureToggleService.getSyncToggleRuleUsageStatistics() ? [this.RULES_USAGE_REPORT] : [])
    ]
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
      this.deploymentDetailedInfoService
        .get(this.currentSolrIndexId)
        .then(apiDeploymentInfo => {
          // filter deployInstance down to exactly one entry
          let instanceDeplInfoList = apiDeploymentInfo
            .filter(elemDeplInfo => elemDeplInfo.targetSystem == deployInstance)
          if( instanceDeplInfoList.length == 1 ) {
            let instanceDeplInfo = instanceDeplInfoList[0]

            console.log(
              ':: clickSetFromDate :: instanceDeplInfo = ' +
                JSON.stringify(instanceDeplInfo)
            )
            // TODO make date format backend/frontend definitions more robust
            // assume date to be in format, e.g.: "2020-02-16 23:59"
            this.configDateFrom = this.dateToFrontendString(
              new Date(Date.parse(instanceDeplInfo.formattedDateTime))
            )

          } else {
            this.showErrorMsg('Error in clickSetFromDate :: deployInstance = "' + deployInstance + '" not found!')
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
