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
                        const envTenantTag = envDescr.tenantTag
                        const bTenantRequirementFullfilled = allowOnlyTenantTags
                            .some(tenantTagValue => 
                                // TODO here we are testing for substring occurance, e.g.
                                //   "tenant:AmazonDE" contains "AmazonDE"
                                //   As it not fully enforced or specified by SMUI how
                                //   tenant tags are being defined, this is a bit risky.
                                envTenantTag.trim().indexOf(tenantTagValue.trim()) !== -1
                            )
                        
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
