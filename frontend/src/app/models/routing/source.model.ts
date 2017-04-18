
export class Source {
  public action: string;
  public actionOption: string;
  public intervention: string;
  public interventionText: string;
  public unit: string;
  public routingCode: string;
  public priority: string;

  //   constructor();
  constructor(action?: string, actionOption?: string, intervention?: string, interventionText?: string, unit?: string, routingCode?: string, priority?: string) {
    this.action = action;
    this.actionOption = actionOption;
    this.intervention = intervention;
    this.interventionText = interventionText;
    this.unit = unit;
    this.routingCode = routingCode;
    this.priority = priority;
  }

  static empty() : Source {
    return new Source("", "", "", "", "", "", "");
  }  
}