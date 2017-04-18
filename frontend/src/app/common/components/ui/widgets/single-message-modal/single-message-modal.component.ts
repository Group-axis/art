import {Injectable, EventEmitter} from 'angular2/core';
import {AlertComponent} from 'ng2-bootstrap/components/alert';
import {FormFieldComponent} from '../form-field';
import { NG_VALIDATORS, NgClass, Validators, Control, ControlGroup, FormBuilder, CORE_DIRECTIVES, FORM_DIRECTIVES, FORM_PROVIDERS } from '@angular/common';
import {User} from '../../../../../models/users';
import {Message} from '../../../../../models/simulation';
import {Store, Auth, Logger} from '../../../services';
import {CustomValidatorsComponent} from '../../controls';
import {AMHRoutingService} from "../../../../../amh-routing/amh-routing.service";

//this.logger.log('`SingleMessageModal` component loaded asynchronously');

export const SingleMessageModalObjectMetadata = {
        selector: 'single-message-modal',
        template: require('./single-message-modal.html'),
        directives: [AlertComponent, FormFieldComponent, FORM_DIRECTIVES, CORE_DIRECTIVES],
        providers: [ FORM_PROVIDERS, AMHRoutingService, Auth],
        styles: [`
                .alert-danger {
                  opacity: 0.7;
                }
                .alert-warning { 
                  opacity: 0.7;
                  color: #ffffff;
                  background-color: #007aff;
                  border-color: #007aff;
                }
              `]
    };

@Injectable()
export class SingleMessageModalComponent {
  private isDirty : boolean = false;
  private swiftMessage : string = "";
  private isReadOnly : boolean = false;
  private doneSender : EventEmitter<any>;
  private statusAlert : any;
  private successAlert : any;
  private errorAlert : any;
  private messageId : string = "-1";
  private messageName : string = "";
  private messageForm: ControlGroup;
  private saveInProcess : boolean = false;
  private messageNameCtrl : Control;

  constructor(private amhRoutingService: AMHRoutingService, private fb: FormBuilder, private auth: Auth, private logger : Logger) {
    this.logger.log("SingleMessageModalComponent constructor amhRoutingService = "+ this.amhRoutingService);
    let messageNameCtrl = new Control('', Validators.compose([Validators.required, CustomValidatorsComponent.validMessageName]));
    let contentCtrl = new Control('', Validators.required);
    this.messageForm = this.fb.group({
      'name': messageNameCtrl,
      'content': contentCtrl
    });
    
  }

  initialize( params : Map<string, string>) {
    let messageId = params.get("messageId");
    let isCreation =  messageId.length == 0
    this.messageId = isCreation ? "-1" : messageId;
    
    this.messageNameCtrl = new Control('', Validators.compose([Validators.required, CustomValidatorsComponent.validMessageName]));
    // if (isCreation) {
    //     messageNameCtrl = new Control('', Validators.compose([Validators.required, CustomValidatorsComponent.validMessageName]), CustomValidatorsComponent.messageNameDuplication);
    // }
    
    let contentCtrl = new Control('', Validators.required);
    this.messageForm = this.fb.group({
      'name': this.messageNameCtrl,
      'content': contentCtrl
    });
    
     
    this.swiftMessage = "";
    this.messageName = "";
    if (messageId) {
      this.loadMessage(messageId);
    }
  }

  private loadMessage(messageId : string) {
    this.amhRoutingService.findMessage(messageId).
     subscribe(
       msg => { 
         this.logger.log("received msg: "+JSON.stringify(msg.content));
         this.swiftMessage = msg.content;
         this.messageName = msg.name;
         },
       error => { this.logger.error("error while loading message: "+error.message);},
       () => {this.logger.log("message load done!!"); }
     );
  }

  closeAlert() {
   // this.statusAlert = undefined;
  }

  getSwiftMessage() {
    return this.swiftMessage;
  }

  actionSubmit() {
    this.logger.log("actionSubmit " );
    let user = this.auth.getUser();
    let message = new Message(this.messageId, this.messageName, this.swiftMessage);
    this.statusAlert = {msg: 'The Message has been sent. The response status will be displayed soon....  ', type: 'warning', closable: true};
    this.saveInProcess = true;
    if(this.messageId != "-1") {
      this.amhRoutingService.saveMessage(message, user)
      .subscribe(
        msg => {},
        error => {
           this.statusAlert = undefined;
           this.errorAlert = {msg: 'An error has occurred while saving the Message.', closable: true};
           this.saveInProcess = false;
        },
        () => {
          this.statusAlert = undefined;
           this.successAlert = {msg: 'The Message has been successfuly updated.', closable: true};
           this.saveInProcess = false;
           setTimeout(() => this.doneSender.emit("done"), 1000);
        }
      );
    } else {
      this.amhRoutingService.createMessage(message, user)
      .subscribe(
        msg => {},
        error => {
           this.statusAlert = undefined;
           this.errorAlert = {msg: 'An error has occurred while saving the Message.', closable: true};
           this.saveInProcess = false;
        },
        () => {
          this.statusAlert = undefined;
          this.successAlert = {msg: 'The Message has been successfuly created.', closable: true};
          this.saveInProcess = false;
          setTimeout(() => this.doneSender.emit("done"), 1000);
        });
    }
  }

  actionCancel() {
    this.logger.log("actionCancel " );
     this.doneSender.emit("cancel"); 
  }

  actionVerifyMessageName(name : string) {
    this.logger.debug("verifying name : "+ name);
    let found = false;
    this.amhRoutingService.findMessageByName(name)
    .subscribe(
      data => {
        this.logger.debug("from validator "+ JSON.stringify(data));
        if (data["found"]) {
          found = true;
          this.messageNameCtrl.setErrors({ "messageNameDuplicated": true });
          this.logger.log(" message name found [" + data["message"]["name"] + "] ");
          
        } else {
          this.logger.log("message name " +name + " not found!");
          }
      },
      error => {
        this.logger.error("an error ocurred while looking for massage name " + name + " : " + error.message);
        this.messageNameCtrl.setErrors({ "messageNameDuplicatedError": true });
      },
      () => {
        this.logger.log("findMessageByName done");
        if (!found) {
          this.logger.log("message name " + name + " not found!");
          
        }
      }
    )
  }

  actionLimit(value : string, max : number) {
      this.logger.log("  value.length "+value.length + " max " + max );
    if (value.length >= max) {
      this.messageName = value.substr(0,max);
      return false;
    }
  }

}
