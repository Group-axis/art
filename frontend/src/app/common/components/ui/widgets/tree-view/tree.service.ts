import {Injectable} from 'angular2/core';
import {Logger} from "../../../services";

@Injectable()
export class TreeSelectionService {
  private selection : string;
    
  constructor(private logger : Logger) { }

  selectionDone(criteriaCode: string) {
    this.logger.log("saving selection "+criteriaCode);
    this.selection = criteriaCode;
  }
  
  getSelection() {
    this.logger.log("returning "+ this.selection);
    return this.selection;
  }
}