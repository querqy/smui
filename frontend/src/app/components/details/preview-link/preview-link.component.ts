import {
    Component
} from '@angular/core'

import {
    PreviewSection
} from '../../../models'

import {
    PreviewLinkService
} from '../../../services'

@Component({
    selector: 'app-smui-preview-link',
    templateUrl: './preview-link.component.html',
    styleUrls: ['./preview-link.component.css']
})
export class PreviewLinkComponent {

    constructor(
        public previewLinkService: PreviewLinkService
    ) {}

    previewData(inputTerm: string): PreviewSection[] {
        return this.previewLinkService.renderLinkFor(inputTerm)
    }

    openPreviewLink(toURL: string): void {
        console.log('TODO openPreviewLink :: toURL = ' + toURL)
    }
    
}
