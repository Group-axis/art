import {IdCodeDescription} from '../referential';
import {Source} from './source.model';
import {NewInstance} from './new-instance.model';

export class Action {
  public actionOn: IdCodeDescription;
  public source: Source;
  public newInstance: NewInstance;
  
//   constructor();
  constructor(actionOn?: IdCodeDescription, source?:Source, newInstance?:NewInstance) {
      this.actionOn = actionOn;
      this.source = source;
      this.newInstance = newInstance;
  }

  public static empty() : Action {
    return new Action(IdCodeDescription.empty(), Source.empty(), NewInstance.empty());
  }

  public update(that : Action) {
    // this.actionOn = that.actionOn || this.actionOn;
    // this.source = that.source || this.source;
    // this.newInstance = that.newInstance || this.newInstance;
    Action.updateField(that, ["actionOn","source","newInstance"], this);
  }

  public toWriteModel()  : any { 
    /*
  "action": {
    "actionOn": "SOURCE",
	"instanceAction": "ACTION_TYPE_COMPLETE",
	"instanceInterventionType": "INTV_NO_INTV",
	"instanceInterventionTypeText": null,
	"instanceRoutingCode": "IFR-S",
	"instanceTargetQueue": null,
	"instanceUnit": null,
	"instancePriority": null,
	"newInstanceAction": null,
	"newInstanceRoutingCode": null,
	"newInstanceInterventionType": null,
	"newInstanceInterventionTypeText": null,
	"newInstanceTargetQueue": null,
	"newInstanceType": null,
	"newInstanceUnit": null,
	"newInstancePriority": null
  }, 
  "condition":{
    "conditionOn": "MESSAGE",
	  "criteria": "(Sender = 'PARBFRPP') and (SibesBranchCodeE = 'XXX') and (SibesFlux like '&PS%') and (SibesService like '0243%,0246%,4678%,9881%')",
	  "functionList": null
  }
 */
   let actionOn  = this.actionOn.id == 1 ? "SOURCE" : this.actionOn.id == 2 ? "NEW_INSTANCE" : "SOURCE_AND_NEW_INSTANCE";
   let instanceType =  this.newInstance.instanceType +  (this.newInstance.instanceTypeOption ? "_"+this.newInstance.instanceTypeOption : "");
  
    return {
    "actionOn": actionOn,
	"instanceAction": this.source.action,
	"instanceInterventionType": this.source.intervention,
	"instanceInterventionTypeText": this.source.interventionText,
	"instanceRoutingCode": this.source.routingCode,
	"instanceTargetQueue": this.source.actionOption,
	"instanceUnit": this.source.unit,
	"instancePriority": this.source.priority,
	"newInstanceAction": this.newInstance.action,
	"newInstanceRoutingCode": this.newInstance.routingCode,
	"newInstanceInterventionType": this.newInstance.intervention,
	"newInstanceInterventionTypeText": this.newInstance.interventionText,
	"newInstanceTargetQueue": this.newInstance.actionOption,
	"newInstanceType": instanceType,
	"newInstanceUnit": this.newInstance.unit,
	"newInstancePriority": this.newInstance.priority
    };
  }

  static updateField(orig : any, fields : Array<string>, dest : any) {
    fields.forEach( f => dest[f] = orig[f] || dest[f]);
  }
  
}