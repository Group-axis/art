import {BackendPK} from './backend-pk.model';
import {AssignmentUniqueBackend} from './assignment-unique-backend.model';
import {AssignmentUniqueRule} from './assignment-unique-rule.model';
import {AssignType} from './assignment-type.model';

export class AssignmentUnique {
  public active: boolean;
  public code: string;
  public backendPrimaryKey: BackendPK;
  public dataOwner: string;
  public description: string;
  public lockCode: string;
  public sequence: number;
  public copies: number;
  public name: string;
  public environment: string;
  public version: string;
  public rules: Array<AssignmentUniqueRule>;
  public backends: Array<AssignmentUniqueBackend>;

  constructor(active?: boolean, backendPrimaryKey?: BackendPK, code?: string, dataOwner?: string, description?: string, lockCode?: string, sequence?: number, copies?: number, name?: string, environment?: string, version?: string, rules?: Array<AssignmentUniqueRule>, backends?: Array<AssignmentUniqueBackend>) {
    this.active = active;
    this.backendPrimaryKey = backendPrimaryKey;
    this.code = code;
    this.dataOwner = dataOwner;
    this.description = description;
    this.lockCode = lockCode;
    this.sequence = sequence || undefined;
    this.copies = copies || undefined;
    this.name = name;
    this.environment = environment;
    this.version = version;
    this.rules = rules || [];
    this.backends = backends || [];
  }

  public backendCodeList() : Array<string> {
    return this.backendPrimaryKey ? [this.backendPrimaryKey.code] : this.backends.map(backend => backend.code);
  }

  public toWriteModel(type: AssignType, environment: string, version: string, modificationUserId : string,  modificationDate: string, creationUserId? : string, creationDate?: string): any {
    let rules: Array<AssignmentUniqueRule> = this.rules || [];
    rules = rules.map(rule => rule.toWriteModel(type, this.code, environment, version, modificationUserId,  modificationDate, creationUserId, creationDate));
    
    let backends: Array<AssignmentUniqueBackend> = this.backends || [];
    backends = backends.map(backend => backend.toWriteModel(type, this.code, environment, version, modificationUserId,  modificationDate, creationUserId, creationDate));

    let writeModelObj = {
      sequence: this.sequence,
      active: String(this.active),
      dataOwner: this.dataOwner,
      lockCode: this.lockCode,
      description: this.description,
      environment: environment,
      version: version,
      rules: rules,
      modificationUserId : modificationUserId,
      modificationDate : modificationDate,
      creationUserId : creationUserId,
      creationDate : creationDate
    };
    

    switch (type) {
      case AssignType.BK_CHANNEL:
        
        writeModelObj["backCode"] = this.backendPrimaryKey.code;
        writeModelObj["backDirection"] = this.backendPrimaryKey.direction;
        break;
      
      case AssignType.DTN_COPY:
      case AssignType.FEED_DTN_COPY:

        writeModelObj["copies"] = this.copies;
        writeModelObj["name"] = this.name;
        writeModelObj["backends"] = backends;
        break;
    }

    return writeModelObj;
  }

  public static fromJson(json: any): AssignmentUnique {
    let rules: Array<AssignmentUniqueRule> = json.rules || [];
    let backends: Array<AssignmentUniqueBackend> = json.backends || [];

    return new AssignmentUnique(json.active, json.backendPrimaryKey, json.code, json.dataOwner, json.description, json.lockCode, json.sequence, json.copies, json.name, json.environment, json.version,
      rules.map(rule => AssignmentUniqueRule.fromJson(rule)),
      backends.map(backend => AssignmentUniqueBackend.fromJson(backend)));
  }
 
  

}