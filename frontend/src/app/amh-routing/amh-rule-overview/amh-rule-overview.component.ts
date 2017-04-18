import {Component, ViewChild} from '@angular/core';
import {NgFormControl, Control} from '@angular/common';
import {DataTable, DataTableDirectives} from 'angular2-datatable/datatable';
import {AMHRuleService} from "../amh-service";
import {AMHRule, RuleAssignType} from "../../models/routing-amh";
import {Observable} from 'rxjs/Observable';
import {Option} from '../../models/referential/option';
import {MenuConfig} from '../../models/menu';
import {HeaderSecondary} from '../../header-secondary';
import {AMHSelectionTableComponent} from '../amh-selection-table';
import { RouteParams} from 'angular2/router';
import {Alert} from '../../common/components/ui/widgets/modal';
import {Auth, Logger} from '../../common/components/services';
import { Permissions } from '../../common/directives';
import 'rxjs/add/operator/debounceTime';
import 'rxjs/add/operator/switchMap';

//this.logger.log('`AMH rule overview` component loaded asynchronously');

@Component({
  selector: 'amh-rule-overview',
  providers: [AMHRuleService, Auth],
  styles: [`
    h1 {
      font-family: Arial, Helvetica, sans-serif
    }
  `],
  template: require('./amh-rule-overview.html'),
  directives: [Permissions, Alert, NgFormControl, DataTableDirectives, HeaderSecondary, AMHSelectionTableComponent]
})
export class AMHRuleOverviewComponent {
  @ViewChild(Alert) alert;
  @ViewChild("filterText") filterValue;
  @ViewChild(DataTable) table;

  private data: Array<AMHRule> = [];
  private menuConfig: Array<MenuConfig> = [
    new MenuConfig("fa fa-home", "/home", "Home"),
    new MenuConfig("fa fa-sitemap", "/amh-routing", "AMH Routing"),
    new MenuConfig("fa fa-cogs", "", "Rule Overview")
  ];

  private selectionTables: Array<Option> = [
    new Option(RuleAssignType.ALL, "ALL", "All Rules"),
    new Option(RuleAssignType.ASSIGNED, "ASSIGNED", "Assigned Rules"),
    new Option(RuleAssignType.UNASSIGNED, "UNASSIGNED", "Unassigned Rules")
  ];
  private defaultOption: Option;
  private ruleTextInput: Control;
  private ruleTextFilter: string;
  private ruleCodeToDelete: string;

  constructor(routeParams: RouteParams, private amhRuleService: AMHRuleService, private auth: Auth, private logger : Logger) {

    this.defaultOption = this.selectedRuleType(+ routeParams.params['at']);
    this.loadRules(this.defaultOption);

    this.ruleTextFilter = routeParams.params['filter'] || "";
    this.ruleTextInput = new Control("");
    let tempText = "";
    this.ruleTextInput.valueChanges
      .debounceTime(50)
      .switchMap(filterText => {
        tempText = filterText;
        return this.amhRuleService.findRuleMatches(this.defaultOption.id, filterText);
      })
      .subscribe(
      data => {
        this.data = this.changeFilter(data, { filtering: { filterString: tempText, columnName: "code" } });
        this.logger.debug("response from findRuleMatches with text " + tempText + " size " + this.data.length);
      },
      err =>
        this.logger.log("Can't get rules. Error code: %s, URL: %s ", err.status, err.url),
      () => {
        // this.data = temp;
        this.logger.debug(this.data.length + ' rule(s) are retrieved from ES with text ' + tempText);
      }
      );
  }

  private selectedRuleType(ruleType: RuleAssignType): Option {
    let typeSelected = this.selectionTables.find((type) => { return type.id === ruleType; });
    return typeSelected ? typeSelected : this.selectionTables[0];
  }

  ngOnInit() {
    this.logger.log('hello `AMH Rule Overview` component');
    // Get the data from the server
  }


  private loadRules(option: Option) {
    if (!option) {
      option = this.selectionTables[0];
    }

    //this.original = [];
    this.amhRuleService.findRulesByAssignType(option.id).subscribe(
      rules => {
        //  this.original = rules;
        this.data = rules;
        this.logger.debug("final size " + this.data.length);
        // this.data.forEach(value => this.logger.log(" assignList "+value.code+" rule "+ value.ruleExpressions));
      },
      err =>
        this.logger.log("Can't get rules. Error code: %s, URL: %s ", err.status, err.url),
      () => this.logger.log('Rule(s) are retrieved')
    );
  }


  private updateSelectionTable(option) {
    //let codeToFind = this.defaultOption ? this.defaultOption.code : option.code; 
    //this.optionRollback = this.selectionTables.find((innerOption) => { return innerOption.code === codeToFind; });
    this.defaultOption = option;
    this.loadRules(this.defaultOption);
    // this.filterValue.nativeElement.value = "";
    this.ruleTextFilter = "";
    let pageEvent = this.table.getPage();
    //return pageEvent = {activePage: this.activePage, rowsOnPage: this.rowsOnPage, dataLength: this.inputData.length};
    this.table.setPage(1, pageEvent.rowsOnPage);
    //this.assignmentConfig = new AssignmentConfig(option.id);
    this.logger.log("selection table updated ------------  " + option.description);
  }

  private changeFilter(data: any, config: any): any {
    if (!config.filtering) {
      return data;
    }

    let valueToFind = config.filtering.filterString.toUpperCase();
    let filteredRuleExpresssion: Array<any> = data.filter((item: any) =>
      item[config.filtering.columnName].toUpperCase().match(valueToFind));

    let filteredExpression: Array<any> = data.filter((item: any) =>
      item["expression"].toUpperCase().match(valueToFind));


    let filteredData = filteredRuleExpresssion.concat(filteredExpression);

    let uniqueList: Array<any> = [];
    filteredData.forEach(item => {
      let found = uniqueList.find((value, index, array) => {
        return item["code"] == value["code"];
      });
      if (!found) {
        uniqueList.push(item);
      }
    });

    return uniqueList;
  }

  actionAlertResponse(response: number) {
    switch (response) {
      case 0: //Delete Rule cancel
        // this.saveStatus = "Cancel";
        break;
      case 1: //Delete Rule Yes
        this.deleteRule();
        break;

    }
  }

  private alertYesCancel(yesResponse: number, cancelResponse: number, message: string, yesLabel?: string) {
    this.alert.waitIcon = false;
    this.alert.alertFooter = true;
    this.alert.cancelButton = true;
    this.alert.cancelButtonText = "Cancel";
    this.alert.cancelButtonResponse = cancelResponse;
    this.alert.yesButton = true;
    this.alert.yesButtonText = yesLabel || "Yes";
    this.alert.yesButtonResponse = yesResponse;
    this.alert.okButton = false;
    this.alert.alertHeader = true;
    this.alert.alertTitle = " Alert ";
    this.alert.message = message;
    this.alert.open();
  }

  private deleteAlertMessage = `
     The rule '$ruleCode' is assigned to the following list:<br\>
       * Backend Selection Table:<br\>
         ##assignmentCodes## <br\>
       * Feedback Distribution Copy Selection Table:<br>
         $feedbackCodes<br>
       * Distribution Copy Selection Table:<br>
         $distributionCode<br>
  `;

  actionDeleteRule(rule: AMHRule) {
    this.logger.debug("action delete rule " + rule.code);
    this.ruleCodeToDelete = rule.code;
    /*
hit: {"type":"assignments","code":"AEMM6","sequence":10}
hit: {"type":"feedbackDtnCopies","code":"FDI08","sequence":10}
hit: {"type":"distributionCopies","code":"BNP_Distribute_Autocancel_TOBEX","sequence":10}
 */
    let message = undefined;
    this.amhRuleService.findAssignmmentsByRuleCode(rule.code)
      .subscribe(
      founds => {
        //founds.forEach(e => this.logger.log("hit: "+JSON.stringify(e)))
        message = founds.map(hit => hit.type + " -> ('" + hit.code + "'," + hit.sequence + ")\n")
          .reduce((acc, e) => { return acc + e }, "")
      },
      error => this.logger.error("On delete rule: " + error.message),
      () => {
        let alertMessage = "This action will unassign and delete the rule " + rule.code + ". Are you sure you want to delete it? ";
        if (message) {
          // alertMessage = this.deleteAlertMessage.replace(/##assignmentCodes##/g, message) + "   " + alertMessage;
          alertMessage = alertMessage;
        }
        this.alertYesCancel(1, 0, alertMessage);
      }
      )
  }

  private deleteRule() {
    this.logger.log("deleting rule code " + this.ruleCodeToDelete);
    this.amhRuleService.deleteRule(this.ruleCodeToDelete, this.auth.getUser())
      .subscribe(
      ok => this.logger.debug("rule deleted"),
      error => this.logger.error("while deleting rule " + error.message),
      () => {
        this.logger.info("delete done successfully");
        setTimeout(() => this.loadRules(this.defaultOption), 1000);
        ;
      }
      );
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

  actionExportRuleOverview() {
    if (this.data.length == 0) {
      this.logger.log(" there is no rules in the overview to export ");
      return;
    }

    this.logger.log(" exporting rules overview ");
    
    this.alertOpen();
    
    this.amhRuleService.exportRuleOverview(this.defaultOption.id, this.auth.getUser())
      .subscribe(
      data => {
        this.logger.log("[EXPORT_CSV] %s", data);
        this.amhRuleService.downloadFile("/amhrouting/csv/export/rules/" + data.fileName, data.fileName)
      },
      err => {
        this.logger.log("[EXPORT_CSV] Can't be done. Error code: %s, URL: %s ", err.status, err.url),
          this.alert.message = " An error has occurred while downloading the asynchronous execution result";
        this.closeAlert();
      },
      () => {
        this.logger.log("[EXPORT_CSV]  from backend done");
        this.alert.message = "Download Done.";
        this.closeAlert();

      });
  }

}
