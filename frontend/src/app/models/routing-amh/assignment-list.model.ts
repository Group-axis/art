
 
export class AssignmentList {
  public active: boolean;
  public code: string;
  public backCode: string;
  public backDirection: string;
  public backName: string;
  public backSequence: number;
  public ruleCode: string;
  public ruleExpressions: string;
  public ruleSequence: number;
  public environment: string;
  public version: string;
  
  constructor(active: boolean, code: string, backCode: string, backDirection: string, backName: string, backSequence: number, 
              ruleCode: string, ruleExpressions: string, ruleSequence: number, environment : string, version : string ) {
    this.active = active;
    this.code = code;
    this.backCode = backCode;
    this.backDirection = backDirection;
    this.backName = backName;
    this.backSequence = backSequence;
    this.ruleCode = ruleCode;
    this.ruleExpressions = ruleExpressions;
    this.ruleSequence  = ruleSequence;
    this.environment = environment;
    this.version = version;
  }
}