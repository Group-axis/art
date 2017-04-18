import { Headers, RequestOptions, Http } from 'angular2/http';
import { Injectable } from 'angular2/core';
import { Observable } from 'rxjs/Observable';
import { Assignment, AssignmentRule, AssignmentList, AMHRule, Backend, BackendPK, AssignType } from '../models/routing-amh';
import { SimulationJob, SimulationResult } from '../models/simulation';
import { Message } from '../models/simulation';
import { User } from '../models/users';
import { Option } from '../models/referential/option';
import { TreeNode } from '../common/components/ui/widgets/tree-view';
import { Elastic4js, Config, Logger } from '../common/components/services';
import { pegjs } from '../../platform/browser';

@Injectable()
export class AMHRoutingService {
  private query: string = ` 
    {
  "query": {
    "bool": {
      "should": [
        {
          "match": {
            "name": {
              "query": "##TO_REPLACE##",
              "analyzer": "id_analyzer",
              "operator": "and"
            }
          }
        }
      ]
    }
  },
  "from": 0,
  "size": 1000,
  "sort": [
    {
      "name": {
        "order": "asc"
      }
    }
  ]
}
  `;

  private findMessageByNameQuery(name: string): string {
    return "{" + Elastic4js.query(
      Elastic4js.bool([
        Elastic4js.should([
          Elastic4js.match("name", name)
        ])
      ])
    ) + "}";
  }

  private findMessagesByGroupQuery(groupNames: Array<string>): string {
    return "{" + Elastic4js.query(
      Elastic4js.bool([
        Elastic4js.should(
          groupNames.map(groupName => {
            return Elastic4js.match("group", groupName);
          })
        )
      ])
    ) + "}";
  }

  constructor(private http: Http, private config: Config, private logger: Logger) {
    this.logger.log(" retrieving config");
    this.logger.log("simulationBackUrl value: " + config.get('simulationBackUrl'));
  }

  findAssignmentByID(productID: string): Observable<any> {
    return this.http.get('/products/${productID}').map(res => res.json());
  }

  //TODO: 
  /*
   *  + remove limit size=100 for all http request, use pagination instead  
  */

  findAssignments(): Observable<any> {
    return this.http.post(this.config.get("esBackUrl") + "/amhrouting/assignments/_search?size=1000", '{"sort": [{"sequence": {"order": "asc"}}]}').map(res => res.json());
  }

  findRuleByCode(code: string): Observable<any> {
    this.logger.log(">> looking for " + code);
    let found = new AMHRule("", "");
    return this.findRules().flatMap(
      (data) => {
        let rule = AMHRoutingService.getFromSource(data).find(r => {
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

  findMessages(): Observable<any> {
    return this.http.get(this.config.get("esBackUrl") + "/messages/amh/_search?size=1000")
      .map(res => res.json())
      .flatMap(this.toHits);
  }

  findGroupMessages(): Observable<any> {
    return this.http.get(this.config.get("esBackUrl") + "/messages/group/_search?size=100000")
      .map(res => res.json())
      .flatMap(this.toHits);
  }

  findMessage(messageId: string): Observable<Message> {
    return this.http.get(this.config.get("esBackUrl") + "/messages/amh/" + messageId)
      .map(res => res.json())
      .map(res => res._source);
  }

  private replacerFor(fields: Array<string>): (key: string, value: any) => any {

    let replacer =
      function (key: string, value: any): any {
        fields.forEach(field => {
          if (value && value[field]) {
            //this.logger.log(" [replacer] instanceof " + (value[field] instanceof Array));
            if (value[field] instanceof Array) {
              value[field] = value[field][0];
            } else {
              // this.logger.log(field + "  [replacer] is not array");
            }
          }
        });

        return value;
      };

    return replacer;
  }

  // replacer(key: any, value: any) : any {
  //   if ( value && value["userId"] ) {
  //     this.logger.log(" instanceof "+(value["userId"] instanceof Array));
  //     if ( value["userId"] instanceof Array ) {
  //        value["userId"] = value["userId"][0];
  //     } else {
  //       this.logger.log(" userId is not array");
  //     }
  //   }
  //   return value;
  // } 

  saveMessage(message: Message, user: User): Observable<any> {
    let headers = new Headers({ 'Content-Type': 'application/json' });
    let options = new RequestOptions({ headers: headers });


    let payload: string = JSON.stringify(message.toWriteModel(user.username), this.replacerFor(["userId"]));
    // let payload : string = this.convertToText(message.toWriteModel(user.username));
    this.logger.log(" save message  sent: " + payload);
    return this.http.put(this.config.get("simulationBackUrl") + "/messages/amh/" + message.id, payload, options).map(res => { this.logger.log(" from message save " + res.json()); res.json(); })
  }

  createMessage(message: Message, user: User): Observable<any> {
    let headers = new Headers({ 'Content-Type': 'application/json' });
    let options = new RequestOptions({ headers: headers });

    let payload: string = JSON.stringify(message.toWriteModel(user.username), this.replacerFor(["userId"]));
    this.logger.log(" create message sent: " + payload);
    return this.http.post(this.config.get("simulationBackUrl") + "/messages/amh", payload, options).map(res => { this.logger.log(" from message create " + res.json()); res.json(); })
  }

  deleteMessages(ids: Array<string>, user: User, groupId?: string): Observable<any> {
    let headers = new Headers(
      {
        'Content-Type': 'application/json',
        'userId': user.username,
        'time': Date.now(),
        'groupId': groupId,
        'ids': ids.join(";")
      });
    let options = new RequestOptions({ headers: headers });

    return this.http.delete(this.config.get("simulationBackUrl") + "/messages/amh", options).map(res => { this.logger.log(" from message delete " + res.json()); res.json(); })
  }

  findMessageMatches(text?: string): Observable<Array<Message>> {
    return this.findMessageMatchesJson(text)
      .flatMap(this.toHits)
      .reduce((acc, R) => { acc.push(R); return acc; }, new Array<Message>());
  }

  findMessageByName(name: string): Observable<any> {
    let payload = this.findMessageByNameQuery(name);
    return this.http.post(this.config.get("esBackUrl") + "/messages/amh/_search", payload)
      .map(res => res.json())
      .flatMap(this.toFoundHits)
      .map(resp => {
        if (resp.found) {
          let message = resp.hits.find(hit => hit.name.toLowerCase() == name.toLowerCase());
          if (message) {
            return { "found": true, "message": message };
          }
        }
        return { "found": false };
      });
  }

  loadFlatGroupMessages(groupNames: Array<string> = [], indexName: string = "messages/group"): Observable<any> {
    if (groupNames.length == 0) {
      // console.debug("returning found false");
      return Observable.from([{ "found": false }]);
    }

    let payload = this.findMessagesByGroupQuery(groupNames);

    return this.http.post(this.config.get("esBackUrl") + "/" + indexName + "/_search?size=200000", payload)
      .map(res => res.json())
      .flatMap(this.toFoundHits)
      .map(resp => { return resp.found ? { "found": true, "messages": resp.hits } : { "found": false }; });

  }

  loadGroupMessages(groupNames: Array<string> = [], indexName: string = "messages/group"): Observable<any> {
    let payload = groupNames.length > 0 ? this.findMessagesByGroupQuery(groupNames) : "{}";

    return this.http.post(this.config.get("esBackUrl") + "/" + indexName + "/_search?size=200000", payload)
      .map(res => res.json())
      .flatMap(this.toFoundHits)
      .map(resp => {
        if (resp.found) {
          let messages = resp.hits.reduce(function (mapped, current) {

            if (mapped.get(current.group) == undefined) {
              mapped = mapped.set(current.group, [current]);
            } else {
              //   console.debug("current.group "+current.group+ " != undefined actual size "+mapped.get(current.group));
              mapped.get(current.group).push(current);
            }
            //console.debug("mapped ["+current.group+"] ="+mapped.get(current.group));  
            return mapped;
          }, new Map<string, any[]>());
          return { "found": true, "messages": messages };
        }

        return { "found": false };
      });

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

  private findMessageMatchesJson(text?: string): Observable<any> {
    if (text && text.length > 1) { //BUGFIX- show all messages when no filter text
      let payload = this.query.replace(/##TO_REPLACE##/g, text);
      //this.logger.debug("findPointMatches payload "+ payload);
      return this.http.post(this.config.get("esBackUrl") + "/messages/amh/_search?size=1000", payload).map(res => res.json());
    } else {
      return this.http.post(this.config.get("esBackUrl") + "/messages/amh/_search?size=100", '{"sort": [{"name": {"order": "asc"}}]}').map(res => res.json());
    }

  }


  findJobs(user: User, launcherId : number,  instantLimit?: number, jobStatus?: number): Observable<SimulationJob> {
    let headers = new Headers(
      {
        'Content-Type': 'application/json',
        'userId': user.username,
        'time': Date.now(),
        'job_status': jobStatus,
        'job_threshold': instantLimit,
        'job_launcher': launcherId
      });
    let options = new RequestOptions({ headers: headers });
    return this.http.get(this.config.get("simulationBackUrl") + "/jobs/amh/users/" + user.username, options)
      .map(res => res.json())
      .flatMap(res => {
        let cjobs: Array<SimulationJob> = res.jobs
          .map(job => {
            let s: SimulationJob = SimulationJob.fromJson(job);
            return s;
          });
        return cjobs;
      });
  }

  createJob(job: SimulationJob, user: User): Observable<any> {
    let headers = new Headers(
      {
        'Content-Type': 'application/json',
        'userId': user.username,
        'time': Date.now()
      });
    let options = new RequestOptions({ headers: headers });
    let payload = JSON.stringify(job.toWriteModel(), this.replacerFor(["user"]));
    return this.http.post(this.config.get("simulationBackUrl") + "/jobs/amh", payload, options)
      .map(res => res.json());
  }

  cancelJob(jobId: number, user: User): Observable<any> {
    let headers = new Headers(
      {
        'Content-Type': 'application/json',
        'userId': user.username,
        'time': Date.now()
      });
    let options = new RequestOptions({ headers: headers });
    //let payload = JSON.stringify(job.toWriteModel());
    return this.http.delete(this.config.get("simulationBackUrl") + "/jobs/amh/" + jobId, options)
      .map(res => res.json());
  }

  reExecuteJob(jobId: number, user: User): Observable<any> {
    let headers = new Headers(
      {
        'Content-Type': 'application/json',
        'userId': user.username,
        'time': Date.now()
      });
    let options = new RequestOptions({ headers: headers });
    let payload = JSON.stringify({});
    return this.http.put(this.config.get("simulationBackUrl") + "/jobs/amh/" + jobId, payload, options)
      .map(res => res.json());
  }

  exportJobResults(jobId: number, user: User): Observable<any> {
    let headers = new Headers(
      {
        'Content-Type': 'application/json',
        'userId': user.username,
        'time': Date.now()
      });
    let options = new RequestOptions({ headers: headers });
    let payload = JSON.stringify({});
    return this.http.post(this.config.get("simulationBackUrl") + "/jobs/amh/export/" + jobId, payload, options)
      .map(res => res.json());
  }

  loadAMHRuleGrammar(fileName: string): Observable<any> {
    return this.http.get(this.config.get("simulationBackUrl") + "/jobs/amh/export/" + fileName)
      .map(data => data.text())
      .map(fileContent => {
        let methodName = (typeof pegjs.generate === 'function') ? 'generate' : 'buildParser';
        try {
          return pegjs[methodName](fileContent);
        } catch (e) {
          console.error(e.message);
        }
      });
  }

  exportSimulationResults(results: Array<SimulationResult>, user: User) {
    /*
    Hit(fileName : String, selectionSequence : Long, selectionCode : String,
               ruleSequence : Long, ruleName : String, ruleExpression : String,
               backendSequences : String, backendNames : String) {
    */
    let headers = new Headers(
      {
        'Content-Type': 'application/json',
        'userId': user.username,
        'time': Date.now()
      });
    let options = new RequestOptions({ headers: headers });

    let hits = results.map(res => res.toWriteModel())
    let payload = JSON.stringify(hits);
    return this.http.post(this.config.get("simulationBackUrl") + "/jobs/amh/export", payload, options)
      .map(res => res.json());

  }

  private toHits(match): Observable<any> {
    return Observable.from(match['hits'].hits)
      .map(hit => hit["_source"])
  }

  findBackends(): Observable<any> {
    this.logger.log("findBackends url = " + this.config.get("esBackUrl"));
    return this.http.get(this.config.get("esBackUrl") + "/amhrouting/backends/_search?size=1000").map(res => res.json());
  }


  findAssignmentsBySequence(sequence: number): Observable<any> {
    return this.findAssignments().flatMap(
      (data) => {
        let assignments = AMHRoutingService.getFromSource(data);
        let found = [];
        let assignBySequenceFound = assignments.find(assignment => assignment.sequence == sequence);
        if (assignBySequenceFound) {
          found.push(assignBySequenceFound);
        }
        this.logger.log("assignmentBySequence " + found);
        return Observable.from(found);
      }
    );

  }

  findAssignmentsByCode(code: string): Observable<any> {
    return this.findAssignments().flatMap(
      (data) => {
        let assignments = AMHRoutingService.getFromSource(data);
        let found = [];
        let assignByCodeFound = assignments.find(assignment => assignment.code == code);
        if (assignByCodeFound) {
          found.push(assignByCodeFound);
        }
        this.logger.log("assignmentByCode " + found);
        return Observable.from(found);
      }
    );

  }

  findAssignmentsByBackendPK(pk: BackendPK): Observable<any> {
    return this.findAssignments().flatMap(
      (data) => {
        let assignments = AMHRoutingService.getFromSource(data);
        let found = [];
        for (var i = 0; i < assignments.length; i++) {
          var existingBackend = assignments[i];
          if (existingBackend.backendPrimaryKey.code === pk.code && existingBackend.backendPrimaryKey.direction === pk.direction) {
            found.push(existingBackend);
            break;
          }
        }
        this.logger.log("[flatMap] returning " + found);
        return Observable.from(found);
      }
    );

  }

  findAllCriterias(): Observable<TreeNode> {
    this.logger.log("returning findAllCriterias...");
    return this.http.get(this.config.get("esBackUrl") + "/amhreference/criteria/_search?size=1000")
      .map(res => res.json())
      .flatMap(this.toHits)
      .map(tree => TreeNode.fromJson(tree));

  }

  saveRule(rule: AMHRule, user: User): Observable<any> {
    let headers = new Headers({
      'Content-Type': 'application/json',
      'userId': user.username,
      'time': Date.now()
    });
    let options = new RequestOptions({ headers: headers });

    let payload: string = JSON.stringify(rule.addAuditValue(user.username, String(Date.now())).toWriteModel());
    this.logger.log(" save rule sent: " + payload);
    return this.http.put(this.config.get("backUrl") + "/amhrouting/rules/" + rule.code, payload, options).map(res => { this.logger.log(" from save " + res.json()); res.json(); })
  }

  createRule(rule: AMHRule, user: User): Observable<any> {
    let headers = new Headers({
      'Content-Type': 'application/json',
      'userId': user.username,
      'time': Date.now()
    });
    let options = new RequestOptions({ headers: headers });

    let payload: string = JSON.stringify(rule.addAuditValue(user.username, String(Date.now()), user.username, String(Date.now())).toWriteModel());
    this.logger.log(" create rule sent: " + payload);
    return this.http.post(this.config.get("backUrl") + "/amhrouting/rules/" + rule.code, payload, options).map(res => { this.logger.log(" from create " + res.json()); res.json(); })
  }

  saveAssignment(assignment: Assignment, user: User): Observable<any> {
    let headers = new Headers({
      'Content-Type': 'application/json',
      'userId': user.username,
      'time': Date.now()
    });
    let options = new RequestOptions({ headers: headers });

    let payload: string = this.assignToWriteModel(assignment, user.username, String(Date.now()));
    this.logger.log(" save assignment sent: " + payload);
    return this.http.put(this.config.get("backUrl") + "/amhrouting/assignments/" + assignment.code, payload, options).map(res => { this.logger.log(" from save " + res.json()); res.json(); })
  }

  createAssignment(assignment: Assignment, user: User): Observable<any> {
    let headers = new Headers({
      'Content-Type': 'application/json',
      'userId': user.username,
      'time': Date.now()
    });
    let options = new RequestOptions({ headers: headers });

    let payload: string = this.assignToWriteModel(assignment, user.username, String(Date.now()), user.username, String(Date.now()));
    this.logger.log(" create assignment sent: " + payload);
    return this.http.post(this.config.get("backUrl") + "/amhrouting/assignments/" + assignment.code, payload, options).map(res => { this.logger.log(" from create " + res.json()); return res.json(); })
  }

  export(env: string, version: string, fileName: string, user: User): Observable<any> {
    let headers = new Headers({ 'Content-Type': 'application/json' });
    let options = new RequestOptions({ headers: headers });

    let payload: string = JSON.stringify({
      filePath: "useless",
      fileName: fileName,
      env: env,
      version: version,
      "time": String(Date.now()),
      "userId": user.username
    }, this.replacerFor(["userId"]));
    //  let payload: string = JSON.stringify(message.toWriteModel(user.username), this.replacerFor(["userId"]));
    this.logger.log(" export sent: " + payload);
    return this.http.post(this.config.get("backUrl") + "/amhrouting/export", payload, options)
      .map(res => {
        this.logger.log(" from export " + res.json().response);
        return res.json();
      });
  }

  getExportedFile(fileName: string): Observable<any> {
    return this.http.get(this.config.get("backUrl") + "/amhrouting/export/" + fileName).map(res => res.json());
  }

  import(filePath: string): Observable<any> {
    let headers = new Headers({ 'Content-Type': 'application/json' });
    let options = new RequestOptions({ headers: headers });

    let payload: string = JSON.stringify({
      filePath: filePath
    });
    this.logger.log(" import sent: " + payload);
    return this.http.post(this.config.get("backUrl") + "/amhrouting/import", payload, options).map(res => { this.logger.log(" from import " + res.json()); return res.json(); })
  }

  private assignToWriteModel(assignment: Assignment, modificationUserId: string, modificationDate: string, creationUserId?: string, creationDate?: string): string {

    let writeModelObj = {
      sequence: assignment.sequence,
      backCode: assignment.backendPrimaryKey.code,
      backDirection: assignment.backendPrimaryKey.direction,
      active: String(assignment.active),
      dataOwner: assignment.dataOwner,
      lockCode: assignment.lockCode,
      description: assignment.description,
      modificationUserId: modificationUserId,
      modificationDate: modificationDate,
      creationUserId: creationUserId,
      creationDate: creationDate,
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

  private criterias: Array<TreeNode> = [
    new TreeNode("transaction", "Transaction", "Transaction", [
      new TreeNode("transactiondirection", "direction", "Direction"),
      new TreeNode("transactionreceiveraddress", "receiverAddress", "Receiver Address"),
      new TreeNode("transactionmessagetype", "messageType/code", "Message Type"),
      new TreeNode("transactionswiftparameters", "SwiftParameters", "Swift Parameters", [
        new TreeNode("swiftparametersrequestreference", "swiftParameters/requestReference", "Request Reference"),
        new TreeNode("swiftparametersservice", "swiftParameters/service", "Service")
      ]),
      new TreeNode("transactionsenderaddress", "senderAddress", "Sender Address"),
      /*new TreeNode("transactionadditionalparameterslabel","AdditionalParameters","Additional Parameters"),
      new TreeNode("transactionamlstatuslabel","AmlStatus","Aml Status"),
      new TreeNode("transactionarchivedlabel","Archived","Archived"),
      new TreeNode("transactionbackendchannellabel","BackendChannel","Backend Channel"),
      new TreeNode("transactionbulkdetailslabel","BulkDetails","Bulk Details"),
      new TreeNode("transactionbulkfiletransactionlabel","BulkFileTransaction","Bulk File Transaction"),
      new TreeNode("transactionbusinessdetailslabel","BusinessDetails","Business Details"),
      new TreeNode("transactionbusinessstatuslabel","BusinessStatus","Business Status"),
      new TreeNode("transactioncommunicationparameterslabel","CommunicationParameters","Communication Parameters"),
      new TreeNode("transactioncommunicationprioritylevellabel","CommunicationPriorityLevel","Communication Priority Level"),
      new TreeNode("transactioncreationdatelabel","CreationDate","Creation Date"),
      new TreeNode("transactioncustomerdetailslabel","CustomerDetails","Customer Details"),
      new TreeNode("transactiondatedeliveredlabel","DateDelivered","Date Delivered"),
      new TreeNode("transactiondatereceivedlabel","DateReceived","Date Received"),
      new TreeNode("transactiondecisionlabel","Decision","Decision"),
      new TreeNode("transactiondecisionmakerlabel","DecisionMaker","Decision Maker"),
      new TreeNode("transactiondocumentlabel","Document","Document"),
      new TreeNode("transactionduplicatecheckoverridelabel","DuplicateCheckOverride","Duplicate Check Override"),
      new TreeNode("transactionextractedfieldslabel","ExtractedFields","Extracted Fields"),
      */
      new TreeNode("transactionfinparameterslabel", "FINParameters/messageType", "FIN Parameters"),
    /*new TreeNode("transactionfieldextractionprofilelabel","FieldExtractionProfile","Field Extraction Profile"),
    new TreeNode("transactionfiletypeparameterslabel","FileTypeParameters","File Type Parameters"),
    new TreeNode("transactionflexfieldslabel","FlexFields","Flex Fields"),
    new TreeNode("transactionippcontrollabel","IPPControl","IPP control"),
    new TreeNode("transactionippoidlabel","IPPOID","IPP OID"),
    new TreeNode("transactioninstructionlabel","Instruction","Instruction"),
    new TreeNode("transactioninternalreferencelabel","InternalReference","Internal Reference"),
    new TreeNode("transactionlastmodificationdatelabel","LastModificationDate","Last Modification Date"),
    new TreeNode("transactionlastmodifiernamelabel","LastModifierName","Last Modifier Name"),
    new TreeNode("transactionlastupdatetimestamplabel","LastUpdateTimestamp","Last Update Timestamp"),
    new TreeNode("transactionlockcodelabel","LockCode","Lock Code"),
    new TreeNode("transactionlogicalnodelabel","LogicalNode","Logical Node"),
    new TreeNode("transactionmqinparameterslabel","MQInParameters","MQ In Parameters"),
    new TreeNode("transactionmqoutparameterslabel","MQOutParameters","MQ Out Parameters"),
    new TreeNode("transactionmessagecategorylabel","MessageCategory","Message Category"),
    new TreeNode("transactionmessageformatlabel","MessageFormat","Message Format"),
    new TreeNode("transactionmessagereferencelabel","MessageReference","Message Reference"),
    new TreeNode("transactionmessagetypelabel","MessageType","Message Type"),
    new TreeNode("transactionnetworkchannellabel","NetworkChannel","Network Channel"),
    new TreeNode("transactionnetworkprioritylabel","NetworkPriority","Network Priority"),
    */new TreeNode("transactionnetworkprotocollabel", "networkProtocol", "Network Protocol"),
    /*new TreeNode("transactionnextactivitylabel","NextActivity","Next Activity"),
    new TreeNode("transactiononlysendfinalacklabel","OnlySendFinalAck","Only Send Final Ack"),
    new TreeNode("transactionoriginatingapplicationlabel","OriginatingApplication","Originating Application"),
    new TreeNode("transactionpdeindicationlabel","PDEIndication","PDE Indication"),
    new TreeNode("transactionprocessprioritylabel","ProcessPriority","Process Priority"),
    new TreeNode("transactionprocessingchannellabel","ProcessingChannel","Processing Channel"),
    new TreeNode("transactionprocessingstatelabel","ProcessingState","Processing State"),
    new TreeNode("transactionprocessingtypelabel","ProcessingType","Processing Type"),
    new TreeNode("transactionreceiveraddresslabel","ReceiverAddress","Receiver Address"),
    new TreeNode("transactionresponsedocumentlabel","ResponseDocument","Response Document"),
    new TreeNode("transactionsecomparameterslabel","SECOMParameters","SECOM Parameters"),
    new TreeNode("transactionsixnetworkparameterslabel","SIXNetworkParameters","SIX Network Parameters"),
    new TreeNode("transactionstpenginelabel","STPEngine","STP Engine"),
    new TreeNode("transactionstpversionlabel","STPVersion","STP Version"),
    new TreeNode("transactionstpwbeventsequencelabel","STPWBEventSequence","STP-WB Event Sequence"),
    new TreeNode("transactionsanctionfilterdetaillabel","SanctionFilterDetail","Sanction Filter Detail"),
    new TreeNode("transactionsenderaddresslabel","SenderAddress","Sender Address"),
    new TreeNode("transactionstrandedlabel","Stranded","Stranded"),
    new TreeNode("transactiontestandtraininglabel","TestAndTraining","TestAndTraining"),
    new TreeNode("transactiontimertasklabel","TimerTask","Timer Task"),
    new TreeNode("transactiontransactionreferencelabel","TransactionReference","Transaction Reference"),
    new TreeNode("transactiontransactionstatuslabel","TransactionStatus","Transaction Status"),
    new TreeNode("transactiontransferreferencelabel","TransferReference","Transfer Reference"),
    new TreeNode("transactionworkflowlabel","Workflow","Workflow"),
    new TreeNode("transactionworkflowprogresslabel","WorkflowProgress","Workflow Progress"),
    new TreeNode("transactionlabel","label","Transaction"),
    new TreeNode("transactionounttotalnumberoftransactionslabel","TransactionCount/TotalNumberOfTransactions","Total Number Of Transactions"),
    new TreeNode("transactionountlabel","TransactionCount","Transaction Count"),
    new TreeNode("transactionountaudittotalnumberoftransactionslabel","TransactionCountAudit/TotalNumberOfTransactions","Total Number Of Transactions"),
    new TreeNode("transactionountauditlabel","TransactionCountAudit","Transaction Count Audit"),
    new TreeNode("transactionountgroupingcriteriatotalnumberoftransactionslabel","TransactionCountGroupingCriteria/TotalNumberOfTransactions","Total Number Of Transactions"),
    new TreeNode("transactioncountgroupingcriterialabel","TransactionCountGroupingCriteria","Transaction Count Grouping Criteria"),
    new TreeNode("transactioncountgroupingcriteriaaudittotalnumberoftransactionslabel","TransactionCountGroupingCriteriaAudit/TotalNumberOfTransactions","Total Number Of Transactions"),
    new TreeNode("transactioncountgroupingcriteriaauditlabel","TransactionCountGroupingCriteriaAudit","Transaction Count Grouping Criteria Audit"),
    new TreeNode("transactioneventcreationdatelabel","TransactionEvent/CreationDate","Creation Date"),
    new TreeNode("transactioneventeventcountlabel","TransactionEvent/EventCount","Event Count")
   */ ])
  ];


}