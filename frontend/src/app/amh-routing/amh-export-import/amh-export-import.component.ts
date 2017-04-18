import {ElementRef, Component, ViewChild} from 'angular2/core';
import { NgClass, Validators, ControlGroup, FormBuilder, CORE_DIRECTIVES, FORM_DIRECTIVES, FORM_PROVIDERS } from '@angular/common';
import { ACCORDION_DIRECTIVES, BUTTON_DIRECTIVES } from 'ng2-bootstrap';
import { Router, RouteParams} from 'angular2/router';
import {FormFieldComponent} from '../../common/components/ui/widgets/form-field';
import {FormLabelComponent} from '../../common/components/ui/widgets/label';
import {AMHRule} from '../../models/routing-amh';
import {AMHRoutingService} from "../amh-routing.service";
import {Observable} from 'rxjs/Observable';
import {FormFieldComponentExample, BootstrapFormDirective, BootstrapInputDirective, LabelsComponent, LabelsValueAccessor, notEmpty} from '../../common/components/ui/controls';
import {TreeView, TreeNode, TreeViewAccessor} from '../../common/components/ui/widgets/tree-view';
import {Alert} from '../../common/components/ui/widgets/modal';
import {MenuConfig} from '../../models/menu';
import {HeaderSecondary} from '../../header-secondary';
import {FileUploadService, FileDownloader, Config, Auth, Logger} from '../../common/components/services';

//this.logger.log('`AMHExportImport` component loaded asynchronously');

@Component({
  selector: 'amh-export-import',
  styles: [`
    h1 {
      font-family: Arial, Helvetica, sans-serif
    }
  `],
  template: require('./amh-export-import.html'),
  directives: [ACCORDION_DIRECTIVES, BUTTON_DIRECTIVES, CORE_DIRECTIVES, FORM_DIRECTIVES,
    FormFieldComponent, FormLabelComponent, FormFieldComponentExample, BootstrapFormDirective, BootstrapInputDirective, LabelsComponent, LabelsValueAccessor, NgClass,
    TreeView, TreeViewAccessor, Alert, HeaderSecondary],
  providers: [FORM_PROVIDERS, AMHRoutingService, FileUploadService, FileDownloader, Auth]
})
export class AMHExportImportComponent {
  @ViewChild(Alert) alert;

  // private ruleDataSource: Observable<AMHRule>;
  // private criteriaDataSource: Observable<TreeNode>;
  // private criteriaSearchText: string;
  // private criterias: TreeNode[] = [];
  // private originalCriterias: TreeNode[] = [];
  // private nodeSelected:string="N/A";
  private returnToString: string;
  private parameters = {};
  private actionMode: string = "Import";
  private isImport: boolean;
  private filePath: string;
  private uploadProgress: number = 0;
  private fileList: File[] = new Array<File>();
  /*
  <li><i class="fa fa-home"></i><a [routerLink]=" ['/Home'] ">Home</a></li>
                      <li><i class="fa fa-sitemap"></i><a [routerLink]=" ['AMHHome'] ">AMH Routing</a></li>
                      <li><i class="fa fa-upload"></i>{{actionMode}}</li>
   */
  private menuConfig: Array<MenuConfig>;

  constructor(private router: Router, private amhRoutingService: AMHRoutingService,
    private fileUploadService: FileUploadService,
    private fileDownloader: FileDownloader,
    private config: Config, 
    routeParams: RouteParams,
    private auth : Auth,
    private logger : Logger) {
    this.actionMode = routeParams.params['action'] || 'Import';
    this.isImport = this.actionMode == 'Import';
    this.menuConfig = [
      new MenuConfig("fa fa-home", "/home", "Home"),
      new MenuConfig("fa fa-sitemap", "/amh-routing", "AMH Routing"),
      new MenuConfig("fa fa-cloud-upload", "", this.actionMode)]
  }


  ngOnInit() {
    //this.logger.log('hello `AMH Export Import` component');
    // Get the data from the server
  }

  asyncDataWithWebpack() {
  }

  private fileSelection(fileInput: any) {
    let files: FileList = fileInput.target.files;

    for (let i = 0, length = files.length; i < length; i++) {
      this.fileList.push(files.item(i));
    }
    //  this.fileList = fileInput.target.files.forEach( f => this.fileList.push(f));
    //this.logger.log("file selected " + this.fileList);
  }

  private exportFileNameChanged(fileName: string) {
    this.filePath = fileName;
  }

  alertOpen() {
    this.alert.alertFooter = false;
    this.alert.cancelButton = false;
    this.alert.okButton = false;
    this.alert.alertHeader = true;
    this.alert.alertTitle = this.actionMode + " in progress ";
    this.alert.message = "This message wil be updated when the " + this.actionMode + " finish.";
    this.alert.cancelButtonText = "Ok";
    this.alert.open();
  }

private alertOkCancel(okResponse : number, cancelResponse: number, message : string, okText: string = "Ok" ) {
    this.alert.waitIcon = false;
    this.alert.alertFooter = true;
    this.alert.cancelButton = true;
    this.alert.cancelButtonText = "Cancel";
    this.alert.cancelButtonResponse = cancelResponse;
    this.alert.yesButton = true;
    this.alert.yesButtonText = okText;
    this.alert.yesButtonResponse = okResponse;
    this.alert.okButton = false;
    this.alert.alertHeader = true;
    this.alert.alertTitle = " Alert ";
    this.alert.message = message;
    this.alert.open();
  }

alertResponse(resp){
    switch(resp) {
      case 0: //Import action cancel
      break;
      case 1: //Import action 'Yes'
         this.alertOkCancel(100,0,"Please verify that there is no user connected.");
      break;
      case 100: //Import action 'User verifiycation'
         this.exportImport();
      break;
    }
  }

  confirmClose(data) {
    //this.logger.log("confirming ------------  " + data);
  }

  closeAlert() {
    setTimeout(() => this.alert.cancel(), 1000);
    setTimeout(() => this.router.parent.navigate(["AMHHome"]), 1200);
  }

  private disableExpImpButton(): boolean {
    if (this.isImport) {
      return this.fileList.length == 0;
    } else {
      return !this.filePath || this.filePath.length == 0;
    }
  }

  actionExportImport() {
    if (this.isImport) {
      this.alertOkCancel(1,0," All rules and assignments will be erased. Do you want to continue ?", "Yes");
      return;
    } 

    this.exportImport();
  }

private getSelectedFileName() : String {
  if (this.fileList.length == 0) {
    return "";
  }
  return this.fileList[0].name;
}

  private exportImport() {
    this.alertOpen();
    //this.logger.log("export import ..." + " isImport " + this.isImport + " selected file " + this.filePath);
    if (this.isImport) {
      this.uploadHandler().then(data => {
        this.alert.message = this.actionMode + " Done.";
        // this.closeAlert();
        //this.logger.log(" PROMISE " + data);
      }, error => {
        //this.logger.log(" ERROR while importing " + error);
        // this.alert.message = " An error has occurred ["+error+"] while processing the " + this.actionMode + ".";
        this.alert.message = " An error has occurred on file '"+this.getSelectedFileName()+"'. Please verify its content.";
      });
      //   let fromIndex = this.filePath.lastIndexOf('\\');
      //  send = this.amhRoutingService.import("c:/demo/"+this.filePath.substr(fromIndex+1));
    } else {
      // this.logger.log("[EXPORT] just before calling export post on server");
      let send = this.amhRoutingService.export("UNKNOWN", "DEFAULT", this.filePath, this.auth.getUser());
      // this.logger.log("[EXPORT] after returning observer of export post");
      send.subscribe(
        data => {
          //this.logger.log("[" + this.actionMode + "] %s", data);
          this.fileDownloader.download(this.config.get("backUrl") + "/amhrouting/export/" + data.response, data.response);

          // this.amhRoutingService.getExportedFile(data.response);
        },
        err => {
          this.logger.error("[" + this.actionMode + "] Can't be done. Error code: %s, URL: %s ", err.status, err.url),
            this.alert.message = " An error has occurred while processing the " + this.actionMode;
        },
        () => {
       //   this.logger.log("[" + this.actionMode + "]  from backend done");
          this.alert.message = this.actionMode + " Done.";
          // this.closeAlert();

        });
    //  this.logger.log("[EXPORT] after the subscribe finished! ");
    }
  }

  public uploadHandler(): Promise<any> {
    let result: any;

    this.fileUploadService.getObserver()
      .subscribe(progress => {
        this.uploadProgress = progress;
      });

    try {
      let extraParams = new Map<string, string>();
      extraParams.set("userId",this.auth.getUser().username);
      extraParams.set("time", String(Date.now()));
      return this.fileUploadService.upload(this.config.get("backUrl") + "/amhrouting/import", this.fileList, extraParams);
    } catch (error) {
      this.logger.error("An error has occurred while importing: " + error);
      document.write(error)
    }

  }


  private cancel() {

    this.router.parent.navigate(["AMHHome"]);
  }



}