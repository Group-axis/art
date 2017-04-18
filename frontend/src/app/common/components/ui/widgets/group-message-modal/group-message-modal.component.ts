import {Injectable, EventEmitter} from 'angular2/core';
import {Observable} from 'rxjs/Observable';
import {AlertComponent} from 'ng2-bootstrap/components/alert';
import {FormFieldComponent} from '../form-field';
import { NG_VALIDATORS, NgClass, Validators, Control, ControlGroup, FormBuilder, CORE_DIRECTIVES, FORM_DIRECTIVES, FORM_PROVIDERS } from '@angular/common';
import {User} from '../../../../../models/users';
import {Message} from '../../../../../models/simulation';
import {FileUploadService, Auth, Config, Logger} from '../../../services';
import {AMHRoutingService} from "../../../../../amh-routing/amh-routing.service";
import {CustomValidatorsComponent} from '../../controls';


//this.logger.log('`GroupMessageModal` component loaded asynchronously');

export const GroupMessageModalObjectMetadata = {
  selector: 'group-message-modal',
  template: require('./group-message-modal.html'),
  directives: [AlertComponent, FormFieldComponent, FORM_DIRECTIVES, CORE_DIRECTIVES],
  providers: [Config, FileUploadService, FORM_PROVIDERS, AMHRoutingService, Auth]
};

@Injectable()
export class GroupMessageModalComponent {
  private isDirty: boolean = false;
  private isReadOnly: boolean = false;
  private doneSender: EventEmitter<any>;
  private statusAlert: any;
  private successAlert: any;
  private errorAlert: any;
  private messageId: number = -1;
  private messageName: string = "";
  private messagesForm: ControlGroup;
  private saveInProcess: boolean = false;
  private messageNameCtrl: Control;
  private uploadProgress: number = 0;
  private fileList: File[] = new Array<File>();

 private activeValidation : boolean = true;

  constructor(private amhRoutingService: AMHRoutingService,
    fb: FormBuilder,
    private auth: Auth,
    private fileUploadService: FileUploadService,
    private config: Config, private logger : Logger) {
    this.logger.log("AMHSimulatorModalImportComponent constructor amhRoutingService = " + this.amhRoutingService);
    config.load();
    this.messageNameCtrl = new Control('', Validators.required) 
    this.messagesForm = fb.group({
      'name': this.messageNameCtrl
    });
  }

  initialize(params: Map<string, string>) {

  }


  private fileSelection(fileInput: any) {
    
    let files: FileList = fileInput.target.files;
    this.fileList = new Array<File>();
    for (let i = 0, length = files.length; i < length; i++) {
      this.logger.log("file selected " + files.item(i).name);
      this.fileList.push(files.item(i));
    }
    //  this.fileList = fileInput.target.files.forEach( f => this.fileList.push(f));
    this.messageName = this.getFileName(files);
    this.activeValidation = false;
    
    this.amhRoutingService.findMessageByName(this.messageName)
     .subscribe(
      this.verifyMessageOnNext(this.messageName),
      this.verifyMessageNameOnError(this.messageName),
      this.verifyMessageNameOnComplete
    )

    //this.logger.log("file selected " + this.fileList);
  }

  private getFileName(files: FileList) : string {
    if (this.fileList.length == 0) {
      return "";
    }
    let fileName = this.fileList[this.fileList.length - 1].name;
    let dotIndex = fileName.indexOf(".");
    if (dotIndex > -1) {
      fileName = fileName.substring(0, dotIndex);
    }
    return fileName;
  }
  // private loadMessage(messageId : string) {
  //   this.amhRoutingService.findMessage(messageId).
  //    subscribe(
  //      msg => { 
  //        this.logger.log("received msg: "+JSON.stringify(msg.content));
  //        this.swiftMessage = msg.content;
  //        this.messageName = msg.name;
  //        },
  //      error => { this.logger.error("error while loading message: "+error.message);},
  //      () => {this.logger.log("message load done!!"); }
  //    );
  // }

  closeAlert() { }

  // getSwiftMessage() {
  //   return this.swiftMessage;
  // }

  actionSubmit() {
    this.logger.log("actionSubmit ");
    this.activeValidation = false;

    this.amhRoutingService.findMessageByName(this.messageName)
    .flatMap( response => {
      this.logger.debug("response: "+JSON.stringify(response));
      
      let observerResponse = Observable.create(observer => {
            observer.next(response);
            observer.complete();
            return () => this.logger.log('dispose findMessageByName observer ')
          });

      if (response["found"]) {
          this.logger.error("ERRORRR: msgNameFound ? "+response["found"]);
          // this.messageNameCtrl.setErrors({ "messageNameDuplicated": true });
          return observerResponse;
      }
      this.statusAlert = { msg: 'The Message has been sent. The response status will be displayed soon....  ', type: 'warning', closable: true };
    this.saveInProcess = true;

    this.uploadHandler().then(data => {
      this.saveInProcess = false;
      this.statusAlert = undefined;
      this.successAlert = { msg: 'The Message has been successfuly load.', closable: true };
      setTimeout(() => this.doneSender.emit("done"), 1000);
    }, error => {
      this.statusAlert = undefined;
      this.errorAlert = { msg: 'An error has occurred while saving the Message.', closable: true };
      this.saveInProcess = false;
    });
    
      return observerResponse;
    })
    .subscribe(
      this.verifyMessageOnNext(this.messageName),
      this.verifyMessageNameOnError(this.messageName),
      this.verifyMessageNameOnComplete
    )
    

  }


  private uploadHandler(): Promise<any> {
    let result: any;

    this.fileUploadService.getObserver()
      .subscribe(progress => {
        this.uploadProgress = progress;
        this.logger.log("Progress " + progress + "%");
      },
      error => this.logger.error(""+error.message),
      () => this.logger.log("import done!!!!!"));

    try {
      let user = this.auth.getUser();
      let params = new Map<string, string>();
      params.set("username", user.username);
      params.set("creationDate", String(new Date().getTime()));
      params.set("group", this.messageName);


      return this.fileUploadService.upload(this.config.get("simulationBackUrl") + "/messages/amh/import", this.fileList, params, 50);
    } catch (error) {
      this.logger.error("An error has occurred while importing: " + error);
      document.write(error)
    }

  }

  actionCancel() {
    this.logger.log("actionCancel ");
    this.doneSender.emit("cancel");
  }

  private verifyMessageOnNext(name : String) { 
       return data => {
        this.logger.debug("from validator "+ JSON.stringify(data));
        if (data["found"]) {
          this.messageNameCtrl.setErrors({ "messageNameDuplicated": true });
          this.logger.log(" message name found [" + data["message"]["name"] + "] ");
          
        } else {
          this.logger.log("message name " +name + " not found!");
          }
      };
  }

  private verifyMessageNameOnError(name : String) { 
       return error => {
        this.logger.error("an error ocurred while looking for message name " + name + " : " + error.message);
        this.messageNameCtrl.setErrors({ "messageNameDuplicatedError": true });
      };
  }

 private verifyMessageNameOnComplete = () => {
        this.logger.log("findMessageByName done");
      };

  actionVerifyMessageName(name : string) {
    this.logger.debug("verifying name : "+ name);
    this.amhRoutingService.findMessageByName(name)
    .subscribe(
      this.verifyMessageOnNext(name),
      this.verifyMessageNameOnError(name),
      this.verifyMessageNameOnComplete
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
