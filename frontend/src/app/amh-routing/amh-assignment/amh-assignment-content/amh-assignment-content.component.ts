import {Component, ElementRef, ViewChild, EventEmitter, Output, Input, OnChanges, SimpleChange } from 'angular2/core';
import {NgClass} from '@angular/common';
import {AMHAssignmentService} from "../../amh-service";
import { Router, RouteParams} from 'angular2/router';
import {Observable} from 'rxjs/Observable';
import {DataTableDirectives} from 'angular2-datatable/datatable';
import {AssignType, AssignmentConfig, AMHRule, Backend, AssignmentUnique, AssignmentUniqueRule, AssignmentUniqueBackend, BackendPK} from "../../../models/routing-amh";
import {Alert} from '../../../common/components/ui/widgets/modal';
import { AMHRuleSelectionComponent} from '../../amh-rule-selection';
import { AMHBackendSelectionComponent} from '../../amh-backend-selection';
import { AMHAssignmentSearchComponent} from '../../amh-assignment-search';
import { Permissions, NotPermissions, DisablePermissions } from '../../../common/directives';
import { Auth , Logger} from '../../../common/components/services';


//this.logger.log('`AMHAssignmentContent` component loaded asynchronously');

@Component({
  selector: 'amh-assignment-content',
  template: require('./amh-assignment-content.html'),
  providers: [AMHAssignmentService, Auth],
  directives: [NgClass, DataTableDirectives, Alert, AMHRuleSelectionComponent, AMHBackendSelectionComponent, AMHAssignmentSearchComponent,
               Permissions, NotPermissions, DisablePermissions]
})
export class AMHAssignmentContentComponent implements OnChanges {
  @ViewChild(Alert) alert;
  @Input("selection-config") public config: AssignmentConfig;
  @Input("save-status") public saveStatus: string;
  @Output() public enableSaveButtons: EventEmitter<any> = new EventEmitter();
  @Output() public assignmentUpdate: EventEmitter<any> = new EventEmitter();
  @Output() public saveWork: EventEmitter<any> = new EventEmitter();
  @Output() public rollbackSelection: EventEmitter<any> = new EventEmitter();
  @Output() public creationStatus: EventEmitter<boolean> = new EventEmitter<boolean>();

  private isCreation: boolean;
  private unconfirmedAssignmentCode: string = undefined;
  private assignment: AssignmentUnique = undefined;
  private receivedAssignmentCode: string;
  private assignmentTextFilter: string;
  private assignmentSequenceErrorMsg = "";
  private assignmentCopiesErrorMsg = "";
  private assignmentCodeErrorMsg = "";
  private isDirty : boolean = false;
  private disableInputs: boolean = true;
  private usedBackends: Array<Backend> = [];
  private haveRulesErrors : boolean = false;
  private pendingAssignmentSave: number = 0;
  private pendingNavigationConfig: any = {};
  private selectionRollbackType = undefined;
  private hasUserPermissions : boolean;

  constructor(private router: Router, routeParams: RouteParams, private amhAssignmentService: AMHAssignmentService
  , auth: Auth, private logger : Logger) {
    this.receivedAssignmentCode = routeParams.params['code'];
    //this.logger.log(" this.receivedAssignmentCode " + this.receivedAssignmentCode);

    this.assignmentTextFilter = this.receivedAssignmentCode || '';
    this.assignment = new AssignmentUnique(false, new BackendPK("", ""), "", "", "", "", undefined, undefined, "","","", [], []);
    this.isCreation = false;
    this.pendingAssignmentSave = 0;
    this.hasUserPermissions = auth.hasPermission(["amh.modify.assignment"]) == 1;
    this.creationStatus.emit(this.isCreation);
    
  }

/* SimpleChange
      previousValue: any;
      currentValue: any;
*/
  ngOnChanges(changes: { [propertyName: string]: SimpleChange }) {
    if (changes["config"]) {
      this.logger.debug("config has changed type "+this.config.type);
        if (this.selectionRollbackType != this.config.type) {
            this.confirmSelectionTableChanges();      
        }
    }
    let saveStatusChange : SimpleChange = changes["saveStatus"]; 
    this.logger.debug(" saveStatusChange === "+saveStatusChange+ " this.pendingAssignmentSave "+this.pendingAssignmentSave);
    if (saveStatusChange) {
      switch(saveStatusChange.currentValue) {
          case "OK":
              this.onSaveStatusOk();
          break;
        }
    }

    if (saveStatusChange && this.pendingAssignmentSave) {
        this.logger.debug("saveStatusChanged and pendingAssignmentSave == true");
        switch(saveStatusChange.currentValue) {
          case "OK":
              this.onSaveStatusOk();
              this.pendingAssignmentSave = 0;
          break;
          case "Error":
              this.onSaveStatusFail();
              this.pendingAssignmentSave = 0;
          break;
        }
        this.logger.debug("after switch pendingAsignmentSave");
    }
    
    
  }

  private onSaveStatusFail() {
    switch(this.pendingAssignmentSave) {
      case 2: //after save change selection table
      //An error has ocurred the rollback the selection change
        this.rollbackSelection.emit(true);
      break;
     }    
  }

  private onSaveStatusOk() {
    switch(this.pendingAssignmentSave) {
      case 1: //after save go to Rule creation
        this.logger.debug(" save was OK then go to Rule creation");
        this.navigateTo(this.pendingNavigationConfig.composentName, this.pendingNavigationConfig.parameters);
      break;
      case 2: //after save change selection table
        this.logger.debug(" save was OK then change selection table");
        this.initialization();
        break;
      case 3:
      case 0:
        this.logger.debug(" save in creation was OK then call afterSaveNewAssignment");
        this.afterSaveNewAssignment();
      break;
     }
  }


  ngOnInit() {
    //this.logger.log('hello `AMH assignment content` component');
    this.selectionRollbackType = this.config.type;
    this.amhAssignmentService.updateDirtyStatus(this.isDirty);
    this.loadAssignment(this.receivedAssignmentCode);
  }

  actionChangeRuleStatus(rulesStatus) {
    this.haveRulesErrors = rulesStatus;
    this.isDirty = true;
    this.amhAssignmentService.updateDirtyStatus(this.isDirty);
    this.emitDisableEnableSaveButtons();
  } 

  private addRule(addedRule) {
    this.assignment.rules = [addedRule, ...this.assignment.rules];
    this.assignmentUpdate.emit(this.assignment);
    this.isDirty = true;
    this.amhAssignmentService.updateDirtyStatus(this.isDirty);
    //this.logger.log(" adding " + addedRule.code + " to assignment ");
  }

  private deleteRule(deletedRuleCode) {
    this.assignment.rules = this.assignment.rules.filter((item: any) =>
      item["code"] !== deletedRuleCode);

    // this.enableSaveButtons.emit(true);
    this.assignmentUpdate.emit(this.assignment);
    // this.isDirty = true;
    //this.logger.log(deletedRuleCode + " deleted ");
  }

  private addToBackendChannel(addedBackend) {
    this.assignment.backendPrimaryKey = new BackendPK(addedBackend.pkCode, addedBackend.pkDirection);

    // this.enableSaveButtons.emit(false); //???
    this.assignmentUpdate.emit(this.assignment);
    this.isDirty = true;
    this.amhAssignmentService.updateDirtyStatus(this.isDirty);
    //this.logger.log(" adding " + addedBackend.pkCode);
  }

  private addToMultiBackend(addedBackend) {
    
    let uniqueBackend = new AssignmentUniqueBackend(addedBackend.pkCode, addedBackend.pkDirection, addedBackend.dataOwner, addedBackend.lockCode);
    this.assignment.backends = [uniqueBackend, ...this.assignment.backends];

    // this.enableSaveButtons.emit(true); //??? verify if there is no errors before send the event
    this.assignmentUpdate.emit(this.assignment);
    this.isDirty = true;
    this.amhAssignmentService.updateDirtyStatus(this.isDirty);
    //this.logger.log(" adding " + addedBackend.code + " to assignment ");
  }

  private addBackend(addedBackend) {
    switch(this.config.type) {
      case AssignType.BK_CHANNEL:
        this.addToBackendChannel(addedBackend);
        break;
      case AssignType.DTN_COPY:
      case AssignType.FEED_DTN_COPY:
        this.addToMultiBackend(addedBackend);
    }
    this.fillBackends();
    this.emitDisableEnableSaveButtons();
  }

  private deleteToBackendChannel(backendCodeDeleted) {
    this.logger.debug(" from deleteToBackendChannel  " + backendCodeDeleted);

    if (!this.assignment.backendPrimaryKey || this.assignment.backendPrimaryKey.code != backendCodeDeleted) {
      //this.logger.log("backend code to delete [" + backendCodeDeleted + "] not found ");
      return;
    }

    this.assignment.backendPrimaryKey = undefined;

    // this.enableSaveButtons.emit(true); //???
    this.assignmentUpdate.emit(this.assignment);
    this.isDirty = true;
    this.amhAssignmentService.updateDirtyStatus(this.isDirty);
  }

  private deleteToMultiBackend(backendCodeDeleted) {
    this.assignment.backends = this.assignment.backends
              .filter((item: any) => item["code"] !== backendCodeDeleted);

    // this.enableSaveButtons.emit(true);
    this.assignmentUpdate.emit(this.assignment);
    this.isDirty = true;
    this.amhAssignmentService.updateDirtyStatus(this.isDirty);
    //this.logger.log(backendCodeDeleted + " deleted ");
  }

  private deleteBackend(backendCodeDeleted) {
    switch(this.config.type) {
      case AssignType.BK_CHANNEL:
        this.deleteToBackendChannel(backendCodeDeleted);
        break;
      case AssignType.DTN_COPY:
      case AssignType.FEED_DTN_COPY:
        this.deleteToMultiBackend(backendCodeDeleted);
    }
    this.fillBackends();
    this.emitDisableEnableSaveButtons();
  }

  private fillBackends() {
    //this.logger.log("filling up backends ");
    let backends: Array<Backend> = [];
    switch (this.config.type) {
      case AssignType.BK_CHANNEL:
        if (this.assignment.backendPrimaryKey && this.assignment.backendPrimaryKey.code) {
          backends.push(new Backend(this.assignment.backendPrimaryKey.code, this.assignment.backendPrimaryKey.direction));
        }
        break;
      case AssignType.DTN_COPY:
      case AssignType.FEED_DTN_COPY:
        if (this.assignment.backends) {
          this.assignment.backends.forEach(backend => {
            backends.push(new Backend(backend.code, backend.direction));
          });
        }
        break;
    }

    this.usedBackends = backends;
  }

  selectAssignment(code) {
    //this.logger.log("selected assignment code " + code);
    if (!code) {
      this.logger.error("code[" + code + "] missing ");
      return;
    }
    this.unconfirmedAssignmentCode = code;
    this.confirmAssignmentChange();
  }

  private hasThisAssignmentErrors() : boolean {
      let enableSaveButtons : boolean = this.assignmentSequenceErrorMsg.length > 0; 
      //this.logger.debug("1.- sequenceMsgErrors "+ enableSaveButtons);
      enableSaveButtons = enableSaveButtons || this.assignmentCodeErrorMsg.length > 0;
      //this.logger.debug("2.- CodeMsgError "+ (this.assignmentCodeErrorMsg.length > 0));
      enableSaveButtons = enableSaveButtons || this.haveRulesErrors;
      //this.logger.debug("3.- ruleErrors "+ this.haveRulesErrors);

      // has at least one backend assigned
      enableSaveButtons = enableSaveButtons || (!this.usedBackends || this.usedBackends.length < 1);
      //this.logger.debug("4.- backendSizeError "+ (!this.usedBackends || this.usedBackends.length < 1));
      // On creation has a code and sequence
      if (this.isCreation) {
          enableSaveButtons = enableSaveButtons || (!this.assignment.code || this.assignment.code.length == 0);
         // this.logger.debug("5.- onNew-CodeError "+ (!this.assignment.code || this.assignment.code.length == 0));
          enableSaveButtons = enableSaveButtons || (!this.assignment.sequence || this.assignment.sequence == 0);
         // this.logger.debug("6.- onNew-SequenceError "+ (!this.assignment.sequence || this.assignment.sequence == 0));
      }

      return enableSaveButtons;
  }

  private emitDisableEnableSaveButtons() {
    this.enableSaveButtons.emit(this.hasThisAssignmentErrors());
  }

 private doDisableInputs() : void {
   this.logger.debug(" doDisable "+this.hasUserPermissions);
   this.disableInputs = true;
 }

 private doEnableInputs() : void {
   this.logger.debug(" doEnable "+this.hasUserPermissions);
   if (!this.hasUserPermissions) {
      this.disableInputs = true;
      return; 
   }

   this.disableInputs = false;
 }


  private loadAssignment(code: string) {
    if (!code || code.length == 0) {
      this.createNewAssignment();
      return;
    }

    this.amhAssignmentService.findAssignmentByCode(this.config.type, code)
      .subscribe(
      data => {
        //this.logger.log("[loadAssignment] Data retrieved from service: %s ", data.code);
        this.assignment = AssignmentUnique.fromJson(data);
        this.assignment.rules = this.assignment.rules.sort(
          (rule1, rule2) => {
            return rule1.sequence - rule2.sequence;
          });
        this.isCreation = false;
        this.creationStatus.emit(this.isCreation);
        this.doEnableInputs();
        this.haveRulesErrors = false;
        this.isDirty = false;
        this.amhAssignmentService.updateDirtyStatus(this.isDirty);
        this.assignmentUpdate.emit(this.assignment);
        this.fillBackends();
      },
      err => {
        this.logger.warn("[loadAssignment] Can't get assignment. Error code: %s, URL: %s ", err.status, err.url);
        this.createNewAssignment();
        this.emitDisableEnableSaveButtons();
      },
      () => {
        //this.logger.log("[loadAssignment] assignment code [%s] retrieved", code);
        if (!this.assignment) {
          this.createNewAssignment();
        }
        this.emitDisableEnableSaveButtons();
      }
      );

  }

  actionCreateNewAssignment() {
    this.doEnableInputs();
    this.unconfirmedAssignmentCode = "";
    let hasAssignErros = this.hasThisAssignmentErrors();
    if (this.isDirty && hasAssignErros) {
        this.alertPreventLostChanges(400,401);
        return;
    }

    if (!this.isDirty || hasAssignErros) {
      this.loadAssignment(this.unconfirmedAssignmentCode);
      return;  
    }
    this.confirmAssignmentChange();
  }
  
  disableRuleCreation() {
    return this.disableInputs || this.hasThisAssignmentErrors();
  }

  // actionCreateNewRule() {
  //   if (!this.isDirty) {
  //     this.actionRuleNavigation(undefined);
  //     return;  
  //   }
  //   this.alertLostChanges(0, 201,202);
  // }

  private createNewAssignment() {
    //this.logger.log("creating new assignment");
    this.assignment = new AssignmentUnique(false, new BackendPK("", ""), "", "", "", "", undefined, undefined, "", "", "", [], []);
    this.fillBackends();
    this.isCreation = true;
    this.creationStatus.emit(this.isCreation);
    this.assignmentUpdate.emit(this.assignment);
    this.assignmentSequenceErrorMsg = "";
    this.assignmentCopiesErrorMsg = "";
    this.isDirty = false;
    this.amhAssignmentService.updateDirtyStatus(this.isDirty);
    this.haveRulesErrors = false;
    this.emitDisableEnableSaveButtons();
  }

  private isInteger(x: number): boolean {
    return x % 1 === 0;
  }

  private updateAssignmentUniquenessErrorMsg(sequence: number) {
    this.assignmentSequenceErrorMsg = "";
    this.logger.debug("[assingmentWithSequence] calling findAssignmentsBySequence(%s,%s)", this.config.type, sequence);
    this.amhAssignmentService.findAssignmentsBySequence(this.config.type, sequence)
      .subscribe(
      data => {
        //this.logger.log("[assingmentWithSequence] Data retrieved from service: %s with type %s", data.code, this.config.type);
        if (this.assignment.code !== data.code) {
          this.assignmentSequenceErrorMsg = "The sequence " + sequence + " is already used by the assignment " + data.code;
        }
      },
      err => {
        this.logger.error("[assingmentWithSequence] Can't get assignments. Error code: %s, URL: %s ", err.status, err.url);
      },
      () => {
       // this.logger.log("[assingmentWithSequence] with msg [%s]", this.assignmentSequenceErrorMsg);
        this.emitDisableEnableSaveButtons();
      }
      );

  }

  private updateAssignmentCodeUniquenessErrorMsg(code: string) {
    this.assignmentCodeErrorMsg = "";
    this.amhAssignmentService.findAssignmentsByCode(this.config.type, code)
      .subscribe(
      data => {
        //this.logger.log("[assingmentWithCode] Data retrieved from service: %s, current %s ", data.code, this.assignment.code);
        this.assignmentCodeErrorMsg = "The code " + code + " already exists ";
      },
      err => {
        this.logger.error("[assingmentWithCode] Can't get assignments. Error code: %s, URL: %s ", err.status, err.url);
      },
      () => {
        //this.logger.log("[assingmentWithCode] with msg [%s]", this.assignmentCodeErrorMsg);
        this.emitDisableEnableSaveButtons();
      }
      );
  }

  private getRulesUniquenessErrorMsgs(): string[] {
    if (!this.assignment || !this.assignment.rules) {
      return [];
    }
    let hashRuleCode = {};

    let errorMessages: string[] = this.assignment.rules.filter(rule => {
      if (hashRuleCode[rule.sequence]) {
        return true;
      }

      hashRuleCode[rule.sequence] = rule;
      return false;
    }).map(rule => {
      return rule.sequence + " is used by " + rule.code.toUpperCase();
    });

    return errorMessages;
  }

  assignmentSequenceUpdate(inputElem: HTMLInputElement) {
    //this.logger.log("assignment " + this.assignment.code + " has changed its value to " + this.assignment.sequence);
    let numericSeqValue: number = + inputElem.value;
    let oldSequence = this.assignment.sequence;
    this.assignmentUpdate.emit(this.assignment);
    this.isDirty = true;
    this.amhAssignmentService.updateDirtyStatus(this.isDirty);

    if (!numericSeqValue || !this.isInteger(numericSeqValue) || numericSeqValue < 0) {
      //this.logger.log(" not valid sequence " + numericSeqValue + " old value " + oldSequence);
      this.assignmentSequenceErrorMsg = "Assignment sequence is incorrect";
      this.emitDisableEnableSaveButtons();
      return;
    }

    this.assignment.sequence = numericSeqValue;
    this.updateAssignmentUniquenessErrorMsg(numericSeqValue);
  }

  assignmentCopiesUpdate(inputElem: HTMLInputElement) {
    //this.logger.log("assignment " + this.assignment.code + " has changed its copies value to " + this.assignment.copies);
    let numericCopiesValue: number = + inputElem.value;
    let oldCopies = this.assignment.copies;
    this.assignmentUpdate.emit(this.assignment);
    this.assignmentCopiesErrorMsg = "";

    if (!numericCopiesValue || !this.isInteger(numericCopiesValue) || numericCopiesValue < 0) {
      //this.logger.log(" not valid copies " + numericCopiesValue + " old value " + oldCopies);
      this.assignmentCopiesErrorMsg = "Assignment copies is incorrect";
      this.emitDisableEnableSaveButtons();
      return;
    }

    this.assignment.copies = numericCopiesValue;
    this.isDirty = true;
    this.amhAssignmentService.updateDirtyStatus(this.isDirty);
  }
 
  private isValidCode(code : string) : boolean {
    //^,$ are the begining and the end of the string respectively
    //to test https://regex101.com/#javascript
    let rex = new RegExp('^[A-Za-z0-9_\.\-]*$');
    return rex.test(code);
  }

  assignmentCodeUpdate(inputElem: HTMLInputElement) {
    //this.logger.log("assignment code changed " + inputElem.value + " assignment code " + this.assignment.code);
    this.isDirty = true;
    this.amhAssignmentService.updateDirtyStatus(this.isDirty);
    this.assignment.code = inputElem.value;
    
    if (!this.isValidCode(inputElem.value)) {
       this.assignmentCodeErrorMsg = "Valid characters are A-Z a-z 0-9 . - _";
       return;
    }

    this.updateAssignmentCodeUniquenessErrorMsg(inputElem.value);
  }

  private confirmAssignmentChange() {
      if (!this.isDirty) {
        //this.logger.log("Assignment has no changes so far");
        this.loadAssignment(this.unconfirmedAssignmentCode);
        return;
      }

      this.alertLostChanges();
  }

  private initialization() {
    this.assignmentTextFilter = "";
    this.assignmentCodeErrorMsg = "";
    this.createNewAssignment();
    this.doDisableInputs();
    this.isDirty = false;
    this.amhAssignmentService.updateDirtyStatus(this.isDirty);
  }

  private afterSaveNewAssignment() {
    this.assignmentTextFilter = "";
    this.assignmentCodeErrorMsg = "";
    this.assignmentSequenceErrorMsg = "";
    this.assignmentCopiesErrorMsg = "";
    this.isCreation = false;
    this.creationStatus.emit(this.isCreation);
    this.isDirty = false;
    this.amhAssignmentService.updateDirtyStatus(this.isDirty);
    this.haveRulesErrors = false;
    this.emitDisableEnableSaveButtons();
  }

  private confirmSelectionTableChanges() {
    let hasAssignErros = this.hasThisAssignmentErrors();
    if (this.isDirty && hasAssignErros) {
        this.alertPreventLostChanges();
        return;
    }

    if (!this.isDirty || hasAssignErros) {
        //this.logger.log("onChanges config.type = [" + this.config.type + "] hasThisAssignmentErrors " + hasAssignErros + " dirty " + this.isDirty);
        this.selectionRollbackType = this.config.type;
        this.initialization();
        return;
    }

    this.alertLostChanges(100, 101,102);
  }

  private alertPreventLostChanges(cancelResponse=300, yesResponse = 301) {
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

  private alertLostChanges(cancelResponse=0, yesResponse = 1, okResponse = 2) {
    this.alert.waitIcon = false;
    this.alert.alertFooter = true;
    this.alert.cancelButton = true;
    this.alert.cancelButtonText = "Cancel";
    this.alert.cancelButtonResponse = cancelResponse;
    this.alert.yesButton = true;
    this.alert.yesButtonText = "Yes";
    this.alert.yesButtonResponse = yesResponse;
    this.alert.okButton = true;
    this.alert.okButtonText = "No";
    this.alert.okButtonResponse = okResponse;
    this.alert.alertHeader = true;
    this.alert.alertTitle = " Alert ";
    this.alert.message = "Do you want to save the changes done?";
    this.alert.open();
  }

  confirmClose(data) {
    //this.logger.log("confirming ------------  " + data);
    
    switch(data) {
      case 0:
      case 400: //on Create Assignment, cancel do nothing.
        //this.logger.log("confirm cancel, do nothing");
        break;
      case 1:
        if (this.isCreation) {
           //If is creation then on save disable assignment code
           this.pendingAssignmentSave = 3;
        }
        this.saveWork.emit({"withRollbackType":false, "saveAndContinue":true});
      break;
      case 2:
        this.loadAssignment(this.unconfirmedAssignmentCode);
      break;
      case 100: //Cancel selection table change
      case 300: //On dirty, Cancel change selection table
        //this.logger.log("change assign cancel, rollback selection table");
        this.rollbackSelection .emit(true);
        break;
      case 101: // save and change selection table
        this.saveWork.emit({"withRollbackType":true, "saveAndContinue":true});
        //TODO: do the initialization only if save was OK.
        //TODO: Add parentEventEmitter to do the initialization part.
        this.pendingAssignmentSave = 2;
        this.selectionRollbackType = this.config.type;
        break;
      case 102: //Do not save and just change selection table
      case 301: //On dirty, do not save and just change selection table
        this.initialization();
        this.selectionRollbackType = this.config.type;
        break;
      case 201: //save and go to rule creation
        this.saveWork.emit({"withRollbackType":false, "saveAndContinue":true});
        this.pendingAssignmentSave = 1;
      break;
      case 202: // do not save and go to rule creation
        this.navigateTo(this.pendingNavigationConfig.composentName, this.pendingNavigationConfig.parameters); 
       break;
       case 401: // on Create Assignment, lost changes clicked, so load empty assignment
          this.loadAssignment(this.unconfirmedAssignmentCode);
        break;
      default: return;
    }
    
  }

  actionRuleNavigation(ruleCode) {
    let parameters = { 'code': ruleCode, 'return_to':'AMHAssignmentEdit','params':'code='+this.assignment.code+'&st='+this.config.type }
    if (this.hasThisAssignmentErrors()) { //If there are some errors do not navigate
      return;
    }
    if (!this.isDirty) {
      this.navigateTo("AMHRuleCreate", parameters);
      return;  
    }
    this.pendingNavigationConfig = {"composentName":"AMHRuleCreate" , "parameters": parameters}
    this.alertLostChanges(0, 201,202);
  }
  
  private navigateTo(targetComponent : string, parameters : any) {
    this.router.parent.navigate([targetComponent, parameters])
  }

  actionSetDirtyTrue() {
    this.isDirty = true;
    this.amhAssignmentService.updateDirtyStatus(this.isDirty);
  }
}
