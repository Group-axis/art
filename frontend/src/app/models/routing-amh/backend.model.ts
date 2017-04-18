import {BackendPK} from './backend-pk.model';


export class Backend {
  public pkCode: string;
  public pkDirection: string;
  public code: string;
  public name: string;
  public dataOwner: string;
  public description: string;
  public lockCode: string;

  constructor(pkCode:string, pkDirection:string, code?: string, dataOwner?: string, description?: string, lockCode?: string, name?: string) {
    this.pkCode = pkCode;
    this.pkDirection = pkDirection;
    this.code = code;
    this.dataOwner = dataOwner;
    this.description = description;
    this.lockCode = lockCode;
    this.name = name;
  }


}