import {Component, ViewChild} from '@angular/core';
import {DataTable, DataTableDirectives} from 'angular2-datatable/datatable';
import {AssignmentList, AssignType} from "../../models/routing-amh";
import {Observable} from 'rxjs/Observable';
import {Option} from '../../models/referential/option';
import {MenuConfig} from '../../models/menu';
import {HeaderSecondary} from '../../header-secondary';
import {AMHSelectionTableComponent} from '../amh-selection-table';
import { RouteParams} from 'angular2/router';
import {Auth, Logger} from '../../common/components/services';
import {AMHAssignmentService} from "../amh-service/amh-assignment.service";
import { Permissions, NotPermissions } from '../../common/directives';
import {Alert} from '../../common/components/ui/widgets/modal';

//this.logger.log('`AMH assignment list` component loaded asynchronously');

@Component({
  selector: 'amh-assignment-list',
  providers: [AMHAssignmentService, Auth ],
  styles: [`
    h1 {
      font-family: Arial, Helvetica, sans-serif
    }
  `],
  template: require('./amh-assignment-list.html'),
  directives: [DataTableDirectives, HeaderSecondary, AMHSelectionTableComponent, Alert, Permissions, NotPermissions]
})
export class AMHAssignmentList {
  @ViewChild("filterText") filterValue;
  @ViewChild(DataTable) table;
  @ViewChild(Alert) alert;
    
  private data: Array<AssignmentList> = [];
  private original: Array<AssignmentList> = [];
  private menuConfig : Array<MenuConfig> = [
    new MenuConfig("fa fa-home","/home","Home"),
    new MenuConfig("fa fa-sitemap","/amh-routing","AMH Routing"),
    new MenuConfig("fa fa-cloud-download","","Routing Overview")
    ];
  
  private selectionTables : Array<Option> = [
    new Option(AssignType.BK_CHANNEL,"BK_CH","Backend Channel"),
    new Option(AssignType.DTN_COPY,"DTN_CPY","Distribution Copy"),
    new Option(AssignType.FEED_DTN_COPY,"FEED_DTN_CPY","Feedback Distribution Copy")
  ];
  private defaultOption : Option;
  
  constructor(routeParams: RouteParams, private amhAssignmentService: AMHAssignmentService, private auth: Auth, private logger : Logger) { 
    //  this.logger.log(" from assigList "+this.config.get("esBackUrl"));
      this.defaultOption = this.selectedAssignmentType(+ routeParams.params['st']);
      this.loadAssignments(this.defaultOption);
  }

  private selectedAssignmentType(assignType : AssignType) : Option {
    let typeSelected = this.selectionTables.find((type) => { return type.id === assignType; });
    return typeSelected ? typeSelected : this.selectionTables[0]; 
  }

  ngOnInit(){
    //this.logger.log('hello `AMH assignment list` component');
  // Get the data from the server
  }

  
  private loadAssignments(option: Option) {
    if (!option ) {
      option = this.selectionTables[0];
    }

    this.original = [];
    this.amhAssignmentService.findAssignments(option.id).subscribe( 
      data => {
        let resp = AMHAssignmentService.getFromSource(data);
        resp.map(assign => {
         // this.logger.debug(" loading "+assign.code)
          this.original = this.original.concat(this.fromJsonToAssignList(option.id, assign));
        });

        this.logger.debug("final size "+ this.original.length);
        // this.data.forEach(value => this.logger.log(" assignList "+value.code+" rule "+ value.ruleExpressions));
        this.data = this.original;
      },
      err =>
        this.logger.error("Can't get assignments. Error code: %s, URL: %s ", err.status, err.url)
     // ,( ) => this.logger.log('Assignment(s) are retrieved')
    );

  }

  private fromJsonToAssignList(type : AssignType, assign) : Array<AssignmentList> {
    let list : Array<AssignmentList> = [];
    switch(type) {
      case AssignType.BK_CHANNEL:
        if (assign.rules.length > 0) {
            assign.rules.map( rule => {
                list.push(new AssignmentList(assign.active, assign.code, assign.backendPrimaryKey.code, assign.backendPrimaryKey.direction,
                "",assign.sequence, rule.code, rule.expression, rule.sequence, assign.environment, assign.version));
            });
            list = list.sort((assig1,assig2) => { 
                return (assig1.backSequence * 100 + assig1.ruleSequence) - (assig2.backSequence * 100 + assig2.ruleSequence); 
            });
        } else {
           //this.logger.debug("adding assign "+assign.code+" without rules");
           list.push(new AssignmentList(assign.active, assign.code, assign.backendPrimaryKey.code, assign.backendPrimaryKey.direction,
                "", assign.sequence, "", "", undefined, assign.environment, assign.version)); 
        }
      break;
      case AssignType.DTN_COPY:
      case AssignType.FEED_DTN_COPY:
      let isBackendLarger = assign.rules.length <= assign.backends.length; 
      let largestList = this.getList(isBackendLarger, assign, ["code","sequence"]);
      let smallest = this.getList(!isBackendLarger, assign, ["code","sequence"]);
      
      largestList.map( large => {
            let backend = this.getBackendInfo(isBackendLarger ? [large] : smallest.splice(0,1));
            let rule = this.getRuleInfo(isBackendLarger ? smallest.splice(0,1): [large]); 
            list.push(new AssignmentList(assign.active, assign.code, backend.code, backend.direction ,
            "",assign.sequence, rule.code, rule.expression, rule.sequence, assign.environment, assign.version));
          });
      break;
    }
   // this.logger.debug(" size of mapped list "+list.length);
    return list;
  } 
  
  private getList(isBackendLarger : boolean, assign, fieldNamesComp : Array<string>) {
    let list = isBackendLarger ? assign.backends : assign.rules;
    let fieldName = isBackendLarger ? fieldNamesComp[0] : fieldNamesComp[1];
    return this.sort(list, fieldName);
  }

  private sort(input : Array<any>, fieldToCompare : string) : Array<any> {
     return input.sort((a,b) => {
           if (a[fieldToCompare] < b[fieldToCompare])
            return -1;
          if (a[fieldToCompare] > b[fieldToCompare])
            return 1;
          return 0;
        });
  }

  private getBackendInfo(backends : Array<any>) : any {
    if ( backends.length == 0 ) {
        return {"code":"", "direction":""};
    }
    return {"code":backends[0].code, "direction":backends[0].direction};
  }

  private getRuleInfo(rules : Array<any>) : any {
    if ( rules.length == 0 ) {
        return { "code":"", "expression":"", "sequence":undefined };
    }
    return { "code":rules[0].code, "expression":rules[0].expression, "sequence":rules[0].sequence };
  }
  
  private updateSelectionTable(option) {
    //let codeToFind = this.defaultOption ? this.defaultOption.code : option.code; 
    //this.optionRollback = this.selectionTables.find((innerOption) => { return innerOption.code === codeToFind; });
    this.defaultOption = option;
    this.loadAssignments(this.defaultOption);
    this.filterValue.nativeElement.value = "";
    let pageEvent = this.table.getPage();
    //return pageEvent = {activePage: this.activePage, rowsOnPage: this.rowsOnPage, dataLength: this.inputData.length};
    this.table.setPage(1, pageEvent.rowsOnPage);
    //this.assignmentConfig = new AssignmentConfig(option.id);
    //this.logger.log("selection table updated ------------  " + option.description);
  }

   updateData( filterText) {
     this.data = this.changeFilter(this.original, {filtering:{filterString:filterText, columnName:"ruleExpressions"}});
   }
   
   private changeFilter(data:any, config:any):any {
    if (!config.filtering) {
      return data;
    }

    let valueToFind = config.filtering.filterString.toUpperCase();
    let filteredRuleExpresssion:Array<any> = data.filter((item:any) =>
      item[config.filtering.columnName].toUpperCase().match(valueToFind));
     
    let filteredCode:Array<any> = data.filter((item:any) =>
      item["code"].toUpperCase().match(valueToFind));
    
    let filteredBackendCode:Array<any> = data.filter((item:any) =>
      item["backCode"].toUpperCase().match(valueToFind));
      
    let filteredRuleCode:Array<any> = data.filter((item:any) =>
      item["ruleCode"].toUpperCase().match(valueToFind));
      
    let filteredData =  filteredRuleExpresssion.concat(filteredCode).concat(filteredBackendCode).concat(filteredRuleCode);
      
    let uniqueList : Array<any>= [];
    filteredData.forEach( item => {
        let found = uniqueList.find((value, index, array) =>  {
              return item["code"] == value["code"] && item["backCode"] == value["backCode"] && item["ruleCode"] == value["ruleCode"];
            });
        if (!found) {
          uniqueList.push(item);
        }
    });

    return uniqueList;
  }
  
  actionExportCSVFile() {
    
    if (this.data.length == 0) {
      //this.logger.log(" there is no assignments in the overview to export ");
      return;
    }
    //this.logger.log(" exporting assignment overview ");
    this.alertOpen();
    this.amhAssignmentService
      .exportOverview(this.defaultOption.id, this.auth.getUser())
      .subscribe(
      data => {
        //this.logger.log("[EXPORT_CSV] %s", data);
        this.amhAssignmentService.downloadFile("/amhrouting/csv/export/assignments/" + data.fileName, data.fileName)
      //  this.fileDownloader.download(this.config.get("backUrl") + "/amhrouting/csv/export/" + data.fileName, data.fileName);
      },
      err => {
        this.logger.error("[EXPORT_CSV] Can't be done. Error code: %s, URL: %s ", err.status, err.url),
          this.alert.message = " An error has occurred while downloading the asynchronous execution result";
        this.closeAlert();
      },
      () => {
      //  this.logger.log("[EXPORT_CSV]  from backend done");
        this.alert.message = "Download Done.";
        this.closeAlert();

      });
  }

  private closeAlert() {
    setTimeout(() => this.alert.cancel(), 1000);
  }

   private alertOpen() {
    this.alert.alertFooter = false;
    this.alert.cancelButton = false;
    this.alert.okButton = false;
    this.alert.alertHeader = true;
    this.alert.alertTitle = "Download in progress ";
    this.alert.message = "This alert will be close when the download finish.";
    this.alert.cancelButtonText = "Ok";
    this.alert.open();
  } 

 alertResponse(resp) {
    switch (resp) {
      case 0: //Delete message cancel
        // this.saveStatus = "Cancel";
        break;
      case 1: //Delete message Yes
        break;
      case 100: //Send an email 'Yes' response
        break;
      case 101: //Send an email 'No' response.
        break;
    }
  }
  // private findAssignment(assignment) {
     
  // }


  // private existsInList(item:any, list : Array<any>, fieldName : string) : boolean {
  //   list.forEach( e => {
  //     if(item[fieldName] === e[fieldName]) {
  //       return true;
  //     }
  //   });
  //   return false;
  // }
 

}
