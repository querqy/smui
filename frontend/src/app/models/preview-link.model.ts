export class PreviewItem {
    inputTerm: string
    fullURL: string

    constructor(inputTerm: string, fullURL: string) {
        this.inputTerm = inputTerm
        this.fullURL = fullURL
    }
}

export class PreviewSection {
    name: string
    previewItems: PreviewItem[]

    constructor(name: string, previewItems: PreviewItem[]) {
        this.name = name
        this.previewItems = previewItems
    }
}
