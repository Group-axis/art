import {Source} from './source.model';

export class NewInstance extends Source {
  public instanceType: string;
  public instanceTypeOption: string;
  
  constructor(instanceType?: string, instanceTypeOption?: string, action?: string, actionOption?: string, intervention?: string, interventionText?: string, unit?: string, routingCode?: string, priority?: string){
      super(action, actionOption, intervention, interventionText, unit, routingCode, priority);
      this.instanceType = instanceType;
      this.instanceTypeOption = instanceTypeOption;
  }

  static empty () : NewInstance {
    return new NewInstance("","","","","","","","","");
  }
}