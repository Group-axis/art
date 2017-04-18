
import {BackendPK} from './backend-pk.model';
import {AssignmentRule} from './assignment-rule.model';
 
export class Assignment {
  public active: boolean;
  public backendPrimaryKey: BackendPK;
  public code: string;
  public dataOwner: string;
  public description: string;
  public lockCode: string;
  public sequence: number;
  public environment: string;
  public version: string;
  public rules: AssignmentRule[];

  constructor(active?: boolean, backendPrimaryKey?: BackendPK, code?: string, dataOwner?: string, description?: string, lockCode?: string, sequence?: number, environment?: string, version?: string, rules?: AssignmentRule[]) {
    this.active = active;
    this.backendPrimaryKey = backendPrimaryKey;
    this.code = code;
    this.dataOwner = dataOwner;
    this.description = description;
    this.lockCode = lockCode;
    this.sequence = sequence;
    this.environment =environment;
    this.version = version;
    this.rules = rules || [];
  }
}