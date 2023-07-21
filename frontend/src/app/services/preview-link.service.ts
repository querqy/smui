import {
    Injectable
} from '@angular/core'

import {
    PreviewItem,
    PreviewSection
} from './../models/preview-link.model'

import {
    TargetEnvironmentDescription,
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

    public previewLinksAvailable(): boolean {
        const targetEnvConf = this.configService.targetEnvironment
        if( targetEnvConf === undefined ) {
            return false
        } else {
            return targetEnvConf.length > 0
        }
    }

    public renderLinkFor(inputTerm: string, rulesCollectionName: string, allowOnlyTenantTags: string[]): PreviewSection[] {

        function renderPreviewItems(targetInst: TargetEnvironmentInstance): PreviewItem[] {

            function doesEnvDescrMatchTenant(envDescr: TargetEnvironmentDescription): boolean {

                console.log("In :: doesEnvDescrMatchTenant :: envDescr = " + JSON.stringify(envDescr) + " allowOnlyTenantTags = " + JSON.stringify(allowOnlyTenantTags))

                if( allowOnlyTenantTags.length < 1 ) {
                    // No specific tenant requirement, so every entry matches.
                    return true
                } else {
                    if( envDescr.tenantTag === undefined ) {
                        // No tenant specification present in the target environment (although demanded),
                        // so it cannot match the requirement.
                        return false
                    } else {
                        // See, if some tenant requirement can be fullfilled by the environment
                        const tagFromConfig = envDescr.tenantTag.trim();
                        // Tag formats are not standardized, assume they are either "value" or "key:value",
                        // extract "value" from both variants
                        const tenantTagFromConfig = tagFromConfig.substring(tagFromConfig.lastIndexOf(":") + 1)
                        const bTenantRequirementFullfilled = allowOnlyTenantTags
                            .some(tenantTagValue => tenantTagFromConfig === tenantTagValue.trim())
                        return bTenantRequirementFullfilled
                    }
                }
            }

            // Filter all relevant target environment descriptions for the rules collection and tenant setup
            const resultPreviewItems: PreviewItem[] = []
            // TODO Consider introducing a flatMap mechanic here
            targetInst.targetEnvironmentGroups.forEach(envGroup => {
                envGroup.targetEnvironments.forEach(envDescr => {
                    if(
                        (envDescr.rulesCollection.trim() == rulesCollectionName)
                        && doesEnvDescrMatchTenant(envDescr)
                    ) {
                        // Push the preview link to the result list, if relevant
                        resultPreviewItems.push({
                            "inputTerm": inputTerm,
                            "fullURL": envDescr.previewUrlTemplate.replace(
                                "$QUERY",
                                encodeURIComponent(inputTerm)
                            ),
                            "tenantInfo": (envDescr.tenantTag === undefined) ? "" : envDescr.tenantTag
                        })
                    }
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
                // Construct the preview sections
                .map(targetInst => {
                    return<PreviewSection>{
                        "name": targetInst.id,
                        "previewItems": renderPreviewItems( targetInst )
                    }
                })
                // A preview section must have at least one preview item
                .filter(targetInst => targetInst.previewItems.length > 0)

            return rawPreviewSections
        }
    }

}
