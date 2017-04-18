import {Component, OnInit, ViewChild} from 'angular2/core';
import { NgClass, Validators, ControlGroup, FormBuilder, CORE_DIRECTIVES, FORM_DIRECTIVES, FORM_PROVIDERS } from '@angular/common';
import { ACCORDION_DIRECTIVES, BUTTON_DIRECTIVES } from 'ng2-bootstrap';
import { Router, RouteParams} from 'angular2/router';
import {FormFieldComponent} from '../../common/components/ui/widgets/form-field';
import {FormLabelComponent} from '../../common/components/ui/widgets/label';
import {FormPickListComponent, FormPickListValueAccessor, listRequired } from '../../common/components/ui/widgets/pick-list';
import {FormComboDuoComponent, FormComboDuoValueAccessor } from '../../common/components/ui/widgets/combo-duo';
import {SAARoutingService} from "../saa-routing.service";
import {ElementSearchComponent} from "../saa-point-search";
import {PageReferential, Logger} from '../../common/components/services';
import {DisablePermissions, Permissions, NotPermissions} from '../../common/directives';
import {FormFieldComponentExample, BootstrapFormDirective, BootstrapInputDirective, LabelsComponent, LabelsValueAccessor, notEmpty} from '../../common/components/ui/controls';
import {Rule, Action, Source, ActionOn, NewInstance, Condition, ConditionType} from '../../models/routing';
import {IOption, IdCodeDescription} from '../../models/referential';
import {Alert} from '../../common/components/ui/widgets/modal';

// console.log('`SAARoutingRule` component loaded asynchronously');

function selectRequired(control) {
    // console.debug("control.value "+control.value);
  if( !control.value || 
      control.value.length === 0) {
    return {
      selectRequired: true
    }
  }

  return null;
}


@Component({
  selector: 'saa-routing-rule',
  styles: [`
    h1 {
      font-family: Arial, Helvetica, sans-serif
    }
  `],
  template: require('./saa-routing-rule.html'),
  directives: [ACCORDION_DIRECTIVES, BUTTON_DIRECTIVES, CORE_DIRECTIVES, FORM_DIRECTIVES,
    FormFieldComponent, FormLabelComponent, FormPickListComponent, FormPickListValueAccessor, FormComboDuoComponent, FormComboDuoValueAccessor,
    FormFieldComponentExample, BootstrapFormDirective, BootstrapInputDirective, LabelsComponent, LabelsValueAccessor, NgClass,
    Alert, ElementSearchComponent,DisablePermissions, Permissions, NotPermissions],
  providers: [FORM_PROVIDERS, SAARoutingService, PageReferential]
})
export class SAARoutingRule implements OnInit {
  @ViewChild(Alert) alert;
  descriptionForm: ControlGroup;
  conditionForm: ControlGroup;
  actionForm: ControlGroup;

  private rule: Rule = Rule.empty();
  private ruleActionActionOnId : string;
  private ruleConditionConditionOnId : string;

  private availableFunctions: Array<IdCodeDescription> = [];
  private selectedFunctions: Array<IdCodeDescription>;
  private availableFunctionsMap : Map<number, any>;
  private availableTypes: IOption[];
  private availableActions: IOption[];
  private availableInterventions: IOption[];
  private availableUnits: IOption[];
  private availablePriorities: IOption[];

  private sourceActionsSelected: { parentValue: string, childValue: string } = {parentValue: "", childValue: ""};
  private displaySourceInterventionText: boolean;
  private newInstanceActionsSelected: { parentValue: string, childValue: string } = {parentValue: "", childValue: ""};
  private newInstanceTypesSelected: { parentValue: string, childValue: string } = {parentValue: "", childValue: ""};
  private displayNewInstanceInterventionText: boolean;

  //Navigations
  private isCreation : boolean;
  private returnToString : string;
  private parameters : string;
  private navigateUrl : string;
  private isDirty : boolean;  
  
   //Validations
  private pointNameErrorMsg : string = "";
  private sequenceErrorMsg : string = "";
  

  constructor(private router : Router, routeParams: RouteParams, fb: FormBuilder,
              private saaRoutingService: SAARoutingService, 
              private referentialService : PageReferential,
              private logger : Logger) {
    let pointName = routeParams.params['pointName'];
    let sequence = routeParams.params['sequence'];
    this.logger.debug("rule constructor sequence "+ sequence + " pointName " + pointName);
    this.isCreation = !pointName || !sequence;

    this.returnToString = routeParams.params['return_to'] || 'SAARoutingHome';
    this.setReturnToParameters(routeParams.params['params']);

    this.descriptionForm = fb.group({
      'sequence': ['', Validators.required]
      //TODO: add rule sequence uniqueness validation 
    });

    this.conditionForm = fb.group({
      'functions': ['', listRequired],
      'messages': ['', Validators.required]
    });
    
    this.actionForm = fb.group({
      'srcIntervention': ['', selectRequired],
      'newIntervention': ['', selectRequired]
    });

    
    this.loadRule(pointName, sequence);
  }

  ngOnInit() {
    this.logger.log('hello `SAA  Routing Rule` component');
    if (true==true) return;
    this.referentialService.getSAARules().subscribe(
      data => {
        this.availableTypes = data["types"];
        this.availableTypes.forEach(v => this.logger.debug("  types "+ v))
        // this.availableActions = this.getActions();
        this.availableActions = data["actions"];
        this.availableActions.forEach(a => this.logger.debug(a.text+" - "+a.id+" - "+a.code));
        this.availableInterventions = data["interventions"];
        this.availableUnits = data["units"];
        this.availablePriorities = data["priorities"];
        
        this.availableFunctionsMap = data["functions"].reduce( ( total, current ) => {
          total.set(current.id, new IdCodeDescription(current.id, current.code, current.description));
          return total;
          }, new Map<number, any>());
        this.fillupSelectedValuesFromRule();  
        }
    );
  }


private setReturnToParameters(params: string) {
    this.logger.log(" receiving params to return = " + params);
    if (!params || params.length == 0) {
      return;
    }

    let parameters = params.split('&');
    parameters.forEach(pair => {
      let pairValues = pair.split("=");
      this.parameters[pairValues[0]] = pairValues[1];
      this.logger.log(" paramteres to return " + pairValues[0]+" = "+ pairValues[1]);
    });
  }

  actionCancel(){
    this.verifyAndNavigate(this.returnToString, 300, 301);
  }

  actionUpdateActionId(newValue : number) {
    this.logger.debug("this.ruleActionActionOnId "+this.ruleActionActionOnId+" newValue "+newValue);
    this.rule.action.actionOn.id = newValue;
    this.updateActionControlGroup(newValue);
  }

  actionUpdateConditionId(newValue : number) {
    this.logger.debug("this.ruleConditionConditionOnId "+this.ruleConditionConditionOnId+" newValue "+newValue);
    this.rule.condition.conditionOn.id = newValue;
    this.updateConditionControlGroup(newValue);
  }

  private updateConditionControlGroup(selection : number) {
     switch(selection) {
      case 1:
        this.conditionForm.exclude('functions');
        this.conditionForm.include('messages');
      break;
      case 2:
        this.conditionForm.include('functions');
        this.conditionForm.exclude('messages');
      break;
      case 4:
        this.conditionForm.include('functions');
        this.conditionForm.include('messages');
      break;
    } 
  }

private updateActionControlGroup(selection : number) {
     switch(selection) {
      case 1:
        this.actionForm.exclude('newIntervention');
        this.actionForm.include('srcIntervention');
      break;
      case 2:
        this.actionForm.include('newIntervention');
        this.actionForm.exclude('srcIntervention');
      break;
      case 4:
        this.actionForm.include('newIntervention');
        this.actionForm.include('srcIntervention');
      break;
    } 
  }
  // parentChanged(parentValue: string) {
  //   this.logger.log(" received " + parentValue);
  //   this.rule.action.source.action = parentValue;
  // }

  // displayFunction() {
  //   return ConditionType.MESSAGE !== ConditionType[this.rule.condition.type.code];
  // }

  // displayMessage() {
  //   return ConditionType[this.rule.condition.type.code] !== ConditionType.FUNCTION;
  // }



  // affiche() {
  //   this.logger.log(" source action " + this.rule.action.source.action + " source actionoption " + this.rule.action.source.actionOption);
  //   this.logger.log(" duo parent " + this.ectionSelected.parentCode + " child value " + this.sourceActionsSelected.childCode);

  // }
  
//   suffisammentGrand = (element :IdCodeDescription) : boolean => {
//   return element.code !== "C";
// }

//   private getFunctions = (ruleFunctions: IdCodeDescription[]): IdCodeDescription[] => {
//     let functions = [
//       new IdCodeDescription(1, "A", "Authorization not present"),
//       new IdCodeDescription(2, "E", "Failure"),
//       new IdCodeDescription(3, "R", "Inactive correspondent"),
//       new IdCodeDescription(4, "S", "Nacked"),
//       new IdCodeDescription(5, "Z", "Not authorized by RMAdd"),
//       new IdCodeDescription(6, "C", "Success")];
//     functions = Array.apply(null, functions).filter(this.suffisammentGrand);

//     return functions;
//   }
  
  /*
  (item) => {
      let mapped = ruleFunctions.map(IdCodeDescription.mapToProperty)
       let found =  mapped.indexOf(item.code);
       found  === -1;
    }
  */ 

  // private getActions = (): IOption[] => {
  //   //TODO: service call to server returning json string
  //   let actionsJSON: string = `[ 
  //         { "text": "None", "id": 0, "code":"NONE", "children": []},
	// 		{ "text": "Dispose to", "code":"ACTION_TYPE_ROUTING_POINT", "id":1,
  //               "children": [
  //                   { "text": "SARMTR", "code":"SARMTR", "id": 11 },
  //                   { "text": "EMRGZR", "code":"EMRGZR", "id": 12 },
  //                   { "text": "SEDED", "code":"SEDED", "id": 13 },
  //                   { "text": "MRTTRE", "code":"MRTTRE", "id": 14 }
  //                   ]},
  //           { "text": "Complete", "id": 2, "code":"ACTION_TYPE_COMPLETE", "children": []},
  //           { "text": "To adresse", "id": 3, "code":"ACTION_TYPE_ADDRESSEE", "children": []}
  //       ]`;

  //   return JSON.parse(actionsJSON);
  // }

  private actionComboChanged(value) {
    this.logger.debug("from comboChanged "+ JSON.stringify(value) );
    this.newInstanceActionsSelected = value;
    
    this.rule.action.newInstance.action= value.parentValue; 
    this.rule.action.newInstance.actionOption= value.ChildValue;
  }

  // private getTypes = (): IOption[] => {
  //   //TODO: service call to server returning json string
  //   let typesJSON: string = `[ 
  //         { "text": "None", "id": 0, "code":"NONE", "children": []},
	// 		{ "text": "Notification", "code":"ACTION_TYPE_ROUTING_POINT", "id":1,
  //               "children": [
  //                   { "text": "Transmission", "code":"SARMTR", "id": 11 },
  //                   { "text": "Autre", "code":"MRTTRE", "id": 14 }
  //                   ]},
  //           { "text": "Complete", "id": 2, "code":"ACTION_TYPE_COMPLETE", "children": []},
  //           { "text": "To adresse", "id": 3, "code":"ACTION_TYPE_ADDRESSEE", "children": []}
  //       ]`;

  //   return JSON.parse(typesJSON);
  // }

  // private getInterventions = (): IOption[] => {
  //   //TODO: service call to server returning json string
  //   let interventionsJSON: string = `[ 
  //         { "text": "None", "code":"NONE", "id": 0 },
	// 	  { "text": "Dispose to", "code":"ACTION_TYPE_ROUTING_POINT", "id":1},
  //         { "text": "Complete", "code":"ACTION_TYPE_COMPLETE", "id": 2},
  //         { "text": "To adresse", "code":"ACTION_TYPE_ADDRESSEE", "id": 3}
  //       ]`;

  //   return JSON.parse(interventionsJSON);
  // }
  // private getUnits = (): IOption[] => {
  //   //TODO: service call to server returning json string
  //   let unitsJSON: string = `[ 
  //         { "text": "Keep current", "code":"KEEP_CURRENT", "id": 0 },
	// 	  { "text": "forrward", "code":"FORWARD", "id":1}
  //       ]`;

  //   return JSON.parse(unitsJSON);
  // }
  // private getPriorities = (): IOption[] => {
  //   //TODO: service call to server returning json string
  //   let prioritiesJSON: string = `[ 
  //     { "text": "Keep current", "code":"KEEP_CURRENT" ,"id": 0 },
	// 	  { "text": "High priority", "code":"HIGH_PRIORITY", "id":1}
  //       ]`;

  //   return JSON.parse(prioritiesJSON);
  // }

  changeSourceIntervention(interventionSelected: number) {
    this.displaySourceInterventionText = this.availableInterventions[interventionSelected].code === "INTV_FREE_FORMAT";
  }

  changeNewInstanceIntervention(interventionSelected: number) {
    this.displayNewInstanceInterventionText = this.availableInterventions[interventionSelected].code === "INTV_FREE_FORMAT";
  }

  private fillupSelectedValuesFromRule() {
    this.selectedFunctions = [];
    this.availableFunctions = [];
    let selectedMap = {};

    if (this.rule.condition.functions) {
       this.rule.condition.functions.forEach(func => {
        let funcOption = this.availableFunctionsMap.get(func.id);
        if (funcOption) {
           this.selectedFunctions.push(funcOption);
           selectedMap[func.id] = funcOption; 
        }
       });
    }

    if (this.availableFunctionsMap) {
      this.logger.debug("map has values " + this.availableFunctionsMap.size);
      this.availableFunctionsMap.forEach(
        (v,k,m) => {
        
        let selectedOption = selectedMap[k];
        if (!selectedOption) {
          this.availableFunctions.push(v);
          this.logger.debug("pushing to functionavailables " + v);
        }
      });
    }

    this.newInstanceActionsSelected.parentValue = this.rule.action.newInstance.action;
    this.newInstanceActionsSelected.childValue = this.rule.action.newInstance.actionOption;
    
    let conditionOnId : number = this.rule.condition.conditionOn.id;
    let actionOnId : number = this.rule.action.actionOn.id;
    this.ruleActionActionOnId = actionOnId + "";
    this.ruleConditionConditionOnId = conditionOnId + "";
    this.updateConditionControlGroup(conditionOnId);
    this.updateActionControlGroup(actionOnId);
    this.displaySourceInterventionText = this.rule.action.source.intervention === "INTV_FREE_FORMAT";
    this.displayNewInstanceInterventionText = this.rule.action.newInstance.intervention === "INTV_FREE_FORMAT";
  } 

  private loadRule(pointName : string, sequence: string) {
    if (!pointName && !sequence) {
        this.rule = Rule.empty();
        this.fillupSelectedValuesFromRule();
        return;
    }

    let sequenceNumber = +sequence;
    //load rule from service by sequence
    this.saaRoutingService.findRuleByPointAndSequence(pointName, sequenceNumber)
    .subscribe(
      resp => {
        let tmpRule = Rule.empty();
        if (resp.found) {
          this.logger.log("SAA rule with sequence ["+sequenceNumber+"] was found in point " + pointName);
          tmpRule.update(resp.value); 
          this.logger.debug("loadedRule "+JSON.stringify(resp.value));
          this.logger.debug("updated "+JSON.stringify(tmpRule));
        } else {
          this.logger.warn("SAA rule with sequence ["+sequenceNumber+"] was not found in point " + pointName);
        }
        this.rule = tmpRule;
        // this.rule.action.source = new Source("ACTION_TYPE_ROUTING_POINT", "", "INTV_NO_INTV","","","","");
      },
      error => {
        this.logger.warn("An error has occurred while loading SAA rule");
        // this.rule = Rule.empty();
        this.fillupSelectedValuesFromRule();
      },
      () => {
        this.logger.log("SAA rule with sequence ["+sequenceNumber+"] from point "+pointName+" retrieved");
        this.fillupSelectedValuesFromRule();
      }
    );
  }

confirmClose(data) {
    this.logger.log("confirming ------------  " + data);
    switch(data) {
      case 300: // Navigate Cancel 
        //Do nothing
      break;
      case 301: //Navigate yes, lost changes
        this.router.parent.navigate([this.navigateUrl, this.parameters]);
      break;
    }
  }

  private alertPreventLostChanges(cancelResponse: number, yesResponse : number) {
    this.alert.waitIcon = false;
    this.alert.alertFooter = true;
    this.alert.cancelButton = true;
    this.alert.cancelButtonText = "Cancel";
    this.alert.cancelButtonResponse = cancelResponse;
    this.alert.yesButton = true;
    this.alert.yesButtonText = "Yes";
    this.alert.yesButtonResponse = yesResponse;
    this.alert.okButton = false;
    this.alert.alertHeader = true;
    this.alert.alertTitle = " Alert ";
    this.alert.message = "All changes will be lost, Do you want to continue?";
    this.alert.open();
  }
  
  private verifyAndNavigate(link : string, cancelResponse : number, yesResponse : number) {
     
    if (this.isDirty) {
        this.logger.debug("inside if "+ this.isDirty);
        this.navigateUrl = link;
        this.alertPreventLostChanges(cancelResponse, yesResponse);
        return;
    }

    this.logger.debug("it was not dirty going to "+link); 
    this.router.parent.navigate([link, this.parameters]);
  }


   actionNavigate(link : string) {
     this.verifyAndNavigate(link, 300, 301);
   }

   actionRoutingPointSelected(selectedRoutingPoint : any) {

     this.logger.debug("new value selectedRoutingPoint.pointName "+selectedRoutingPoint.pointName)
     this.rule.routingPoint = selectedRoutingPoint.pointName;
     this.pointNameErrorMsg = "";
     if (selectedRoutingPoint.pointName) {
          this.saaRoutingService.findPointByName(this.rule.routingPoint)
        .subscribe(
          res => {
            if (!res.found) {
               this.pointNameErrorMsg = "Routing point '"+this.rule.routingPoint+"' does not exist";
            } 
            this.verifySequence(this.rule.routingPoint, this.rule.sequence);
            
          },
          err => this.pointNameErrorMsg = "Routing point '"+this.rule.routingPoint+"' does not exist"
        );
     }
   }

   actionSequenceChange(sequence : number) {
     this.logger.debug("new value rule.sequence "+sequence);
     this.verifySequence(this.rule.routingPoint, sequence);

   }

   private verifySequence(pointName : string, sequence : number) {
     this.sequenceErrorMsg = "";
     if (!pointName || !sequence) {
       return;
     }
     this.saaRoutingService.findRuleByPointAndSequence(pointName, sequence)
        .subscribe(
          res => {
            if (res.found) {
                this.sequenceErrorMsg = "Sequence "+sequence+" already exists in routing point'"+pointName;
            } 
          },
          err => this.sequenceErrorMsg = "Sequence "+sequence+" already exists in routing point'"+pointName
        );
   }
   
   isSaveButtonDisabled() {
    this.logger.debug("pointNameErrorMsg " + this.pointNameErrorMsg);
     return !this.descriptionForm.valid || !this.conditionForm.valid || !this.actionForm.valid || this.pointNameErrorMsg.length > 0 || this.rule.routingPoint.length == 0;
   }

  private alertOpen() {
    this.alert.alertFooter = false;
    this.alert.cancelButton = false;
    this.alert.okButton = false;
    this.alert.alertHeader = true;
    this.alert.alertTitle = "Alert";
    this.alert.message = "Save in progress...";
    this.alert.cancelButtonText = "Ok";
    this.alert.open();
  }

  private closeAlert() {
    setTimeout(() => this.alert.cancel(), 1000);
    setTimeout(() => this.router.parent.navigate([this.returnToString, this.parameters]), 1200);
  }

   actionSave() {
     this.logger.log("saving rule ..." + this.rule.sequence + " isCreation " + this.isCreation);
     this.updateRule();
     this.alertOpen();
    let send;
    let env = "UNKNOWN";
    let version = "DEFAULT"
    if (this.isCreation) {
      send = this.saaRoutingService.createRule(env, version, this.rule);
    } else {
      send = this.saaRoutingService.saveRule(env, version, this.rule);
    }
    send.subscribe(
      data => {
        this.logger.log("[saveRule] %s", data);
      },
      err => {
        this.logger.log("[saveRule] The rule cannot be saved. Error code: %s, URL: %s ", err.status, err.url);
        this.alert.message = "The rule cannot be saved !!";
        this.closeAlert();
      },
      () => {
        this.logger.log("[saveRule] assignment's rules from backend [%s,%s+] retrieved");
        this.alert.message = "The rule saved successfuly !!";
        this.closeAlert();
      }
    );
     
   }

   private updateRule() {
    this.rule.action.source.action= this.sourceActionsSelected.parentValue; 
    this.rule.action.source.actionOption= this.sourceActionsSelected.childValue;
    this.rule.action.newInstance.action= this.newInstanceActionsSelected.parentValue; 
    this.rule.action.newInstance.actionOption= this.newInstanceActionsSelected.childValue;
    this.rule.action.newInstance.instanceType= this.newInstanceTypesSelected.parentValue; 
    this.rule.action.newInstance.instanceTypeOption= this.newInstanceTypesSelected.childValue;
    this.rule.condition.functions = this.selectedFunctions;     
   }   
   
}
 
