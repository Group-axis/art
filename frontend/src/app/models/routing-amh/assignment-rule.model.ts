
export class AssignmentRule {
  public code: string;
  public dataOwner: string;
  public lockCode: string;
  public sequence: number;
  public expression:string;
  public environment: string;
  public version: string;

  constructor(code: string, dataOwner: string, lockCode: string, sequence: number,  environment: string, version: string, expression?:string) {
    this.code = code;
    this.dataOwner = dataOwner;
    this.lockCode = lockCode;
    this.sequence = sequence;
    this.environment = environment;
    this.version = version;
    this.expression = expression;
  }
}