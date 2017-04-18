import { ElementRef, Component, ViewChild } from 'angular2/core';
import { NgClass, Validators, CORE_DIRECTIVES, FORM_DIRECTIVES, FORM_PROVIDERS } from '@angular/common';
// import { ACCORDION_DIRECTIVES, BUTTON_DIRECTIVES } from 'ng2-bootstrap';
import { Router, RouteParams } from 'angular2/router';
import { SAARoutingService } from "../saa-routing.service";
import { Observable } from 'rxjs/Observable';
//import {FormFieldComponentExample, BootstrapFormDirective, BootstrapInputDirective, LabelsComponent, LabelsValueAccessor, notEmpty} from '../../common/components/ui/controls';
import { Alert } from '../../common/components/ui/widgets/modal';
import { MenuConfig } from '../../models/menu';
import { HeaderSecondary } from '../../header-secondary';
import { FileUploadService, FileDownloader, Config, Logger } from '../../common/components/services';
import { Permissions, NotPermissions } from '../../common/directives';

// console.log('`SAAExportImport` component loaded asynchronously');

@Component({
  selector: 'saa-export-import',
  styles: [`
    h1 {
      font-family: Arial, Helvetica, sans-serif
    }
  `],
  template: require('./saa-export-import.html'),
  //ACCORDION_DIRECTIVES, BUTTON_DIRECTIVES, FormFieldComponent, FormLabelComponent,
  //FormFieldComponentExample, BootstrapFormDirective, BootstrapInputDirective, LabelsComponent, LabelsValueAccessor,  
  directives: [CORE_DIRECTIVES, FORM_DIRECTIVES, NgClass, Alert, HeaderSecondary, Permissions, NotPermissions],
  providers: [FORM_PROVIDERS, SAARoutingService, FileUploadService, FileDownloader]
})
export class SAAExportImportComponent {
  @ViewChild(Alert) alert;

  private returnToString: string;
  private parameters = {};
  private actionMode: string = "Import";
  private isImport: boolean;
  private filePath: string;
  private uploadProgress: number = 0;
  private fileList: File[] = new Array<File>();
  private menuConfig: Array<MenuConfig>;

  constructor(private router: Router, private saaRoutingService: SAARoutingService,
    private fileUploadService: FileUploadService,
    private fileDownloader: FileDownloader,
    private config: Config,
    routeParams: RouteParams, private logger: Logger) {
    this.actionMode = routeParams.params['action'] || 'Import';
    this.isImport = this.actionMode == 'Import';
    this.menuConfig = [
      new MenuConfig("fa fa-home", "/home", "Home"),
      new MenuConfig("fa fa-sitemap", "/saa-routing", "SAA Routing"),
      new MenuConfig("fa fa-cloud-upload", "", this.actionMode)]
  }


  ngOnInit() {
    this.logger.log('hello `SAA Export Import` component');
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
    this.logger.log("file selected " + this.fileList);
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
    this.alert.message = "You will be notified when the " + this.actionMode + " finish.";
    this.alert.cancelButtonText = "Ok";
    this.alert.open();
  }

  confirmClose(data) {
    this.logger.log("confirming ------------  " + data);
    if (data == 0) {
      this.router.parent.navigate(["SAARoutingHome"]);
    }
  }

  closeAlert() {
    //setTimeout(() => this.alert.cancel(), 1000);
    //setTimeout(() => this.router.parent.navigate(["SAARoutingHome"]), 1200);
  }

  private disableExpImpButton(): boolean {
    if (this.isImport) {
      return this.fileList.length == 0;
    } else {
      return !this.filePath || this.filePath.length == 0;
    }
  }

  private exportImport() {
    this.alertOpen();
    this.logger.log("export import ..." + " isImport " + this.isImport + " selected file " + this.filePath);
    let send;
    if (this.isImport) {
      this.uploadHandler().then(data => {
        this.alert.message = this.actionMode + " Done.";
        this.closeAlert();
      }, error => {
        this.logger.log(" ERROR while importing " + error);
        this.alert.message = " An error has occurred while processing the " + this.actionMode + ".";
        this.closeAlert();
      });
      //   let fromIndex = this.filePath.lastIndexOf('\\');
      //  send = this.saaRoutingService.import("c:/demo/"+this.filePath.substr(fromIndex+1));
    } else {
      this.logger.log("[EXPORT] just before calling export post on server");
      send = this.saaRoutingService.export("UNKNOWN", "DEFAULT", this.filePath);
      send.subscribe(
        data => {
          this.logger.log("[" + this.actionMode + "] %s", data);
          this.fileDownloader.download(this.config.get("saaBackUrl") + "/routing/export/" + data.response, data.response);
        },
        err => {
          this.logger.log("[" + this.actionMode + "] Can't be done. Error code: %s, URL: %s ", err.status, err.url),
            this.alert.message = " An error has occurred while processing the " + this.actionMode;
          this.closeAlert();
        },
        () => {
          this.logger.log("[" + this.actionMode + "]  from backend done");
          this.alert.message = this.actionMode + " Done.";
          this.closeAlert();

        });

    }
  }

  public uploadHandler(): Promise<any> {
    let result: any;

    this.fileUploadService.getObserver()
      .subscribe(progress => {
        this.uploadProgress = progress;
      });

    try {
      return this.fileUploadService.upload(this.config.get("saaBackUrl") + "/routing/import", this.fileList);
    } catch (error) {
      this.logger.error("An error has occurred while importing: " + error);
      document.write(error)
    }

  }


  private cancel() {

    this.router.parent.navigate(["SAARoutingHome"]);
  }



}