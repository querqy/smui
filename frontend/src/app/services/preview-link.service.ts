import { 
    Injectable
} from '@angular/core'

import { 
    PreviewItem,
    PreviewSection
} from './../models/preview-link.model'

import { 
    TargetEnvironmentInstance
} from './../models/target-environment.model'


import {
    ConfigService
} from './config.service'

@Injectable({
    providedIn: 'root'
})
export class PreviewLinkService {

    constructor(
        public configService: ConfigService
    ) { }

    renderLinkFor(inputTerm: string, rulesCollectionName: string, allowOnlyTenantTags: string[]): PreviewSection[] {
        
        function renderPreviewItems(targetInst: TargetEnvironmentInstance): PreviewItem[] {
            // Filter all relevant environment groups for the rules collection and tenant setup
            const relevantEnvGroups = targetInst.targetEnvironmentGroups
                .filter(envGroup =>
                    envGroup.targetEnvironments
                        .some(envDescr => (
                                (envDescr.rulesCollection.trim() == rulesCollectionName)
                                && (true) // TODO respect allowOnlyTenantTags
                            )
                        )
                )
            
            var resultPreviewItems: PreviewItem[] = []
            // TODO Consider introding a flatMap mechanic here
            relevantEnvGroups.forEach(envGroup => {
                envGroup.targetEnvironments.forEach(targetEnv => {
                    resultPreviewItems.push({
                            "inputTerm": inputTerm,
                            "fullURL": targetEnv.previewUrlTemplate.replace(
                                "$QUERY",
                                encodeURIComponent(inputTerm)
                            ),
                            "tenantInfo": (targetEnv.tenantTag === undefined) ? "" : targetEnv.tenantTag
                        })
                })
            })
    
            return resultPreviewItems
        }

        const targetEnvConf = this.configService.targetEnvironment
        if(targetEnvConf === undefined) {
            throw new Error('Cannot render preview links (targetEnvironment is undefined)');
        } else {
            // Apply target environment config to render the preview links for the inputTerm
            const rawPreviewSections = targetEnvConf
                .map(targetInst => {
                    return<PreviewSection>{
                        "name": targetInst.id,
                        "previewItems": renderPreviewItems( targetInst )
                    }
                })

            return rawPreviewSections
        }
    }

}
