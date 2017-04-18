import {Headers, RequestOptions, Http} from 'angular2/http';
import {Injectable} from 'angular2/core';
import {Observable} from 'rxjs/Observable';
import {AssignType, AssignmentUnique, AssignmentUniqueRule, AssignmentList, AMHRule, Backend, BackendPK} from '../../models/routing-amh';
import {FileDownloader, Store, Config, Logger} from '../../common/components/services';
import {User} from '../../models/users';

@Injectable()
export class AMHAssignmentService {
  private environment = "UNKNOWN";
  private version = "DEFAULT";
  
  private persistence : any;
  constructor(private http: Http, private config: Config, private store: Store, private fileDownloader : FileDownloader
  ,private logger : Logger) {
    this.persistence = sessionStorage; 
  }

  private getAssignmentPath(type : AssignType) : string {
    switch(type) {
      case AssignType.BK_CHANNEL:
        return "assignments";
      case AssignType.DTN_COPY:
        return "distributionCopies";
      case AssignType.FEED_DTN_COPY:
        return "feedbackDtnCopies";
      default:
      return "assignments";
    }
  }

  findAssignmentByCode(type : AssignType, code: string): Observable<any> {
    return this.http.get(this.config.get("esBackUrl")+"/amhrouting/"+this.getAssignmentPath(type)+"/"+code).map(res => res.json())
      .map(res => res.found ? res._source : res);
  }
  
  findAssignments(type : AssignType): Observable<any> {
    return this.http.post(this.config.get("esBackUrl")+"/amhrouting/"+this.getAssignmentPath(type)+"/_search?size=1000",'{"sort": [{"sequence": {"order": "asc"}}]}').map(res => res.json());
  }

  findRuleByCode(code: string): Observable<any> {
    this.logger.log(">> looking for " + code);
    let found=new AMHRule("","");
    return this.findRules().flatMap(
      (data) => {
        let rule = AMHAssignmentService.getFromSource(data).find(r => {
          return r.code.toLowerCase() == (code ? code.toLocaleLowerCase() : '');
        });
        this.logger.log(">> elastic return  " + rule );
        if (rule) {
           return Observable.create(observer => {
                        observer.next(rule);
                        observer.complete();
                        // Any cleanup logic might go here
                        return () => this.logger.log('disposed found')
                      });
        } else {
          return Observable.create(observer => {
                        observer.next(new AMHRule("","") );
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
    return this.http.get(this.config.get("esBackUrl")+"/amhrouting/rules/_search?size=100").map(res => res.json());
  }

  findBackends(): Observable<any> {
    this.logger.log("findBackends url = " + this.config.get("esBackUrl"));
    return this.http.get(this.config.get("esBackUrl")+"/amhrouting/backends/_search?size=100").map(res => res.json());
  }


  findAssignmentsBySequence(type: AssignType, sequence: number): Observable<any> {
    return this.findAssignments(type).flatMap(
      (data) => {
        let assignments = AMHAssignmentService.getFromSource(data);
        let found=[];
        let assignBySequenceFound = assignments.find(assignment => assignment.sequence == sequence);
        if (assignBySequenceFound) {
          found.push(assignBySequenceFound);
        }
        this.logger.log("assignmentBySequence "+ found);
        return Observable.from(found);
      }
    );

  }

  findAssignmentsByCode(type: AssignType, code: string): Observable<any> {
    return this.findAssignments(type).flatMap(
      (data) => {
        let assignments = AMHAssignmentService.getFromSource(data);
        let found=[];
        let assignByCodeFound = assignments.find(assignment => assignment.code == code);
        if (assignByCodeFound) {
          found.push(assignByCodeFound);
        }
        this.logger.log("assignmentByCode "+ found);
        return Observable.from(found);
      }
    );

  }
  
  findAssignmentsByBackendPK(type: AssignType, pk: BackendPK): Observable<any> {
    return this.findAssignments(type).flatMap(
      (data) => {
        let assignments = AMHAssignmentService.getFromSource(data);
        let found=[];
        for (var i = 0; i < assignments.length; i++) {
          var existingBackend = assignments[i];
          if (existingBackend.backendPrimaryKey.code === pk.code && existingBackend.backendPrimaryKey.direction === pk.direction) {
            found.push(existingBackend);
            break;
          }
        }
        this.logger.log("[flatMap] returning "+ found);
        return Observable.from(found);
      }
    );

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
  
  saveAssignment(type: AssignType, assignment : AssignmentUnique, user : User) : Observable<any> {
    let headers = new Headers({ 'Content-Type': 'application/json',
  'userId': user.username,
        'time': Date.now() });
    let options = new RequestOptions({ headers: headers });
            
    let payload : string = JSON.stringify(assignment.toWriteModel(type, this.store.getCurrentEnv(), this.store.getCurrentVersion(), 
                             user.username, String(Date.now())), this.replacerFor(["modificationUserId", "creationUserId"]));
                             // let payload = JSON.stringify(job.toWriteModel(), this.replacerFor(["user"]));
    this.logger.log(" save assignment sent: "+ payload);
    return this.http.put(this.config.get("backUrl")+"/amhrouting/"+this.assignmentTypePath(type)+"/"+assignment.code, payload, options).map(res =>  { this.logger.log(" from save "+res.json()); res.json(); })
  }

 createAssignment(type: AssignType, assignment : AssignmentUnique, user : User) : Observable<any> {
    let headers = new Headers({ 'Content-Type': 'application/json',
  'userId': user.username,
        'time': Date.now() });
    let options = new RequestOptions({ headers: headers });            
    
    let payload : string = JSON.stringify(assignment.toWriteModel(type, this.store.getCurrentEnv(), this.store.getCurrentVersion(), 
                            user.username, String(Date.now()), user.username, String(Date.now())), this.replacerFor(["modificationUserId", "creationUserId"]));
    this.logger.log(" create assignment sent: "+ payload);
    return this.http.post(this.config.get("backUrl")+"/amhrouting/"+this.assignmentTypePath(type)+"/"+assignment.code, payload, options).map(res =>  { this.logger.log(" from create "+res.json()); return res.json(); })
  }

  private assignmentTypePath(type : AssignType) : string {
    switch(type) {
      case AssignType.BK_CHANNEL:
      return "assignments";
      case AssignType.DTN_COPY:
      return "distributions";
      case AssignType.FEED_DTN_COPY:
      return "feedbacks";

    }
  } 
  static getFromSource(data : any)  : Array<any> {
    let hitArray = data['hits'].hits;
    let resp = hitArray.map( s => s._source)
    return resp;  
  }

  // private findAssignmentByPK(type: AssignType, pk: BackendPK): (Array<AssignmentUnique>) {
  //   this.findAssignments(type).subscribe(
  //     data => {
  //       this.logger.log("RETURNING DATA %s", data);
  //       if (Array.isArray(data)) {
  //         this.logger.log("Array " + data);
  //       } else {
  //         let assignments = AMHAssignmentService.getFromSource(data);
  //         this.logger.log("Data retrieved from findAssignmentByPK: %s ", assignments);
  //         for (var i = 0; i < assignments.length; i++) {
  //           var existingBackend = assignments[i];
  //           this.logger.log("Existing pkCode "+existingBackend.backendPrimaryKey.code + " pkDir " + existingBackend.backendPrimaryKey.direction + " target pkCode "+ pk.code +" pkDir "+pk.direction );
  //           if (existingBackend.backendPrimaryKey.code === pk.code && existingBackend.backendPrimaryKey.direction === pk.direction) {
  //             this.logger.log("from findAssignmentByPK assignment found by Backend!! ");
  //             return [existingBackend];
  //             }
  //           }
  //        }
  //     },
  //     err => this.logger.log("Can't get assignment. Error code: %s, URL: %s ", err.status, err.url),
  //     () => this.logger.log("assignments retrieved from findAssignmentByPK")
        
  //     );

  //  this.logger.log(" NOT FOUND from findAssignmentByPK!");
  //  return [];

  // };

  //TODO:Move to different service
  public updateDirtyStatus(newValue : boolean) {
    //this.logger.debug("updating isDirty with "+newValue);
    this.persistence.setItem("amh_assignment_isDirty", newValue);
  }

  public isAssignmentStatusDirty() : boolean {
    let isDirty = this.persistence.getItem("amh_assignment_isDirty"); 
    //this.logger.debug("returning isDirty with "+isDirty);
    return "true" === isDirty;
  } 

exportOverview(assignType : AssignType, user: User): Observable<any> {
    let headers = new Headers(
      {
        'Content-Type': 'application/json',
        'userId': user.username,
        'time': Date.now()
      });
    let options = new RequestOptions({ headers: headers });
    let payload = JSON.stringify({});
    return this.http.post(this.config.get("backUrl") + "/amhrouting/csv/export/assignments/" + assignType, payload, options)
      .map(res => res.json());
  }

  downloadFile(fileNamePath : string, fileName: string) {
    this.fileDownloader.download(this.config.get("backUrl") + fileNamePath, fileName);
  }

}