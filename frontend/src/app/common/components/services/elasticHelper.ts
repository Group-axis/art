export class Elastic4js {

    static nested(path: string, query: string): string {
        return ` "nested": {
     "path": "##path##",
     ##query##
   }
   `.replace(/##path##/g, path)
            .replace(/##query##/g, query);
    }

    static query(content: string): string {
        return ` "query" : {
       ###content###
    }
    `.replace(/###content###/g, content);
    }

    static match(field: string, value: string): string {
        return ` "match": {
       "##field##": {
         "query": "##value##",
         "analyzer": "id_analyzer",
         "operator": "and"
       }
    }
    `.replace(/##field##/g, field)
            .replace(/##value##/g, value);
    }

    static bool(content: Array<string>): string {
        return ` "bool": { 
        ###content###
      }`.replace(/###content###/g, content.join(' , '));
    }

    static must(content: Array<string>): string {
        return this.filter("must", content);
    }

    static should(content: Array<string>): string {
        return this.filter("should", content);
    }

    static notMust(content: Array<string>): string {
        return this.filter("not_must", content);
    }

    static filter(type: string, content: Array<string>): string {
        return ` "##type##": [ 
        ###content###
      ]`
            .replace(/##type##/g, type)
            .replace(/###content###/g, content.map(e => '{ ' + e + ' }').join(' , '));
    }
}
