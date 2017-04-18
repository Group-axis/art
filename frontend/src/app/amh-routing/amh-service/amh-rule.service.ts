import {Headers, RequestOptions, Http} from 'angular2/http';
import {Injectable} from 'angular2/core';
import {Observable} from 'rxjs/Observable';
import {RuleAssignType, AssignType, AssignmentUnique, AssignmentUniqueRule, AssignmentList, AMHRule, Backend, BackendPK} from '../../models/routing-amh';
import {FileDownloader, Store, Config, Logger} from '../../common/components/services';
import {User} from '../../models/users';
import 'rxjs/add/operator/reduce';

/*
{
  "query": {
    "bool": {
      "must": [
        {
          "match": {
            "assigned": {
              "query": "false",
              "analyzer": "standard",
              "operator": "and"
            }
          }
        },
        {
          "bool": {
            "should": [
              {
                "match": {
                  "code": {
                    "query": "BA-BPPBCHGG-FUNDS-PRCH",
                    "analyzer": "standard",
                    "operator": "and"
                  }
                }
              },
              {
                "match": {
                  "expression": {
                    "query": "BA-BPPBCHGG-FUNDS-PRCH",
                    "analyzer": "standard",
                    "operator": "and"
                  }
                }
              }
            ]
          }
        }
      ]
    }
  }
}
 */

@Injectable()
export class AMHRuleService {

  private nested(path : string, query : string) : string {
   return ` "nested": {
     "path": "##path##",
     ##query##
   }
   `.replace(/##path##/g, path)
   .replace(/##query##/g,query);
 }
 
 private query(content : string) : string {
    return ` "query" : {
       ###content###
    }
    `.replace(/###content###/g, content);
  }
  
  private match(field : string, value: string) :string {
    return ` "match": {
       "##field##": {
         "query": "##value##",
         "analyzer": "standard",
         "operator": "and"
       }
    }
    `.replace(/##field##/g,field)
     .replace(/##value##/g, value);
  }

  private bool(content : Array<string>) : string {
    return ` "bool": { 
        ###content###
      }`.replace(/###content###/g, content.join(' , '));
  }

  private must(content : Array<string>) : string {
    return this.filter("must", content);
  }

  private should(content : Array<string>) : string {
    return this.filter("should", content);
  }

  private notMust(content : Array<string>) : string {
    return this.filter("not_must", content);
  }

private filter(type: string, content : Array<string>) : string {
    return ` "##type##": [ 
        ###content###
      ]`
      .replace(/##type##/g,type)
      .replace(/###content###/g, content.map(e => '{ ' + e + ' }').join(' , '));
  }

  private codeExpressionQuery : string = `
  "bool": {
      "should": [
        {
          "match": {
            "code": {
              "query": "##TO_REPLACE##",
              "analyzer": "standard",
              "operator": "and"
            }
          }
        },
        {
          "match": {
            "expression": {
              "query": "##TO_REPLACE##",
              "analyzer": "standard",
              "operator": "and"
            }
          }
        }
      ]
    }
  `;
  private matchQuery : string = ` 
  "query": {
    ###CODE_EXPR_FILTER_TO_REPLACE###
    ###ASSIGN_FILTER_TO_REPLACE###
  }
  `; 
  private ruleQuery : string = `
  "query": {
    "bool": {
      "should": [
        {
          "match": {
            "assigned": {
              "query": "##TO_REPLACE##",
              "analyzer": "standard",
              "operator": "and"
            }
          }
        }
      ]
    }
  }
  `;
private ruleAssignTypeQuery = `
{ 
    "match": {
    "assigned": {
    "query": "##TO_REPLACE##",
    "analyzer": "standard",
    "operator": "and"
    }
  }
}
`;

  private querySize :  string = `
  "from": 0,
  "size": 1000,
  "sort": [
    {
      "code": {
        "order": "asc"
      }
    }
  ]
  `;

  private assignedQuery(type : RuleAssignType) : string {
    if (type == RuleAssignType.ALL) {
       return this.querySize;
    }

    let assignedText : string = ""+(type == RuleAssignType.ASSIGNED)
    return  this.ruleQuery.replace(/##TO_REPLACE##/g, assignedText) + " , " +this.querySize;
  };

  private getRuleAssignTypeQuery(type: RuleAssignType) {
   if (type == RuleAssignType.ALL) {
       return "";
    }

    let assignedText : string = ""+(type == RuleAssignType.ASSIGNED)
    return  " , "+this.ruleAssignTypeQuery.replace(/##TO_REPLACE##/g, assignedText);
  }

  private getMatchQuery(type: RuleAssignType, text? : string) : string {
     let isAllType : boolean = type == RuleAssignType.ALL;

    if (isAllType && (!text || text.length == 0)) {
      return this.querySize;
    }
    
    if (!text || text.length == 0 && !isAllType) {
      return this.assignedQuery(type) + " , " + this.querySize;
    }
 
    let assignedParam = String(type == RuleAssignType.ASSIGNED);
  
  
  let fquery = isAllType ? this.query(
                  this.bool([
                        this.should([
                          this.match("code",text),
                          this.match("expression",text)
                        ])
                      ])
                  ) :
     this.query(
                  this.bool([
                    this.must([
                      this.match("assigned",assignedParam),
                      this.bool([
                        this.should([
                          this.match("code",text),
                          this.match("expression",text)
                        ])
                      ])
                    ])
                  ])
                  );

    return  fquery + " , " +this.querySize;
  };


  private persistence : any;
  constructor(private http: Http, private config: Config, private store: Store, private fileDownloader :FileDownloader
  , private logger : Logger) {
    this.persistence = sessionStorage; 
  }

  findRules(): Observable<Array<AMHRule>> {
    return this.findRulesByAssignType(RuleAssignType.ALL);
  }

  findRulesByAssignType(type : RuleAssignType): Observable<Array<AMHRule>> {
    let headers = new Headers({ 'Content-Type': 'application/json' });
    let options = new RequestOptions({ headers: headers });
    let queryPayload = "{"+this.assignedQuery(type)+"}";
    // this.logger.debug("findRulesByAssignType - payload "+queryPayload);
    return this.http.post(this.config.get("esBackUrl")+"/amhrouting/rules/_search", queryPayload, options)
        .map(res => res.json())
        .flatMap(this.toHits)
        .reduce((acc, R) => { acc.push(R); return acc; }, new Array<AMHRule>());;
  }
  
  findRuleByCode(code: string): Observable<any> {
    return this.http.get(this.config.get("esBackUrl")+"/amhrouting/rules/"+code)
    .map(res => res.json())
    .map(res => res.found ? {"found":true, "value": res._source} :{"found":false,"value":{}} );
  }

  private toHits(match): Observable<any> {
    return Observable.from(match['hits'].hits)
      .map(hit => hit["_source"])
  }

 findRuleMatches(type: RuleAssignType, text?: string): Observable<Array<AMHRule>> {
  // let headers = new Headers({ 'Content-Type': 'application/json' });
  // let options = new RequestOptions({ headers: headers });
  let payload = "{"+this.getMatchQuery(type, text)+"}";
  // this.logger.debug("findRuleMatches - payload "+payload);
  return this.http.post(this.config.get("esBackUrl") + "/amhrouting/rules/_search?size=1000", payload) //, options
    .map(res => res.json())
    .flatMap(this.toHits)
    .reduce((acc, R) => { acc.push(R); return acc; }, new Array<AMHRule>());
}

 private toAssignment(match): Observable<any> {
    return Observable.from(match['hits'].hits)
      .map(hit => { return { "type":hit["_type"],"source":hit["_source"]}; })
  }

 findAssignmmentsByRuleCode(ruleCode : string) : Observable<Array<any>> {
  let payload = "{" + 
               this.query(
                  this.bool([ this.should([
                          this.nested("rules", this.query(
                              this.bool([ this.must([ this.match("rules.code", ruleCode) ])])
                          ))])
                      ])
                  ) + "}";

  // this.logger.debug("findAssignmmentsByRuleCode - payload "+payload);
  return this.http.post(this.config.get("esBackUrl") + "/amhrouting/assignments,distributionCopies,feedbackDtnCopies/_search?size=1000", payload) //, options
    .map(res => res.json())
    .flatMap(this.toAssignment)
    .reduce((acc, R) => { acc.push({"type":R.type, "code":R.source.code,"sequence":R.source.sequence}); return acc; }, new Array<any>());
 }

deleteRule(ruleCode : string, user : User) : Observable<any> {
  let headers = new Headers(
      { 'Content-Type': 'application/json',
        'userId': user.username,
        'time': Date.now()
      });
    let options = new RequestOptions({ headers: headers });

    return this.http.delete(this.config.get("backUrl")+"/amhrouting/rules/"+ruleCode, options)
      .map(res =>  { this.logger.log(" from rule deleted "+res.json()); res.json(); })
  }

  exportRuleOverview(ruleAssignType : RuleAssignType, user: User): Observable<any> {
    let headers = new Headers(
      {
        'Content-Type': 'application/json',
        'userId': user.username,
        'time': Date.now()
      });
    let options = new RequestOptions({ headers: headers });
    let payload = JSON.stringify({});
    return this.http.post(this.config.get("backUrl") + "/amhrouting/csv/export/rules/" + ruleAssignType, payload, options)
      .map(res => res.json());
  }

  downloadFile(fileNamePath : string, fileName: string) {
    this.fileDownloader.download(this.config.get("backUrl") + fileNamePath, fileName);
  }

}