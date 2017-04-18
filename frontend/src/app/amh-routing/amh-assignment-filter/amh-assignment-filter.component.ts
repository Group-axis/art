import {Component, ElementRef, EventEmitter, Output, Input, ViewChild, OnInit, SimpleChange} from 'angular2/core';
import {AMHAssignmentService} from "../amh-service";
import {Observable} from 'rxjs/Observable';
import {DataTable, DataTableDirectives} from 'angular2-datatable/datatable';
import {AssignmentUnique, AssignType} from "../../models/routing-amh";
import {Logger} from '../../common/components/services';

//this.logger.log('`AMHAssignmentFilter` component loaded asynchronously');

@Component({
  selector: 'amh-assignment-filter',
  template: require('./amh-assignment-filter.html'),
  providers: [AMHAssignmentService],
  directives: [DataTableDirectives]
})
export class AMHAssignmentFilterComponent implements OnInit {
  @ViewChild(DataTable) table;

  @Input("default-code") public defaultCode: string;
  @Input("assignment-type") public assignmentType: AssignType = AssignType.BK_CHANNEL;
  @Output() public assignmentSelected: EventEmitter<any> = new EventEmitter();

  private assignments: Array<AssignmentUnique> = [];
  private originalBackends: Array<AssignmentUnique> = [];
  // private assignmentDataSource: Observable<AssignmentUnique>;
  private bodyMargin: number = 0;

  private selectedBackend: AssignmentUnique = new AssignmentUnique();
  private selectedPkDirection: string = "";
  private backendCodeToDelete: string = undefined;
  private activePage: number = 1;
  private disabled: boolean = true;
  private oldAssignmentType: string;

  constructor(private amhAssignmentService: AMHAssignmentService, private logger : Logger) {

  }

  ngAfterContentChecked() {
    //this.logger.log("filter::ngAfterContentChecked assignmentType [" + this.assignmentType + "] old [" + this.oldAssignmentType + "] ");
    if (this.oldAssignmentType != this.typeAsString(this.assignmentType)) {
      this.loadAssignments();
      this.oldAssignmentType = this.typeAsString(this.assignmentType);
    }
  }

  ngOnInit() {
    //this.logger.log('hello `AMH Assignment Filter` component');
    this.loadAssignments();
    this.oldAssignmentType = this.typeAsString(this.assignmentType);
  }

  private typeAsString(type: AssignType) : string {
    return  AssignType[type];
  }

  private loadAssignments() {
    //this.logger.log("loadAssignments  AssignmentFilter [" + this.assignmentType + "]");
    this.originalBackends = [] ;
    this.assignments = [] ;
    // Get the data from the server
    this.amhAssignmentService.findAssignments(this.assignmentType).subscribe(
      data => {
        let resp = AMHAssignmentService.getFromSource(data)
        //this.logger.log("Assignment " + resp + " received");
        resp.forEach(backend => {
          this.originalBackends.push(backend);
          this.assignments.push(backend);
        });
      },
      err =>
        this.logger.error("Can't get assignments type " + this.assignmentType + ". Error code: %s, URL: %s ", err.status, err.url),
      () => {
        this.logger.log("Assignment(s) " + this.assignmentType + " are retrieved");
        this.updateAssignments(this.defaultCode);
      }
    );
  }

  private selectAssignment(code: string) {
    //this.logger.log("selected assignment code " + code);
    if (!code) {
      //this.logger.log("code[" + code + "] missing ");
      return;
    }

    this.assignmentSelected.emit(code);
  }

  private updateAssignments(filterText: string) {
    this.assignments = this.changeFilter(this.originalBackends, { filtering: { filterString: filterText, columnName: "code" } });
  }

  private changeFilter(data: any, config: any): any {
    //this.logger.log("empty filter  " + config.filtering);
    if (!config.filtering || !config.filtering.filterString) {
      return data;
    }

    let filteredData: Array<any> = data.filter((item: any) => {
      //this.logger.log(" item " + item[config.filtering.columnName].toUpperCase() + " <==> " + config.filtering.filterString.toUpperCase() +
      //  " match " + item[config.filtering.columnName].toUpperCase().match(config.filtering.filterString.toUpperCase()));
      return item[config.filtering.columnName].toUpperCase().match(config.filtering.filterString.toUpperCase())
    });
    //this.logger.log(" filteredData " + filteredData);
    return filteredData;
  }


}
