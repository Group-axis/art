import {Component, ElementRef, EventEmitter, Output, Input, ViewChild, OnInit, OnChanges, SimpleChange} from 'angular2/core';
import {AMHAssignmentService} from "../amh-service";
import {Observable} from 'rxjs/Observable';
import {DataTable, DataTableDirectives} from 'angular2-datatable/datatable';
import {Backend} from "../../models/routing-amh";
import {Alert} from '../../common/components/ui/widgets/modal';
import {Logger} from '../../common/components/services';


//this.logger.log('`AMHBackendSelection` component loaded asynchronously');

@Component({
  selector: 'amh-backend-selection',
  template: require('./amh-backend-selection.html'),
  host: {
    '(document:click)': 'handleClick($event)',
  },
  providers: [AMHAssignmentService],
  directives: [Alert, DataTableDirectives]
})
export class AMHBackendSelectionComponent implements OnInit, OnChanges {
  @ViewChild(Alert) alert;
  @ViewChild(DataTable) table;

  @Input("existing-backends") public existingBackends: Array<any>;
  @Input("max-backends") public maxBackends: number = 1000;
  @Input("disabled") public disabledParam: boolean = false;
  @Output() public backendAdded: EventEmitter<any> = new EventEmitter();
  @Output() public backendDeleted: EventEmitter<any> = new EventEmitter();

  private backends: Array<Backend> = [];
  private originalBackends: Array<Backend> = [];
  private backendDataSource: Observable<Backend>;
  private bodyMargin: number = 0;

  private selectedBackend: Backend = new Backend("", "");
  private selectedPkDirection: string = "";
  private backendCodeToDelete: string = undefined;
  private activePage: number = 1;
  private disabled: boolean = true;

  constructor(private amhAssignmentService: AMHAssignmentService, private elementRef: ElementRef, private logger : Logger) {
    this.backendDataSource = this.amhAssignmentService.findBackends();
  }

  ngOnChanges(changes: { [propertyName: string]: SimpleChange }) {
    //this.logger.log(" updating backends " + changes);
    if (changes["existingBackends"]) {
      //this.logger.log("existingBackends has been changed,so update");
      this.dynamicUpdate();
    }
  }

  ngOnInit() {
    //this.logger.log('hello `AMH backend selection` component');
    // Get the data from the server
    this.backendDataSource.subscribe(
      data => {
          let resp = AMHAssignmentService.getFromSource(data)
          resp.forEach(backend => {
            this.originalBackends.push(backend);
          });
      },
      err =>
        this.logger.error("Can't get backends. Error code: %s, URL: %s ", err.status, err.url)
       // ,() => this.logger.log('Backend(s) are retrieved')
    );
  }

  handleClick(event) {
    var clickedComponent = event.target;
    var inside = false;

    do {
      // this.logger.log(clickedComponent + " equals " + (clickedComponent === this.elementRef.nativeElement) );
      if (clickedComponent === this.elementRef.nativeElement) {
        inside = true;
      }
      clickedComponent = clickedComponent.parentNode;
    } while (clickedComponent);

    if (!inside) {
      this.backends = [];
      if (!this.selectedBackend.code || !this.selectedBackend.pkDirection) {
        this.selectedBackend = new Backend("", "");
      }
    }
  }

  select(backend: Backend) {
    this.selectedBackend = new Backend(backend.pkCode, backend.pkDirection, backend.code, backend.dataOwner, backend.description);
    this.selectedPkDirection = backend.pkDirection
    this.backends = [];
  }

  private dynamicUpdate() {
    this.calculeInputDisabled();
    this.calculeBodyMargin();
  }

  private calculeInputDisabled() {
    // this.logger.log("this.disabledParam " + this.disabledParam + " this.existingBackends.length  " + this.existingBackends.length +" this.maxBackends "+ this.maxBackends);
    this.disabled = this.disabledParam || this.existingBackends.length >= this.maxBackends;
  }

  private calculeBodyMargin() {
    if (this.existingBackends.length >= 4) {
      this.bodyMargin = 0;
      return;
    }

    if (this.existingBackends.length == 0) {
      this.bodyMargin = 67;
      return;
    }

    if (this.existingBackends.length == 1) {
      this.bodyMargin = 39;
      return;
    }

    if (this.existingBackends.length == 2) {
      this.bodyMargin = 22;
      return;
    }

    if (this.existingBackends.length == 3) {
      this.bodyMargin = 4;
      return;
    }

  }

  private add() {
    if (!this.selectedBackend.pkCode || !this.selectedBackend.pkDirection) {
      //this.logger.log("no backend selected");
      return;
    } 
    // else {
    //   this.logger.log("selected backend " + this.selectedBackend.pkCode);
    // }

    let addedBackend = new Backend(this.selectedBackend.pkCode, this.selectedBackend.pkDirection, this.selectedBackend.code, this.selectedBackend.dataOwner, this.selectedBackend.description, this.selectedBackend.lockCode);
    this.backendAdded.emit(addedBackend);
    this.existingBackends = [addedBackend, ...this.existingBackends];
    this.selectedBackend = new Backend("", "");
    this.selectedPkDirection = "";

   // this.logger.log(" adding " + addedBackend.pkCode);
    this.dynamicUpdate();
    //this.existingBackends.forEach(ee => this.logger.log(" code " + ee.pkCode + " dir " + ee.pkDirection));
  }

  private deleteBackend(backend: Backend) {
    this.confirmBackendDeletion();
    this.backendCodeToDelete = backend.pkCode;
  }

  confirmBackendDeletion() {
    this.alert.waitIcon = false;
    this.alert.alertFooter = true;
    this.alert.cancelButton = false;
    this.alert.yesButton = true;
    this.alert.yesButtonText = "Yes";
    this.alert.yesButtonResponse = 21;
    this.alert.okButton = true;
    this.alert.okButtonText = "No";
    this.alert.okButtonResponse = 22;
    this.alert.alertHeader = true;
    this.alert.alertTitle = " Alert ";
    this.alert.message = "Are you sure you want to unassign this backend?";
    this.alert.open();
  }

  confirmClose(data) {
   // this.logger.log("confirming backend deletion ------------  " + data);
    if (data === 0) {
      return;
    }

    if (data == 21) {
    //  this.logger.log("deleting... " + this.backendCodeToDelete);
      this.existingBackends = this.existingBackends.filter((item: any) =>
        item["pkCode"] !== this.backendCodeToDelete);
      this.backendDeleted.emit(this.backendCodeToDelete);
      this.backendCodeToDelete = "";
      this.table.setPage(1, 3);
      this.dynamicUpdate();
      // this.logger.log(" on Page "+this.table.getPage().activePage);
    }

    if (data == 22) {
    //  this.logger.log("no deletion for backend " + this.backendCodeToDelete);
      this.backendCodeToDelete = "";
    }
  }

  private updateBackends(filterText) {
    if (!filterText) {
      this.backends = [];
      return;
    }

    let assignedBackendCodeMap = {};

    this.selectedPkDirection = "";
    this.selectedBackend.pkDirection = "";
    this.existingBackends.forEach(backend => assignedBackendCodeMap[backend.pkCode] = true);

    let originalBackendsMinusAssignedBackends = this.originalBackends.filter(backend => {
      return !assignedBackendCodeMap[backend.pkCode];
    });

    let valueToFind = filterText.toUpperCase();

    let filteredBackendExpresssion: Array<any> = originalBackendsMinusAssignedBackends.filter((item: any) =>
      item["pkCode"].toUpperCase().match(valueToFind));

    let filteredBackendCode: Array<any> = originalBackendsMinusAssignedBackends.filter((item: any) =>
      item["pkDirection"].toUpperCase().match(valueToFind));

    let filteredData = filteredBackendExpresssion.concat(filteredBackendCode);

    let uniqueList: Array<any> = [];
    filteredData.forEach(item => {
      let found = uniqueList.find((value, index, array) => { return item["pkCode"] == value["pkCode"]; });
      if (!found) {
        uniqueList.push(item);
      }
    });

    this.backends = uniqueList.map(backend => { backend.pkCode = backend.pkCode.toUpperCase(); return backend; });
  }
}
