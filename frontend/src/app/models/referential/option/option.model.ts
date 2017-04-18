import {IText} from '../';

export class Option implements IText {
    public id:number;
    public code:string;
    public description:string;
    public selected : boolean;
    
    
    constructor(id:number, code:string, description:string, selected:boolean = false) {
        this.code = code;
        this.id = id;
        this.description = description;
        this.selected = selected;
    }
    
    get htmlText():string  { return this.description;}
    
    static mapToProperty = (option:Object) => option["id"];
    
    static fromJson(json: string) {
        var data = JSON.parse(json);
        return new Option(data.id, data.code, data.description);
    }
    
}   