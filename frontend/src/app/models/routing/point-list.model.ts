
export class PointList {
  public pointName: string;
  public schemas: string;
  public ruleSeq: number;
  public ruleDescription: string;
  public ruleMessage: string;
  public originalPoint : String;
  public originalCode : String;
  public copyPoint : String;
  public copyCode : String;

constructor(pointName: string, schemas: string, ruleSeq: number, ruleDescription : string, ruleMessage : string, 
            originalPoint? : String, originalCode? : String, copyPoint? : String, copyCode? : String) {
    this.pointName = pointName;
    this.schemas = schemas;
    this.ruleSeq = ruleSeq;
    this.ruleDescription = ruleDescription;
    this.ruleMessage = ruleMessage;
    this.originalPoint = originalPoint;
    this.originalCode = originalCode;
    this.copyPoint = copyPoint;
    this.copyCode = copyCode;
  }
}