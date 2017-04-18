import {Component, ElementRef, EventEmitter, Output, Input, ViewChild, OnInit, OnChanges, SimpleChange } from 'angular2/core';
import {AMHAssignmentService} from "../amh-service";
import {Observable} from 'rxjs/Observable';
import {DataTable, DataTableDirectives} from 'angular2-datatable/datatable';
import {AssignmentUnique, AssignType} from "../../models/routing-amh";
import {Logger} from '../../common/components/services';

//this.logger.log('`AMHAssignmentSearch` component loaded asynchronously');

@Component({
  selector: 'amh-assignment-search',
  template: require('./amh-assignment-search.html'),
  providers: [AMHAssignmentService],
  host: {
    '(document:click)': 'handleClick($event)',
  },
  directives: [DataTableDirectives]
})
export class AMHAssignmentSearchComponent implements OnInit, OnChanges {
  @ViewChild(DataTable) table;

  @Input("default-code") public defaultCode: string;
  @Input("assignment-type") public assignmentType: AssignType = AssignType.BK_CHANNEL;
  @Output() public assignmentSelected: EventEmitter<any> = new EventEmitter();

  private assignments: Array<AssignmentUnique> = [];
  private originalAssignments: Array<AssignmentUnique> = [];
  // private assignmentDataSource: Observable<AssignmentUnique>;
  private bodyMargin: number = 0;

  private selectedAssignment: AssignmentUnique = new AssignmentUnique();
  private selectedPkDirection: string = "";
  private oldAssignmentType: string;

  constructor(private amhAssignmentService: AMHAssignmentService, private myElement: ElementRef, private logger : Logger) {

  }

  ngOnChanges(changes: { [propertyName: string]: SimpleChange }) {
    this.logger.debug("changes : "+ JSON.stringify(changes));
    if (this.oldAssignmentType != this.typeAsString(this.assignmentType)) {
       this.defaultCode = changes["defaultCode"] ?changes["defaultCode"].currentValue : undefined; 
       this.loadAssignments(changes["defaultCode"] != undefined);
       this.oldAssignmentType = this.typeAsString(this.assignmentType);
       this.selectedAssignment =  new AssignmentUnique();
    } else
    if (changes["defaultCode"]) {
      //this.logger.log("defaultCode has been changed to "+this.defaultCode);
      this.updateAssignments(this.defaultCode);
      if (this.assignments.length == 1) {
         this.selectAssignment(this.assignments[0]);
      }
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

  private loadAssignments(loadDefaultSelection? : boolean) {
    //this.logger.log("loadAssignments  AssignmentFilter [" + this.assignmentType + "]");
    this.originalAssignments = [] ;
    this.assignments = [] ;
    // Get the data from the server
    this.amhAssignmentService.findAssignments(this.assignmentType).subscribe(
      data => {
        let resp = AMHAssignmentService.getFromSource(data)
   //     this.logger.log("Assignment " + resp + " received");
        resp.forEach(assignment => {
          this.originalAssignments.push(assignment);
          //this.assignments.push(assignment);
        });
      },
      err =>
        this.logger.error("Can't get assignments type " + this.assignmentType + ". Error code: %s, URL: %s ", err.status, err.url),
      () => {
      //  this.logger.log(this.originalAssignments.length +" Assignment(s) of type '" + this.assignmentType + "' are retrieved.");
        this.updateAssignments(this.defaultCode);
         if (loadDefaultSelection && this.assignments.length == 1) {
            this.selectAssignment(this.assignments[0]);
         }
      }
    );
  }

handleClick(event) {
    var clickedComponent = event.target;
    var inside = false;
    do {
      // this.logger.log(clickedComponent + " equals " + (clickedComponent === this.elementRef.nativeElement) );
      if (clickedComponent === this.myElement.nativeElement) {
        inside = true;
      }
      clickedComponent = clickedComponent.parentNode;
    } while (clickedComponent);

    if (!inside) {
      this.assignments = [];
      if (!this.selectedAssignment.code ) {
        this.selectedAssignment =  new AssignmentUnique();
      }
    }
  }

  // select(backend: AssignmentUnique) {
  //   this.selectedAssignment = new AssignmentUnique(backend.pkCode, backend.pkDirection, backend.code, backend.dataOwner, backend.description);
  //   this.selectedPkDirection = backend.pkDirection
  //   this.backends = [];
  // }

  private selectAssignment(assignment: AssignmentUnique) {
    //this.logger.log("selected assignment code " + assignment.code);
    if (!assignment) {
      this.logger.error("assignment[" + assignment + "] missing ");
      return;
    }

    this.assignmentSelected.emit(assignment.code);
    this.assignments = [];
    this.selectedAssignment = new AssignmentUnique();
  }

 private updateAssignments(filterText) {
    if (!filterText) {
      this.assignments = [];
      return;
    }

    let assignedAssignmentCodeMap = {};
 
    let originalAssignmentsMinusAssignedAssignments = this.originalAssignments.filter(assignment => {
      return !assignedAssignmentCodeMap[assignment.code];
    });

    let valueToFind = filterText.toUpperCase();

    // let filteredAssignmentSequence:Array<any> = originalAssignmentsMinusAssignedAssignments.filter((item:any) =>
    //   item["sequence"].match(valueToFind));

    let filteredAssignmentCode:Array<any> = originalAssignmentsMinusAssignedAssignments.filter((item:any) =>
      item["code"].toUpperCase().match(valueToFind));
    
    // let filteredData =  filteredAssignmentSequence.concat(filteredAssignmentCode);
    let filteredData =  filteredAssignmentCode;

    let uniqueList : Array<any>= [];
    filteredData.forEach( item => {
        let found = uniqueList.find((value, index, array) => { return item["code"] == value["code"]; });
        if (!found) {
          uniqueList.push(item);
        }
    });
//.toUpperCase()
    this.assignments = uniqueList.map( assignment => { assignment.code = assignment.code; return assignment; });
    
  }

  


}
