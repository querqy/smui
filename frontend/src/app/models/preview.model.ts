export class PreviewItem {
    queryLabel: string;
    fullURL: string;
    tenantTag?: string = undefined;

    constructor(queryLabel: string, fullURL: string) {
        this.queryLabel = queryLabel
        this.fullURL = fullURL
    }
}

export class PreviewSection {
    name: string;
    previewItems: PreviewItem[];

    constructor(name: string, previewItems: PreviewItem[]) {
        this.name = name
        this.previewItems = previewItems
    }
}
