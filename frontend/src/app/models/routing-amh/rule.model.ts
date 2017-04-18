import {BaseAudit} from '../audit';

export class AMHRule extends BaseAudit {
  public code: string;
  public dataOwner: string;
  public expression: string;
  public lockCode: string;
  public type: string;
  public environment: string;
  public version: string;
  //UI fields
  public selected: boolean = false;
  
 constructor(code: string, expression: string, dataOwner?: string,  lockCode?: string, type?: string, environment?: string, version?: string) {
   super();
   this.code=code;
  this.dataOwner=dataOwner;
  this.expression=expression;
  this.lockCode=lockCode;
  this.type=type;
 }
 

public toWriteModel() : any {
   return {
     expression : this.expression,
     dataOwner : this.dataOwner,
     lockCode : this.lockCode,
     ruleType: this.type,
     environment: this.environment,
     version: this.version,
     modificationUserId : this.modificationUserId,
     modificationDate: this.modificationDate, 
     creationUserId : this.creationUserId,
    creationDate: this.creationDate 
    }
  };

 public setEnvAndVersion(environment: string, version: string) {
    this.environment = environment;
    this.version = version;
  }

  public cloneWithUpperCaseCode() : AMHRule {
    return new AMHRule(this.code.toUpperCase(), this.expression, this.dataOwner,  this.lockCode, this.type, this.environment, this.version);
  }

  public static fromJson(json : any) : AMHRule {
      return new AMHRule(json.code, json.expression, json.dataOwner, json.lockCode, json.type, json.environment, json.version); 
  }


}