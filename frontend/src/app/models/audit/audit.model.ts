export interface Audit {
  modificationUserId : string;  
  modificationDate: string; 
  creationUserId? : string;
  creationDate?: string;
}

export class BaseAudit implements Audit {
  public modificationUserId : string;  
  public modificationDate: string; 
  public creationUserId : string;
  public creationDate : string;

  addAuditValue(modificationUserId : string,  modificationDate: string, creationUserId? : string, creationDate?: string) {
      this.modificationUserId = modificationUserId;
      this.modificationDate = modificationDate;
      this.creationUserId = creationUserId;
      this.creationDate = creationDate;

      return this;
  }
}