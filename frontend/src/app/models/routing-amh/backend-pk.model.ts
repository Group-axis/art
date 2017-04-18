export class BackendPK {
  public code: string;
  public direction: string;

  constructor(code: string, direction: string) {
    this.code = code;
    this.direction = direction;
  }

  static hashPK(pk: BackendPK): string {
    return pk.code.toString() + "@" + pk.direction.toString();
  }
  
  static hash(code: string, direction: string): string {
    return code.toString() + "@" + direction.toString();
  }
}