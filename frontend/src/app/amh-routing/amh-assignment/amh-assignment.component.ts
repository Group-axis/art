import {Component, ViewChild} from 'angular2/core';
import {AMHAssignmentService} from "../amh-service";
import { Router, RouteParams} from 'angular2/router';
import {AssignmentConfig, AssignType, AssignmentUnique} from "../../models/routing-amh";
import {Option} from '../../models/referential/option';
import {Alert} from '../../common/components/ui/widgets/modal';
import {MenuConfig} from '../../models/menu';
import {HeaderSecondary} from '../../header-secondary';
import {AMHAssignmentContentComponent} from './amh-assignment-content';
import {AMHSelectionTableComponent} from '../amh-selection-table';
import { Permissions, NotPermissions } from '../../common/directives';
import {Auth, Logger} from '../../common/components/services';

//this.logger.log('`AMHAssignment` component loaded asynchronously');

@Component({
  selector: 'amh-assignment',
  template: require('./amh-assignment.html'),
  providers: [AMHAssignmentService, Auth], 
  directives: [AMHSelectionTableComponent, AMHAssignmentContentComponent, Alert, HeaderSecondary, Permissions, NotPermissions]
})
export class AMHAssignmentComponent {
  @ViewChild(Alert) alert;
  private disabledSaveButtons : boolean = false;
  private isCreation: boolean;

  private assignment: AssignmentUnique = undefined;
  private navigateUrl: string;
  private returnToString: string;
  private returnParameters: any = { };
  private assignmentConfig : AssignmentConfig;
  private optionRollback: Option;
  private saveStatus: string ="";
  private  confirmation : any = undefined;
  
  private menuConfig : Array<MenuConfig> = [
    new MenuConfig("fa fa-home","Home","Home"),
    new MenuConfig("fa fa-sitemap","AMHHome","AMH Routing"),
    new MenuConfig("fa fa-cloud-download","","Back End Assignment Rule Criteria")];
  
  private selectionTables : Array<Option> = [
    new Option(1,"BK_CH","Backend Channel"),
    new Option(2,"DTN_CPY","Distribution Copy"),
    new Option(4,"FEED_DTN_CPY","Feedback Distribution Copy")
  ];
  private defaultOption : Option;
  private saveAndContinue: boolean;
  private withRollbackType : AssignType;
  
  constructor(private router: Router, routeParams: RouteParams, private amhAssignmentService: AMHAssignmentService,
      private auth : Auth, private logger : Logger) { 
    this.setReturnTo(routeParams.params['return_to']);
    this.addReturnParamter("st", routeParams.params['st']);
    this.updateSelectionTable(this.selectedAssignmentType(+ routeParams.params['st']));
  }

  ngOnInit() {
    //this.logger.log('hello `AMH assignment ` component');
  }

  private selectedAssignmentType(assignType : AssignType) : Option {
    let typeSelected = this.selectionTables.find((type) => { return type.id === assignType; });
    return typeSelected ? typeSelected : this.selectionTables[0]; 
  }
  
  private addReturnParamter(name : string, value : any) {
    if (!value) {
      return;
    }

    this.returnParameters[name] = value;
  }
   
  private setReturnTo(params: string) {
    this.returnToString = 'AMHHome';

    //this.logger.log(" receiving params to return = " + params);

    if (!params || params.length == 0) {
      return;
    }

    let parameters = params.split('&');
    this.returnToString = parameters[0] || 'AMHHome';
    //this.logger.log(" paramteres to return " + parameters);

  }

  private alertOpen() {
    this.alert.alertFooter = false;
    this.alert.cancelButton = false;
    this.alert.okButton = false;
    this.alert.alertHeader = true;
    this.alert.alertTitle = "Alert";
    this.alert.message = "Save in progress...";
    this.alert.cancelButtonText = "Ok";
    this.alert.cancelButtonResponse = 0;
    this.alert.open();
  }

  private closeAlert(saveAndContinue: boolean, withError? : boolean) {
    //setTimeout(() => this.alert.cancel(), 1000);
    this.alert.cancel();
    if (!saveAndContinue && !withError) {
      setTimeout(() => this.router.parent.navigate([this.returnToString, this.returnParameters]), 1200);
    }
  }

 //ROMOVE this useless method
  private isValidAssigment(): any {
    if (!this.assignment) {
      return { isValid: false, errorMsgs: ["No assignment defined"] };
    }

    // let errorMessages = this.validateSequences();
    let errorMessages = "";


    return { isValid: (!errorMessages || errorMessages.length == 0), errorMsgs: errorMessages };
  }

 private msgEmptyRules() {
    this.alert.waitIcon = false;
    this.alert.alertFooter = true;
    this.alert.okButton = false;
    this.alert.alertHeader = true;
    this.alert.alertTitle = "Alert";
    this.alert.message = "No rules assigned. Every message will be send to the assigned backend(s).";
    this.alert.cancelButton = true;
    this.alert.cancelButtonText = "Cancel";
    this.alert.cancelButtonResponse = 0;
    this.alert.yesButton = true;
    this.alert.yesButtonText = "OK";
    this.alert.yesButtonResponse = 1;
    this.alert.open();
  }

  alertResponse(resp){
    switch(resp) {
      case 0: //Empty rules cancel
        this.saveStatus = "Cancel";
      break;
      case 1: //Empty rules OK
        this.saveStatus = "";
        this.doSave(this.saveAndContinue, this.withRollbackType);
      break;
      case 300: //Navigation cancel response
        //Do nothing
      break;
      case 301: //Navigation Yes response, go ahead.
        this.router.parent.navigate([this.navigateUrl, this.returnParameters]);
      break;
    }
  }

  private save(saveAndContinue: boolean, withRollbackType ? : AssignType) {
    if (this.assignment.rules.length == 0) {
      this.msgEmptyRules();
      this.saveAndContinue = saveAndContinue;
      this.withRollbackType = withRollbackType; 
      return;
    }
    this.doSave(saveAndContinue, withRollbackType);
  }

  private doSave(saveAndContinue: boolean, withRollbackType ? : AssignType) {
    let assignmentStatus = this.isValidAssigment();
    if (!assignmentStatus.isValid) {
      assignmentStatus.errorMsgs.forEach(x => this.logger.error(x));
    } 
    // else {
    //   this.logger.log("Everything is O.K assignment with sequence " + this.assignment.sequence);
    //   this.assignment.rules.forEach(r => this.logger.log("       " + r.code + " - " + r.sequence));
    // }

    let assignmentType = withRollbackType ? this.optionRollback.id : this.defaultOption.id;
  //  this.logger.log("saving assignment ..." + this.assignment.backendPrimaryKey);
    this.alertOpen();
    let send;
    
    if (this.isCreation) {
      send = this.amhAssignmentService.createAssignment(assignmentType, this.assignment, this.auth.getUser());
    } else {
      send = this.amhAssignmentService.saveAssignment(assignmentType, this.assignment, this.auth.getUser());
    }

    send.subscribe(
      data => {
        //this.logger.log("[saveRule] %s", data);
      },
      err => {
        this.logger.error("[saveRule] Can't get assignments. Error code: %s, URL: %s ", err.status, err.url);
        this.alert.message = "An error has occurred, the backend cannot be assigned !!";
        this.closeAlert(saveAndContinue, true);
        this.saveStatus = "Error";
        if (saveAndContinue) {
          this.confirmation = {msg: 'An error has occurred, while saving this assignment !!', type: 'danger', closable: true};
        }
      }, () => {
        //this.logger.log("[saveRule] assignment's rules from backend [%s,%s+] retrieved");
        this.alert.message = "Assignment done sucessfuly !!";
        this.closeAlert(saveAndContinue);
        if (saveAndContinue) {
          this.confirmation = {msg: 'Assignment saved', type: 'success', closable: true};
        }
        this.saveStatus = "OK";
        this.isCreation = false;
      }
    );
  }

  private actionCancel() {
   // this.logger.log("canceling assignment ...");
    this.verifyAndNavigate(this.returnToString, 300, 301);
    //this.router.parent.navigate([, this.returnParameters]);
  }

  private updateDisabledButtons(data) {
  //  this.logger.log("disabledSaveButtons ------------ " + data);
    this.disabledSaveButtons = data;
  }

  private saveWork(config) {
   // this.logger.log("saving work saveAndContinue=" + config.saveAndContinue+ " withRollback= "+  config.withRollbackType);
    this.save(config.saveAndContinue, config.withRollbackType);
  }

  private updateAssignment(assignment) {
  //  this.logger.log("assignment updated ------------  " + assignment);
  //  assignment.rules.forEach(rule => this.logger.log(" rule code in assignment " + rule.code));
    this.assignment = assignment;
  }

  private updateSelectionTable(option) {
    let codeToFind = this.defaultOption ? this.defaultOption.code : option.code; 
    this.optionRollback = this.selectionTables.find((innerOption) => { return innerOption.code === codeToFind; });
    this.defaultOption = option;
    this.assignmentConfig = new AssignmentConfig(option.id);
   // this.logger.log("selection table updated ------------  " + option.description + "  MAX " + this.assignmentConfig.maxBackendsAllowed);
  }

  private rollbackSelectionTable(rollbackCommand) {
    //  this.logger.log("rolling back to code " + this.optionRollback.code);
      if (this.assignmentConfig.type == this.optionRollback.id) {
        this.logger.debug("No changes to rollback");
        return;
      }
      this.defaultOption = this.optionRollback;
      this.assignmentConfig = new AssignmentConfig(this.defaultOption.id);
  }

  actionUpdateCreationStatus(isCreation : boolean) {
    this.isCreation = isCreation;
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

  actionNavigate(link : string) {
    // this.logger.debug(" going to "+link);
    // this.logger.debug(" is dirty ?? "+this.amhAssignmentService.isAssignmentStatusDirty());
    this.verifyAndNavigate(link, 300, 301);
  }

  private verifyAndNavigate(link : string, cancelResponse : number, yesResponse : number) {
    let isDirty = this.amhAssignmentService.isAssignmentStatusDirty(); 
    if (isDirty) {
        this.logger.debug("inside if "+ isDirty);
        this.navigateUrl = link;
        this.alertPreventLostChanges(cancelResponse, yesResponse);
        return;
    }

  this.logger.debug("it was not dirty going to "+link); 
    this.router.parent.navigate([link, this.returnParameters]);
  }
}
