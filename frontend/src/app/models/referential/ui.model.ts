export interface IText {
    htmlText: string;
    id :number;
    
}

export interface IOption {
    id: number;
    code: string;
    text: string;
    children: IOption[];
}

export class ItemContainer implements IText {
    public id: number;
    public description: string;
    public children: ItemChild[];

    constructor(id: number, description: string) {
        this.id = id;
        this.description = description;
        this.children = [];
    }

    get htmlText(): string { return this.description; }

    addChild(child: ItemChild) {
        this.children.push(child);
    }

}

export class ItemChild implements IText {
    public id: number;
    public description: string;

    constructor(id: number, description: string) {
        this.id = id;
        this.description = description;
    }

    get htmlText(): string { return this.description; }

}

export class IdCodeDescription implements IText {
    public id: number;
    public code: string;
    public description: string;

    constructor(id: number, code: string, description: string) {
        this.id = id;
        this.code = code;
        this.description = description;
    }

    get htmlText(): string { return this.description; }

    static mapToProperty = (option: Object) => option["code"];

    static fromJson(json: string) {
        var data = JSON.parse(json);
        return new IdCodeDescription(data.id, data.code, data.description);
    }

    static empty() : IdCodeDescription {
       return new IdCodeDescription(1, "",""); 
    }
}

export function updateFields(dest : any, orig : any, fields : Array<string>) {
    fields.forEach( f => dest[f] = orig[f] || dest[f]);
}

