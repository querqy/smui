/*
export class PreviewItem {
    constructor(readonly inputTerm: string, readonly fullURL: string) { }
}

export class PreviewSection {
    constructor(readonly name: string, readonly previewItems: PreviewItem[]) { }
}
*/

export interface PreviewItem {
    inputTerm: string
    fullURL: string
    tenantInfo: string
}

export interface PreviewSection {
    name: string
    previewItems: PreviewItem[]
}
