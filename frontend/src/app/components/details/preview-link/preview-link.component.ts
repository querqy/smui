import {
    Component,
    Input
} from '@angular/core'

import {
    PreviewSection
} from '../../../models'

@Component({
    selector: 'app-smui-preview-link',
    templateUrl: './preview-link.component.html',
    styleUrls: ['./preview-link.component.css']
})
export class PreviewLinkComponent {

    @Input() previewSections: PreviewSection[] = []

    constructor(
    ) { }

    openPreviewLink(toURL: string): void {
        window.open(toURL, "_smui");
    }
    
}
