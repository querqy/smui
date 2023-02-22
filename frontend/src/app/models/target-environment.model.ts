export class TargetEnvironmentDescription {
    rulesCollection: string
    tenantTag?: string
    previewUrlTemplate: string
}

export class TargetEnvironmentGroup {
    id: string
    targetEnvironments: TargetEnvironmentDescription[]
}

export class TargetEnvironmentInstance {
    id: string
    targetEnvironmentGroups: TargetEnvironmentGroup[]
}
