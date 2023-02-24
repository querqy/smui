import {
  Component,
  Input,
  Output,
  EventEmitter
} from '@angular/core'

import {
  PreviewLinkService
} from '../../../services'

import {
  PreviewSection,
  SearchInput
} from '../../../models'

@Component({
  selector: 'app-smui-input-row',
  templateUrl: './input-row.component.html',
  styleUrls: ['./input-row.component.css']
})
export class InputRowComponent {
  @Input() iconClass = '';
  @Input() label = '';
  @Input() placeholder = 'Please enter...';
  @Input() term = '';
  @Input() editDistance = Number.NaN;
  @Input() disabled = false;
  @Input() active = true;
  @Input() currentSolrIndexId?: string

  @Output() termChange = new EventEmitter();
  @Output() activeChange = new EventEmitter();
  @Output() handleSave = new EventEmitter();
  @Output() handleDeleteRow = new EventEmitter();

  constructor(
    public previewLinkService: PreviewLinkService
  ) { }

  // TODO consider to refactor this into a style / progress width map

  editDistanceToBootstrapWarnClass(): string {
    if(this.editDistance <= 3) {
      return "bg-success"
    } else if(this.editDistance <= 6) {
      return "bg-warning"
    } else {
      return "bg-danger"
    }
  }

  editDistanceToProgressValue(): number {
    if(this.editDistance <= 3) {
      return Math.floor(this.editDistance * (50.0/3.0))
    } else if(this.editDistance <= 6) {
      return Math.floor(60.0 + ((this.editDistance-3.0) * (35.0/3.0)))
    } else {
      return 100
    }
  }

  // TODO Consider making this part of the preview-link component (instead of duplicating the code from the rule-management component)

  showPreviewLinks(): boolean {
    return this.previewLinkService.previewLinksAvailable()
  }

  private cleanPreviewInputTerm(rawInputTerm: string): string {
    return SearchInput.stripExactMatchingSyntax(rawInputTerm)
  }

  previewLinks(forInputTerm: string): PreviewSection[] {
//    console.log('In :: previewLinks :: selectedTags = ' + JSON.stringify(this.selectedTags))
    if( this.currentSolrIndexId === undefined ) {
      console.log("[ERROR] In :: previewLinks :: currentSolrIndexId is undefined" )
      return []
    } else {
      const previewInputTerm = this.cleanPreviewInputTerm(forInputTerm)
      const returnPreviewLinks = this.previewLinkService
        .renderLinkFor(
          previewInputTerm,
          this.currentSolrIndexId,
          []
        )
//      console.log(':: returnPreviewLinks = ' + JSON.stringify(returnPreviewLinks))
      return returnPreviewLinks
    }
  }

  warnForExactMatchingSyntax(): boolean {
    return (
      SearchInput.isTermExact(this.term)
      || SearchInput.isTermLeftExact(this.term)
      || SearchInput.isTermRightExact(this.term)
    )
  }

}
