import { Injectable } from '@angular/core'

import { PreviewItem, PreviewSection } from './../models/preview-link.model'

import { ConfigService } from './config.service'

@Injectable({
    providedIn: 'root'
})
export class PreviewLinkService {

    constructor(public configService: ConfigService) { }

    renderLinkFor(inputTerm: string): PreviewSection[] {
        console.log("In PreviewLinkService :: renderLinkFor :: inputTerm = " + inputTerm + " this.configService.targetEnvironment = " + JSON.stringify(this.configService.targetEnvironment))

        return [
            new PreviewSection('LIVE', [
                new PreviewItem('rule1','https://domain.tld/search/?query=rule1'),
                new PreviewItem('rule2','https://domain.tld/search/?query=rule2')
            ]),
            new PreviewSection('PRELIVE (Staging)', [
                new PreviewItem('rule1','https://staging-domain.tld/search/?query=rule1'),
                new PreviewItem('rule2','https://staging-domain.tld/search/?query=rule2')
            ]),
        ]
    }

}
