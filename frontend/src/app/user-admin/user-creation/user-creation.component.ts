import {ElementRef, Component, ViewChild, Directive, provide} from 'angular2/core';
import { NG_VALIDATORS, NgClass, Validators, Control, ControlGroup, FormBuilder, CORE_DIRECTIVES, FORM_DIRECTIVES, FORM_PROVIDERS } from '@angular/common';
import { ACCORDION_DIRECTIVES, BUTTON_DIRECTIVES } from 'ng2-bootstrap';
import { Router, RouteParams} from 'angular2/router';
import {FormFieldComponent} from '../../common/components/ui/widgets/form-field';
import {FormLabelComponent} from '../../common/components/ui/widgets/label';
import {User} from '../../models/users';
import {FormPickListComponent, FormPickListValueAccessor, listRequired } from '../../common/components/ui/widgets/pick-list';
import {IText, IdCodeDescription} from '../../models/referential';
import {UserService} from "../";
import {Observable} from 'rxjs/Observable';
import {FormFieldComponentExample, BootstrapFormDirective, BootstrapInputDirective, LabelsComponent, LabelsValueAccessor, notEmpty} from '../../common/components/ui/controls';
import {Alert} from '../../common/components/ui/widgets/modal';
import {CustomValidatorsComponent} from '../../common/components/ui/controls';
import {Config, Store, Auth, Logger} from '../../common/components/services';
import {MenuConfig} from '../../models/menu';
import {HeaderSecondary} from '../../header-secondary';
import {Permissions} from '../../common/directives';

const USER_CREATION_LOST_MSG = "All changes will be lost, Do you want to continue?";
const USER_CREATION_RESET_MSG = "The user's password will be reset, Do you want to continue?";

@Component({
  selector: 'user-creation',
  styles: [`
    h1 {
      font-family: Arial, Helvetica, sans-serif
    }
  `],
  template: require('./user-creation.html'),
  directives: [ACCORDION_DIRECTIVES, BUTTON_DIRECTIVES, CORE_DIRECTIVES, FORM_DIRECTIVES, HeaderSecondary,  
    FormFieldComponent, FormLabelComponent, FormFieldComponentExample, BootstrapFormDirective, BootstrapInputDirective, LabelsComponent, LabelsValueAccessor, NgClass,
     Alert, FormPickListComponent, FormPickListValueAccessor, Permissions],
  providers: [UserService, FORM_PROVIDERS, Store, Auth]
})
export class UserManagementComponent {
  @ViewChild(Alert) alert;
  
  private userForm: ControlGroup;
  private user: User = User.empty();
  private originalUser : User;
  private originalProfiles: Array<IdCodeDescription> = [];
  private userDataSource: Observable<any>;
  private criteriaDataSource: Observable<IdCodeDescription>;
  private availableProfiles: Array<IdCodeDescription> = [];
  private selectedProfiles: Array<IdCodeDescription> = [];
  private availableProfilesMap: Map<number, IdCodeDescription> = new Map<number, IdCodeDescription>();

  private nodeSelected: string = "N/A";
  private returnToString: string;
  private returnParameters: any = { };
  private parameters = {};
  private isCreation: boolean = false;
  private criteriaPosition: number;
  private isDirty : boolean = false;
  private navigateUrl: string;
  private hasUserPermissions : boolean = true;

  private menuConfig : Array<MenuConfig> = [
    new MenuConfig("fa fa-home","Home","Home"),
    new MenuConfig("fa fa-users","UserHome","User Administration"),
    new MenuConfig("fa fa-user","","User Management")];

  constructor(private router: Router, private userService: UserService, 
        routeParams: RouteParams, fb: FormBuilder, private store : Store, auth: Auth, private logger : Logger) {
    
    this.isCreation = routeParams.params['username'] === undefined || routeParams.params['username'] === '';
    let usernameParam = routeParams.params['username'];
    usernameParam = (typeof usernameParam).toString() == 'object'? usernameParam[0] : usernameParam;
    this.logger.debug("receiving "+usernameParam[0]+"... "+routeParams.params['username']+" type "+typeof usernameParam);
    this.userDataSource = this.userService.findUserByUsername(usernameParam);
    this.criteriaDataSource = this.userService.findAllProfiles();
    this.returnToString = routeParams.params['return_to'] || 'UserHome';
    this.setReturnToParameters(routeParams.params['params']);

    this.hasUserPermissions = auth.hasPermission(["user.modify"]) == 1;
    let usernameCtrl = new Control('', Validators.required);
    
    if (this.isCreation && this.hasUserPermissions) {
      usernameCtrl = new Control('', Validators.compose([Validators.required,CustomValidatorsComponent.validUsername]), CustomValidatorsComponent.usernameDuplication); 
    }
    let firstNameCtrl = new Control('', Validators.required);
    let lastNameCtrl = new Control('', Validators.required);

    this.userForm = fb.group({
      'username': usernameCtrl,
      'firstName': firstNameCtrl,
      'lastName': lastNameCtrl,
      'profiles': ['', listRequired]
    });



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

  ngOnInit() {
    this.logger.log('hello `User Creation` component');
    this.isDirty = false;

    this.userDataSource.subscribe(
      resp => {
        this.logger.log("User found ["+resp.found+"] from service ");
        this.user = resp.found ? User.getFromObject(resp.value) : User.empty();
        this.originalUser = resp.found ? User.getFromObject(resp.value) : User.empty();
        this.fillupSelectedProfiles();
      },
      err => {
        this.logger.log("Can't get a user. Error code: %s, URL: %s ", err.status, err.url);
        this.user = User.empty();
      },
      () =>  this.logger.log('User retrieved')
    );

    //TODO: change the response type of dataSource to Array[]
    let profileBuffer : Array<IdCodeDescription> = [];

    this.criteriaDataSource.subscribe(
      data => {
        this.logger.debug("gettting profile "+ data.description);
        this.availableProfilesMap.set(data.id, data);
        profileBuffer.push(data);
      },
      err => this.logger.log("Can't get profiles. Error code: %s, URL: %s ", err.status, err.url),
      () => { 
        this.logger.log('Profile(s) are retrieved');
        this.availableProfiles = profileBuffer;
        this.fillupSelectedProfiles();
      }
    );
  }
  
  private removeFromAvailableProfiles(selectedProfile : IdCodeDescription) {
      let index = this.availableProfiles.indexOf(selectedProfile,0);
      if (index > -1) {
        this.availableProfiles.splice(index,1);
      }
  }

  private fillupSelectedProfiles() {
    if (this.user.profiles.length == 0 || this.availableProfilesMap.size == 0) {
      this.logger.debug("fillupSelectedProfiles called without success");
      return;
    }
         
    this.selectedProfiles = this.user.profiles
    .map(profile => {
      let selectedProfile = this.availableProfilesMap.get(profile);
      this.removeFromAvailableProfiles(selectedProfile); 
      return selectedProfile ? selectedProfile : new IdCodeDescription(0,"",""); 
    })
    .filter(profile => profile.id > 0);
    //saving original profile list
    this.originalProfiles = [];
    this.selectedProfiles.forEach(profile => this.originalProfiles.push(profile))
    
    this.logger.debug(" selectedProfiles => "+JSON.stringify(this.selectedProfiles));
  } 

  alertOpen() {
    this.alert.alertFooter = false;
    this.alert.cancelButton = false;
    this.alert.okButton = false;
    this.alert.alertHeader = true;
    this.alert.alertTitle = "Alert";
    this.alert.message = "Save in progress...";
    this.alert.cancelButtonText = "Ok";
    this.alert.open();
  }

  confirmClose(data) {
    this.logger.log("confirming ------------  " + data);
    let DO_NOT_NAVIGATE = false;
    switch(data) {
      case 300: // Reset or Navigate Cancel 
        //Do nothing
      break;
      case 301: //Navigate yes, lost changes
        this.router.parent.navigate([this.navigateUrl, this.getParameters()]);
      break;
      case 401: //Reset yes, user's password reset'
        this.logger.debug("reseting user password..." );
        this.userService.resetPassword(this.user)
          .subscribe(
             data => {
              this.logger.log("[resetPassword] %s", data);
            },
            err => {
              this.logger.log("[resetPassword] The user password cannot be reset. Error code: %s, URL: %s ", err.status, err.url);
              this.alert.message = "The user password cannot be reset !!";
              this.closeAlert(DO_NOT_NAVIGATE);
            },
            () => {
              this.logger.log("[resetPassword] done successfuly");
              this.alert.message = "The user password was reset successfuly !!";
              this.closeAlert(DO_NOT_NAVIGATE);
            }
          );

      break;
    }
  }

  closeAlert(navigate : boolean = true) {
    setTimeout(() => this.alert.cancel(), 1000);
    if (navigate) {
      setTimeout(() => this.router.parent.navigate([this.returnToString, this.getParameters()]), 1200);
    }
  }

  private getParameters() : any {
    return this.parameters;
  }
  private disableSaveButton() {
    // this.ruleForm. 
  }

  actionResetPassword() {
    this.showAlert(USER_CREATION_RESET_MSG, 300, 401);
  }

  actionSave() {
    this.logger.log("saving user ..." + this.user.username + " isCreation " + this.isCreation);
    this.user.profiles = this.selectedProfiles.map(profile => profile.id);
    this.originalUser.profiles = this.originalProfiles.map(profile => profile.id);

    this.alertOpen();
    let send : Observable<any>;
    let resetPassword : Observable<any> = undefined;
    // let audit : Observable<any>;
    if (this.isCreation) {
      send = this.userService.createUser(this.user);
      // audit =  this.userService.createUserAudit(this.user);
    } else {
      send = this.userService.saveUser(this.user);
      resetPassword = this.user.isNotActive() ? this.userService.resetPassword(this.user) : resetPassword;
      // audit =  this.userService.updateUserAudit(this.originalUser, this.user);
    }
    send.subscribe(
      data => {
        this.logger.log("[saveUser] %s", data);
      },
      err => {
        this.logger.log("[saveUser] The user cannot be saved. Error code: %s, URL: %s ", err.status, err.url);
        this.alert.message = "The user cannot be saved !!";
        this.closeAlert();
      },
      () => {
        this.logger.log("[saveUser] done successfuly");
        this.alert.message = "The user was saved successfuly !!";
        this.closeAlert();
        if (resetPassword) {
          resetPassword.subscribe(() => this.logger.debug("password reset done"));
        }
        // audit.subscribe(
        //     ok=>this.logger.info("audit ok"),
        //     err => this.logger.error("while auditing user create/update: "+err.message),
        //     ()=> this.logger.info("audit done"));
      }
    );

  }

  private actionCancel() {
    this.verifyAndNavigate(this.returnToString, 300, 301);
  }

  

private showAlert(message: string, cancelResponse: number, yesResponse : number) {
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
    this.alert.message = message;
    this.alert.open();
  }
  
  private verifyAndNavigate(link : string, cancelResponse : number, yesResponse : number) {
     
    if (this.isDirty) {
        this.logger.debug("inside if "+ this.isDirty);
        this.navigateUrl = link;
        this.showAlert(USER_CREATION_LOST_MSG, cancelResponse, yesResponse);
        return;
    }

    this.logger.debug("it was not dirty going to "+link); 
    this.router.parent.navigate([link, this.getParameters()]);
  }


   actionNavigate(link : string) {
     this.verifyAndNavigate(link, 300, 301);
   }
}


