import {AssignType} from './assignment-type.model.ts';

export class AssignmentConfig {
  public maxBackendsAllowed: number;
  public type: AssignType;
  
  constructor(type : AssignType, maxBackendsAllowed?: number) {
    this.type = type;
    this.maxBackendsAllowed = this.calculeMaxBackendsAllowed(type, maxBackendsAllowed);
  }

  private calculeMaxBackendsAllowed(type : AssignType, maxBackendsAllowed: number) {
    if (maxBackendsAllowed) {
      return maxBackendsAllowed;
    }

    switch (type) {
      case AssignType.BK_CHANNEL:
        return 1;
      case AssignType.DTN_COPY:
      case AssignType.FEED_DTN_COPY:
        return 1000;  
    }
  }

  public isTypeDifferent(type: string) : boolean {
    return  type != AssignType[this.type];
  }

  public typeAsString() : string {
    return AssignType[this.type];
  }

  public showExtraFields() : boolean {
    switch (this.type) {
      case AssignType.BK_CHANNEL:
        return false;
      case AssignType.DTN_COPY:
      case AssignType.FEED_DTN_COPY:
        return true;  
    }
  }
}