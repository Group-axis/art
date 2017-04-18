import { Headers, RequestOptions, Http } from 'angular2/http';
import { Injectable } from 'angular2/core';
import { Observable } from 'rxjs/Observable';
import { Assignment, AssignmentRule, AssignmentList, AMHRule, Backend, BackendPK } from '../models/routing-amh';
import { Rule } from '../models/routing';
import { TreeNode } from '../common/components/ui/widgets/tree-view';
import { Config, Logger } from '../common/components/services';
import { Point, PointList } from "../models/routing";
import { Option } from '../models/referential/option';
import 'rxjs/add/operator/reduce';

@Injectable()
export class SAARoutingService {
  private schemaClause = `
  {
    "match": {
        "rules.schemas": {
        "query": "##TO_REPLACE##",
        "analyzer": "standard",
        "operator": "and"
        }
      }
  }
  `;
  private pointsBySchemaQuery = `
  { 
    "query": {
    "bool": {
      "must": [
        {
          "nested": {
            "path": "rules",
            "query": {
              "bool": {
                "should": [
                  ##MATCH_CLAUSES##
                ]
              }
            }
          }
        }
      ]
    }
  }
    ##FILTER_RULES_CLAUSE##
    ##RANGE_SORT_CLAUSE##
  }`;

  private filterRules : string = `
  ,
  "filter":{
    ##RULE_CLAUSE##
  }`;

  private query: string = ` 
    {
  "query": {
    ##RULE_CLAUSE##
  }
  ##RANGE_SORT_CLAUSE##
}
  `;

  private fromSortClauses = `
  ,
  "from": 0,
  "size": 1000,
  "sort": [
    {
      "pointName": {
        "order": "asc"
      }
    },
    {
      "rules.sequence": {
        "order": "desc"
      }
    }
  ]
  `;

  private queryRule: string = ` 
    "bool": {
      "should": [
        {
          "match": {
            "pointName": {
              "query": "##TO_REPLACE##",
              "analyzer": "standard",
              "operator": "and"
            }
          }
        },
        {
          "nested": {
            "path": "rules.condition",
            "query": {
              "bool": {
                "must": [
                  {
                    "match": {
                      "rules.condition.message": {
                        "query": "##TO_REPLACE##",
                        "analyzer": "standard",
                        "operator": "and"
                      }
                    }
                  }
                ]
              }
            }
          }  
        },
        {
          "nested": {
            "path": "rules",
            "query": {
              "bool": {
                "must": [
                  {
                    "match": {
                      "rules.description": {
                        "query": "##TO_REPLACE##",
                        "analyzer": "standard",
                        "operator": "and"
                      }
                    }
                  }
                ]
              }
            }
          }
        }
      ]
    }
  `;
  constructor(private http: Http, private config: Config, private logger: Logger) {
    // this.logger.log(" retrieving config");
    // this.logger.log("saaBackUrl value: " + config.get('saaBackUrl'));
  }

  findAssignmentByID(productID: string): Observable<any> {
    return this.http.get('/products/${productID}').map(res => res.json());
  }

  //TODO: 
  /*
   *  + remove limit size=100 for all http request, use pagination instead  
  */

  findPoints(): Observable<any> {
    return this.http.post(this.config.get("esBackUrl") + "/routing/points/_search", '{"sort": [{"pointName": {"order": "asc"}}, {"rules.sequence": {"order": "desc"}}]}').map(res => res.json());
  }

  findPointNames(): Observable<any> {
    return this.http.post(this.config.get("esBackUrl") + "/routing/points/_search?size=1000", '{ "fields": ["pointName"], "sort": [{"pointName": {"order": "asc"}}, {"rules.sequence": {"order": "desc"}}]}')
      .map(res => res.json())
      .flatMap(dataset => Observable.from(dataset['hits'].hits))
      .map(hit => {
        let toReturn = {
          "pointName": hit["fields"]["pointName"][0]
        }
        // this.logger.debug("hit "+JSON.stringify(hit) + "  to return "+ JSON.stringify(toReturn));
        return toReturn;
      });
  }

  findPointMatches(schemas : Array<string>, text?: string): Observable<Array<PointList>> {
    if (text) {
      return this.findPointMatchesJson(schemas,text)
        .flatMap(this.toHits)
        .flatMap(this.toPointList)
        .reduce((acc, R) => { acc.push(R); return acc }, new Array<PointList>());
      //TODO: filter with match
    } else {
      return this.findPointMatchesJson(schemas,text)
        .flatMap(this.toHits)
        .flatMap(this.toPointList)
        .reduce((acc, R) => { acc.push(R); return acc }, new Array<PointList>());
    }
  }

  private toPointList(point): Observable<PointList> {
    let list: Array<PointList> = [];
    if (point.rules.length > 0) {

      point.rules.map(rule => {
        let originalPoint = rule.action.source ? rule.action.source.actionOption : "";
        let originalCode = rule.action.source ? rule.action.source.routingCode : "";
        let copyPoint = rule.action.newInstance ? rule.action.newInstance.actionOption : "";
        let copyCode = rule.action.newInstance ? rule.action.newInstance.routingCode : "";

        list.push(new PointList(rule.routingPoint, rule.schemas, rule.sequence, rule.description, rule.condition ? rule.condition.message : "",
          originalPoint, originalCode, copyPoint, copyCode));
      });

    } else {
      //this.logger.debug("adding assign "+assign.code+" without rules");
      list.push(new PointList(point.pointName, "", undefined, "", ""));
    }
    // this.logger.debug("last flatMap with " + list.length);
    return Observable.from(list);
  }



  private findPointMatchesJson(schemas : Array<string>, text?: string): Observable<any> {
  let payload;
  let hasTextValue = text && text.length >= 1;

    if (schemas && schemas.length >= 1) {
      let withSchemaText = hasTextValue ? 
                            this.filterRules.replace(/##RULE_CLAUSE##/g, this.queryRule.replace(/##TO_REPLACE##/g, text)) 
                            : ""; 

      payload = this.pointsBySchemaQuery
                    .replace(/##MATCH_CLAUSES##/g, this.createSchemaQueryClauses(schemas))
                    .replace(/##FILTER_RULES_CLAUSE##/g, withSchemaText)
                    .replace(/##RANGE_SORT_CLAUSE##/g, this.fromSortClauses);

                      
    } else if (hasTextValue) {
      payload = this.query
                      .replace(/##RULE_CLAUSE##/g, this.queryRule.replace(/##TO_REPLACE##/g, text))
                      .replace(/##RANGE_SORT_CLAUSE##/g, this.fromSortClauses);
    } else {
      payload = '{"sort": [{"pointName": {"order": "asc"}}, {"rules.sequence": {"order": "desc"}}]}';
      // return this.http.post(this.config.get("esBackUrl") + "/routing/points/_search?size=100", '{"sort": [{"pointName": {"order": "asc"}}, {"rules.sequence": {"order": "desc"}}]}').map(res => res.json());

    }
      //this.logger.debug("findPointMatches payload "+ payload);
      return this.http.post(this.config.get("esBackUrl") + "/routing/points/_search?size=1000", payload).map(res => res.json());
  }


  findPointByName(pointName: string): Observable<any> {
    return this.http.get(this.config.get("esBackUrl") + "/routing/points/" + pointName)
      .map(res => res.json())
      .map(res => {
        // this.logger.debug("from 1st call "+JSON.stringify(res));
        return { "found": res.found, "source": res._source }
      })
  }

  findRuleByPointAndSequence(pointName: string, sequence: number): Observable<any> {
    return this.findPointByName(pointName)
      .flatMap(
      (res) => {
        //this.logger.debug("from call "+JSON.stringify(res));
        if (res.found) {
          let found = [];
          let ruleBySequenceFound = res.source.rules.find(rule => rule.sequence == sequence);
          if (ruleBySequenceFound) {
            found.push({ "found": true, "value": ruleBySequenceFound });
          }
          this.logger.log("ruleBySequence " + found);
          return Observable.from(found);
        } else {
          //TODO: check how to return an error
          this.logger.error("no point found ");
          return Observable.from([{ "found": true, "value": {} }]);
        }

      }
      );
  }

  private toRules(point): Observable<Rule> {
    let list: Array<Rule> = [];

    if (point.rules.length > 0) {

      point.rules.map(rule => {
        if (rule.schemas && rule.schemas.length > 0) {
          //TODO change schemas from string to array
        }
        list.push(new Rule(rule.sequence, rule.routingPoint, rule.description, rule.schemas, undefined, undefined, rule.condition, rule.action));
      });

    }
    // this.logger.debug("last flatMap with " + list.length);
    return Observable.from(list);
  }

  private createSchemaQueryClauses(schemas : Array<string>) : string {
    if (!schemas || schemas.length <= 0) {
      return "";
    }

   return schemas.map(schema => this.schemaClause.replace(/##TO_REPLACE##/g, schema)).join(",")
  }

  private findOne(source : Array<string>, target : Array<string>) : boolean {
    if (!source || source.length == 0 || !target || target.length == 0) {
      return false;
    }

    return target.some(e => source.indexOf(e) != -1);
  }

  findRulesBySchemas(schemas: Array<string>, filter: boolean = true): Observable<Array<Rule>> {
    this.logger.log(">> looking for rules with schemas " + JSON.stringify(schemas));

    let payload = this.pointsBySchemaQuery
                    .replace(/##MATCH_CLAUSES##/g, this.createSchemaQueryClauses(schemas))
                    .replace(/##FILTER_RULES_CLAUSE##/g,"")
                    .replace(/##RANGE_SORT_CLAUSE##/g, this.fromSortClauses);

    //this.logger.debug("findPointMatches payload "+ payload);
    let allRules = this.http.post(this.config.get("esBackUrl") + "/routing/points/_search?size=1000", payload)
      .map(res => res.json())
      .flatMap(this.toHits)
      .flatMap(this.toRules);

    if (filter) {
      allRules = allRules.filter(rule => {
        let schemaFound = this.findOne(rule.schemas, schemas);
        this.logger.debug("in filter rules: " + rule.schemas + " schemaFound = " + schemaFound);
        return schemaFound;
      });
    }

    return allRules.reduce((acc, R) => { acc.push(R); return acc }, new Array<Rule>());;
  }

  findRuleByCode(code: string): Observable<any> {
    this.logger.log(">> looking for " + code);
    let found = new AMHRule("", "");
    return this.findRules().flatMap(
      (data) => {
        let rule = SAARoutingService.getFromSource(data).find(r => {
          return r.code.toLowerCase() == (code ? code.toLocaleLowerCase() : '');
        });
        this.logger.log(">> elastic return  " + rule);
        if (rule) {
          return Observable.create(observer => {
            observer.next(rule);
            observer.complete();
            // Any cleanup logic might go here
            return () => this.logger.log('disposed found')
          });
        } else {
          return Observable.create(observer => {
            observer.next(new AMHRule("", ""));
            this.logger.log("returning to observer code=empty");
            observer.complete();
            // Any cleanup logic might go here
            return () => this.logger.log('disposed not found')
          });
        }
      }
    );
    // return Observable.from(this.findAMHRuleByCode(code));
  }

  findRules(): Observable<any> {
    return this.http.get(this.config.get("esBackUrl") + "/amhrouting/rules/_search?size=1000").map(res => res.json());
  }

  findBackends(): Observable<any> {
    this.logger.log("findBackends url = " + this.config.get("esBackUrl"));
    return this.http.get(this.config.get("esBackUrl") + "/amhrouting/backends/_search?size=1000").map(res => res.json());
  }


  // findAssignmentsBySequence(sequence: number): Observable<any> {
  //   return this.findAssignments().flatMap(
  //     (data) => {
  //       let assignments = SAARoutingService.getFromSource(data);
  //       let found=[];
  //       let assignBySequenceFound = assignments.find(assignment => assignment.sequence == sequence);
  //       if (assignBySequenceFound) {
  //         found.push(assignBySequenceFound);
  //       }
  //       this.logger.log("assignmentBySequence "+ found);
  //       return Observable.from(found);
  //     }
  //   );

  // }

  // findAssignmentsByCode(code: string): Observable<any> {
  //   return this.findAssignments().flatMap(
  //     (data) => {
  //       let assignments = SAARoutingService.getFromSource(data);
  //       let found=[];
  //       let assignByCodeFound = assignments.find(assignment => assignment.code == code);
  //       if (assignByCodeFound) {
  //         found.push(assignByCodeFound);
  //       }
  //       this.logger.log("assignmentByCode "+ found);
  //       return Observable.from(found);
  //     }
  //   );

  // }

  // findAssignmentsByBackendPK(pk: BackendPK): Observable<any> {
  //   return this.findAssignments().flatMap(
  //     (data) => {
  //       let assignments = SAARoutingService.getFromSource(data);
  //       let found=[];
  //       for (var i = 0; i < assignments.length; i++) {
  //         var existingBackend = assignments[i];
  //         if (existingBackend.backendPrimaryKey.code === pk.code && existingBackend.backendPrimaryKey.direction === pk.direction) {
  //           found.push(existingBackend);
  //           break;
  //         }
  //       }
  //       this.logger.log("[flatMap] returning "+ found);
  //       return Observable.from(found);
  //     }
  //   );

  // }

  saveRule(env: string, version: string, rule: Rule): Observable<any> {
    let headers = new Headers({ 'Content-Type': 'application/json' });
    let options = new RequestOptions({ headers: headers });

    let payload: string = JSON.stringify(rule.toWriteModel(env, version));
    this.logger.log(" save rule sent: " + payload);
    return this.http.put(this.config.get("saaBackUrl") + "/points/" + rule.routingPoint + "/rules/" + rule.sequence, payload, options).map(res => { this.logger.log(" from save " + res.json()); res.json(); })
  }

  createRule(env: string, version: string, rule: Rule): Observable<any> {
    let headers = new Headers({ 'Content-Type': 'application/json' });
    let options = new RequestOptions({ headers: headers });

    let payload: string = JSON.stringify(rule.toWriteModel(env, version));
    this.logger.log(" create rule sent: " + payload);
    return this.http.post(this.config.get("saaBackUrl") + "/points/" + rule.routingPoint + "/rules/" + rule.sequence, payload, options).map(res => { this.logger.log(" from create " + res.json()); res.json(); })
  }

  saveAssignment(assignment: Assignment): Observable<any> {
    let headers = new Headers({ 'Content-Type': 'application/json' });
    let options = new RequestOptions({ headers: headers });

    let payload: string = this.assignToWriteModel(assignment);
    this.logger.log(" save assignment sent: " + payload);
    return this.http.put(this.config.get("saaBackUrl") + "/amhrouting/assignments/" + assignment.code, payload, options).map(res => { this.logger.log(" from save " + res.json()); res.json(); })
  }

  createAssignment(assignment: Assignment): Observable<any> {
    let headers = new Headers({ 'Content-Type': 'application/json' });
    let options = new RequestOptions({ headers: headers });

    let payload: string = this.assignToWriteModel(assignment);
    this.logger.log(" create assignment sent: " + payload);
    return this.http.post(this.config.get("saaBackUrl") + "/amhrouting/assignments/" + assignment.code, payload, options).map(res => { this.logger.log(" from create " + res.json()); return res.json(); })
  }

  export(env: string, version: string, fileName: string): Observable<any> {
    let headers = new Headers({ 'Content-Type': 'application/json' });
    let options = new RequestOptions({ headers: headers });

    let payload: string = JSON.stringify({
      filePath: "useless",
      fileName: fileName,
      env: env,
      version: version
    });
    this.logger.log(" export sent to SAA: " + payload);
    return this.http.post(this.config.get("saaBackUrl") + "/routing/export", payload, options)
      .map(res => {
        this.logger.log(" from export " + res.json().response);
        return res.json();
      });

  }

  getExportedFile(fileName: string): Observable<any> {
    return this.http.get(this.config.get("saaBackUrl") + "/routing/export/" + fileName).map(res => res.json());
  }

  import(filePath: string): Observable<any> {
    let headers = new Headers({ 'Content-Type': 'application/json' });
    let options = new RequestOptions({ headers: headers });

    let payload: string = JSON.stringify({
      filePath: filePath
    });
    this.logger.log(" import sent: " + payload);
    return this.http.post(this.config.get("saaBackUrl") + "/routing/import", payload, options).map(res => { this.logger.log(" from import " + res.json()); return res.json(); })
  }

  private assignToWriteModel(assignment: Assignment): string {

    let writeModelObj = {
      sequence: assignment.sequence,
      backCode: assignment.backendPrimaryKey.code,
      backDirection: assignment.backendPrimaryKey.direction,
      active: String(assignment.active),
      dataOwner: assignment.dataOwner,
      lockCode: assignment.lockCode,
      description: assignment.description,
      rules: []
    };

    let rules = assignment.rules || [];
    rules.forEach(rule => {
      let ruleWriteModel = this.assigRuleToModel(assignment.code, rule);
      writeModelObj.rules.push(ruleWriteModel);
    });

    return JSON.stringify(writeModelObj);
  }

  //  code: String, sequence: Long, ruleCode: String,
  //                                    dataOwner: Option[String], lockCode: Option[String],
  //                                    environment: Option[String] = None, //lastModification: Option[Date] = None, creationDate: Option[Date] = None,
  //                                    createdBy: Option[String] = None, lastModifiedBy: Option[String] = None, version: Option[Long] = None


  //  public code: string;
  //   public dataOwner: string;
  //   public lockCode: string;
  //   public sequence: number;
  //   public expression:string;

  private assigRuleToModel(backendCode: string, assignmentRule: AssignmentRule): any {
    this.logger.log("assigRuleToModel with code " + backendCode + " assignmentRule.sequence " + assignmentRule.sequence
      + " assignmentRule.code " + assignmentRule.code + " assignmentRule.dataOwner " + assignmentRule.dataOwner + " assignmentRule.lockCode " + assignmentRule.lockCode);
    return {
      code: backendCode,
      sequence: assignmentRule.sequence,
      ruleCode: assignmentRule.code,
      dataOwner: assignmentRule.dataOwner,
      lockCode: assignmentRule.lockCode
    };
  }

  static getFromSource(data: any): (Array<any>) {
    let hitArray = data['hits'].hits;
    let resp = hitArray.map(s => s._source)
    return resp;
  }

  private toHits(match): Observable<any> {
    return Observable.from(match['hits'].hits)
      .map(hit => hit["_source"])
  }

  //TODO: check how to pass 'this' in the context to user this.logger instead of console
  private toFoundHits(match): Observable<any> {
    let totalHits = match.hits.total;
    //console.debug("totalHits : "+totalHits);
    if (totalHits > 0) {
      let hits = match['hits'].hits.map(hit => hit["_source"]);
      //console.debug("from  "+ JSON.stringify(hits));
      return Observable.from([{ "found": true, "hits": hits }]);
    } else {
      //console.debug("returning empty hits[] ");
      return Observable.from([{ "found": false, "hits": [] }]);
    }
  }

  findSchemas(): Observable<Array<Option>> {
    this.logger.log("findSchemas url = " + this.config.get("esBackUrl"));
    return this.http.get(this.config.get("esBackUrl") + "/routing/schemas/_search?size=1000")
      .map(res => res.json())
      .flatMap(this.toFoundHits)
      .map(resp => { return resp.hits.map(hit => { return new Option(1, hit['name'], hit['name']); }); });
  }

}