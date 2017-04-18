import {IText} from '../';

export class Schema implements IText {
    public id:number;
    public code:string;
    public description:string;
    
    
    constructor(id:number, code:string, description:string) {
        this.code = code;
        this.id = id;
        this.description = description;
    }
    
    get htmlText():string  { return this.description;}
    
    static mapToProperty = (schema:Object) => schema["code"];
}   