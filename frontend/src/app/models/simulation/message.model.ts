export interface IMessage {
    id :string;
    name : string;
    content: string;
    group: string;
    groupCount : number;
}

export class Message implements IMessage {
    public id: string;
    public name: string;
    public content: string;
    public group: string;
    public groupCount: number;
    public itemMap : Map<string,string>;
    public messages : Array<Message>;
    /* senderAddress, receiverAddress , swiftParameters_service , messageType_code, swiftParameters_requestReference */
    // UI properties
    public selected : boolean;
    
    constructor(id: string, name: string, content: string, group?: string, itemMap? : Map<string,string>, messages? : Array<Message>, groupCount?: number) {
        this.id = id;
        this.name = name;
        this.content = content;
        this.group = group;
        this.groupCount = groupCount;
        this.itemMap = itemMap;
        this.messages = messages;
    }

//  static objToString (obj) {
//     var tabjson=[];
//     for (var p in obj) {
//         if (obj.hasOwnProperty(p)) {
//             tabjson.push('"'+p +'"'+ ':' + obj[p]);
//         }
//     }  tabjson.push()
//     return '{'+tabjson.join(',')+'}';
// }
//  static toString(value : any) : string {
//     if (value ) {
//       if (value.isArray && value.length > 0) {
//         return value[0];
//       }
//       if (value.isArray && value.length == 0) {
//         return "";
//       }

//       return value;
//     }

//     return "";
//   }

    public toWriteModel(username : string) {

        return {
            "userId" : username,
            "creationDate": new String(Date.now()),
            "fileName": this.name,
            "content": this.content,
            "group": this.group
        };    
    }

/*
INSERT INTO MAPPING VALUES('senderAddress','<Saa:Sender>.*<Saa:DN>(.*)</Saa:DN>.*</Saa:Sender>',null);
INSERT INTO MAPPING VALUES('receiverAddress','<Saa:Receiver>.*<Saa:DN>(.*)</Saa:DN>.*</Saa:Receiver>',null);
INSERT INTO MAPPING VALUES('swiftParameters/service','<Saa:NetworkInfo>.*<Saa:Service>(.*)</Saa:Service>.*</Saa:NetworkInfo>',null);
INSERT INTO MAPPING VALUES('messageType/code','<Saa:MessageIdentifier>(.*)</Saa:MessageIdentifier>',null);
INSERT INTO MAPPING VALUES('swiftParameters/requestReference','<Saa:SenderReference>(.*)</Saa:SenderReference>',null);
*/    

}

export function getItem(field : string, itemMap : Map<string,string>) : string {
    return itemMap.get(field);
}

