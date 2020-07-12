import { Component, Input, EventEmitter } from '@angular/core';

import { ActivityLogEntry } from '../../../models/index';
import { ActivityLogService } from '../../../services/index';

@Component({
  selector: 'smui-activity-log',
  templateUrl: './activity-log.component.html',
  styleUrls: ['./activity-log.component.css']
})
export class DetailActivityLog {
  @Input() selectedListItem = null;

  private detailSearchInputId: String = null;
  private inputRuleActivityLog: Array<ActivityLogEntry> = null;

  ngOnChanges(changes: SimpleChanges) {
    console.log('In DetailActivityLog :: ngOnChanges');

    if (this.selectedListItem) {
      this.detailSearchInputId = this.selectedListItem.id
    }
  }

  public loadInputRuleActivityLog() {
    if (this.detailSearchInputId !== null) {
      console.log('In DetailActivityLog :: loadInputRuleActivityLog with detailSearchInput')
      this.searchManagementService.getInputRuleActivityLog(this.detailSearchInputId)
        .then(retActivityLog => {
          console.log(':: retActivityLog received')
          this.inputRuleActivityLog = retActivityLog
        })
        .catch(error => this.handleError(error));
    }
  }

}
