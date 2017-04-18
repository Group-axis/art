import {AssignType} from './assignment-type.model';

export class AssignmentUniqueBackend {
  public code: string;
  public direction: string;
  public dataOwner: string;
  public lockCode: string;
  
  constructor(code: string, direction:string, dataOwner?: string, lockCode?: string) {
    this.code = code;
    this.direction = direction;
    this.dataOwner = dataOwner;
    this.lockCode = lockCode;
  }

  public toWriteModel(type: AssignType, assignmentCode: string, environment: string, version: string, modificationUserId : string,  modificationDate: string, creationUserId? : string, creationDate?: string): any {
    switch (type) {
      case AssignType.DTN_COPY:
      case AssignType.FEED_DTN_COPY:
        return {
          code: assignmentCode,
          backCode: this.code,
          backDirection: this.direction,
          dataOwner: this.dataOwner,
          lockCode: this.lockCode,
          environment: environment,
          version: version,
          modificationUserId : modificationUserId,
          modificationDate : modificationDate,
          creationUserId : creationUserId,
          creationDate : creationDate
        }
        default: 
        return {}
    }
  }

  public static fromJson(json : any) : AssignmentUniqueBackend {
      return new AssignmentUniqueBackend(json.code, json.direction, json.dataOwner, json.lockCode); 
  }
}