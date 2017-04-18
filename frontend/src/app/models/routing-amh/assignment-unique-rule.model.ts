import {AssignType} from './assignment-type.model';

export class AssignmentUniqueRule {
  public code: string;
  public dataOwner: string;
  public lockCode: string;
  public sequence: number;
  public expression: string;
 //UI properties
 public selected : boolean;
 
  constructor(code: string, dataOwner: string, lockCode: string, sequence: number, expression?: string) {
    this.code = code;
    this.dataOwner = dataOwner;
    this.lockCode = lockCode;
    this.sequence = sequence;
    this.expression = expression;
  }

  public toWriteModel(type: AssignType, assignmentCode: string, environment: string, version: string, modificationUserId : string,  modificationDate: string, creationUserId? : string, creationDate?: string): any {
    switch (type) {
      case AssignType.BK_CHANNEL:
      case AssignType.DTN_COPY:
      case AssignType.FEED_DTN_COPY:
        return {
            code: assignmentCode,
            sequence: this.sequence,
            ruleCode: this.code,
            dataOwner: this.dataOwner,
            lockCode: this.lockCode,
            environment: environment,
            version: version,
            modificationUserId : modificationUserId,
            modificationDate : modificationDate,
            creationUserId : creationUserId,
            creationDate : creationDate
          };
    }
  }

  public static fromJson(json : any) : AssignmentUniqueRule {
      return new AssignmentUniqueRule(json.code, json.dataOwner, json.lockCode, json.sequence, json.expression); 
  }
}