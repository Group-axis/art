import {Component, ElementRef, EventEmitter, Output, Input, ViewChild, OnInit, OnChanges } from 'angular2/core';
import {SAARoutingService} from "../";
import {Observable} from 'rxjs/Observable';
import {DataTable, DataTableDirectives} from 'angular2-datatable/datatable';
import {Logger} from '../../common/components/services';

// console.log('`ElementSearch` component loaded asynchronously');

@Component({
  selector: 'element-search',
  template: require('./saa-point-search.html'),
  providers: [SAARoutingService],
  host: {
    '(document:click)': 'handleClick($event)',
  },
  directives: [DataTableDirectives]
})
export class ElementSearchComponent implements OnInit, OnChanges {
  // @ViewChild(DataTable) table;

  @Input("default-key") public defaultCode: string;
  @Output() public elementSelected: EventEmitter<any> = new EventEmitter();
  @Output() public elementNotValid : EventEmitter<any> = new EventEmitter();
  private elements: Array<any> = [];
  private originalElements: Array<any> = [];
  // private assignmentDataSource: Observable<AssignmentUnique>;
  private bodyMargin: number = 0;

  private selectedElement: any = {"pointName":""};
  private oldAssignmentType: string;

  constructor(private saaRoutingService: SAARoutingService, private myElement: ElementRef, private logger : Logger) {


  }

  ngOnChanges() {
    //  this.logger.log("this.selectedElement = {"pointName":this.defaultCode};" + this.assignmentType + "] old [" + this.oldAssignmentType + "] ");
    this.selectedElement = {"pointName":this.defaultCode};
  }

  ngOnInit() {
    this.logger.log('hello `Generic Filter` component');
    this.loadElements();

  }

  private loadElements() {
    this.logger.log("loadElements ");
    this.originalElements = [] ;
    this.elements = [] ;
    // Get the data from the server
    this.saaRoutingService.findPointNames().subscribe(
      data => {
        // let resp = SAARoutingService.getFromSource(data)
        // resp.forEach(assignment => {
        //   this.originalElements.push(assignment);
        // });
        this.originalElements.push(data);
      },
      err =>
        console.error("Can't get elements. Error code: %s, URL: %s ", err.status, err.url),
      () => {
        this.logger.log("Element(s) are retrieved");
        this.updateElements(this.defaultCode);
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
      this.elements = [];
      this.logger.debug("it was not inside!! with value "+this.defaultCode);
      this.elementSelected.emit({"pointName":this.defaultCode});

      if (!this.selectedElement["pointName"] ) {
        // this.selectedElement =  {"pointName":""};
      }
    }
  }

  private selectElement(element: any) {
    if (!element) {
      this.logger.log("element[" + element + "] missing ");
      return;
    }
    this.logger.log("selected element  " + JSON.stringify(element));
//(change)="actionUpdatePointErrorMsg($event.target.value)"
    this.defaultCode = element.pointName;
    this.elementSelected.emit(element);
    this.elements = [];
    this.selectedElement = {"pointName":this.defaultCode};
  }

 private updateElements(filterText) {
    this.defaultCode = filterText;
    if (!filterText) {
      this.elements = [];
      return;
    }

    let valueToFind = filterText.toUpperCase();

    this.elements = this.originalElements.filter((item:any) =>
      item["pointName"].toUpperCase().match(valueToFind));
  }

  

}
