import {Component, ElementRef, ViewChild, EventEmitter, Output, Input, OnInit, OnChanges, SimpleChange } from 'angular2/core';
import {AMHRoutingService} from "../amh-routing.service";
import {Observable} from 'rxjs/Observable';
import {AMHRule, AssignmentUniqueRule, AssignType} from "../../models/routing-amh";
import {DataTable, DataTableDirectives} from 'angular2-datatable/datatable';
import {Alert} from '../../common/components/ui/widgets/modal';
import {Logger} from '../../common/components/services';

//this.logger.log('`AMHRuleSelection` component loaded asynchronously');

@Component({
  selector: 'amh-rule-selection',
  template: require('./amh-rule-selection.html'),
  host: {
    '(document:click)': 'handleClick($event)',
  },
  providers: [AMHRoutingService],
  directives: [DataTableDirectives, Alert]
})
export class AMHRuleSelectionComponent implements OnInit, OnChanges {
  @ViewChild(DataTable) table;
  @Input("assignment-type") public assignType : AssignType = AssignType.BK_CHANNEL;
  @Input("assignment-code") public assignmentCode : string = "";
  @Input("disabled") public disabled : boolean = false;
  @Input("existing-rules") public existingRules : Array<AssignmentUniqueRule> = [];
  @Output() public ruleAdded:EventEmitter<any> = new EventEmitter();
  @Output() public ruleDeleted:EventEmitter<any> = new EventEmitter();
  @Output() public statusChanged:EventEmitter<any> = new EventEmitter();
  @Output() public ruleNavigate:EventEmitter<any> = new EventEmitter();
  @ViewChild(Alert) alert;

  private rules: Array<AMHRule> = [];
  private originalRules: Array<AMHRule> = [];
  private ruleDataSource: Observable<AMHRule>;
  private selectedRule: AMHRule = new AMHRule("", "");
  private selectedExpression: string = "";
  private ruleCodeToDelete : string = "";
  private ruleErrors: Map<string, string[]> = new Map<string, string[]>();

  constructor( private amhRoutingService: AMHRoutingService, private elementRef: ElementRef, private logger : Logger) {
    this.ruleDataSource = this.amhRoutingService.findRules();
  }

  ngOnChanges(changes: { [propertyName: string]: SimpleChange }) {
    if (changes["disabled"]) {
        this.logger.debug("disabled changed "+ changes["disabled"].currentValue);      
    }
  }
  ngOnInit() {
    this.logger.log('hello `AMH rule selection` component');
    // Get the data from the server
    this.ruleDataSource.subscribe(
      data => {
        if (Array.isArray(data)) {
          this.logger.log("Array " + data);
          //  this.data=data;
        } else {
          let resp = AMHRoutingService.getFromSource(data)
          resp.forEach(rule => {
            if (rule.code) {
              //this.logger.debug("in loding "+ rule.code);
            this.originalRules.push(AMHRule.fromJson(rule));
            } else {
              this.logger.warn("no code defined in rule  ");
            }
          });
        }
      },
      err =>
        this.logger.log("Can't get rules. Error code: %s, URL: %s ", err.status, err.url),
      () => this.logger.log('Rule(s) are retrieved')
    );

  }

  handleClick(event) {
    var clickedComponent = event.target;
    var inside = false;
    do {
      if (clickedComponent === this.elementRef.nativeElement) {
        inside = true;
      }
      clickedComponent = clickedComponent.parentNode;
    } while (clickedComponent);

    if (!inside) {
      this.rules = [];
      if (!this.selectedRule.code || !this.selectedRule.expression) {
        this.selectedRule = new AMHRule("", "");
      }
    }
  }

  select(rule: AMHRule) {
    let found = this.originalRules.find(oRule => { return oRule.code.toUpperCase() == rule.code.toUpperCase(); });
    this.selectedRule = new AMHRule(found.code, found.expression, found.dataOwner, found.lockCode, found.type);
    this.selectedExpression = found.expression.substr(0, 100) + " ..."
    this.logger.debug(" rule.code "+ rule.code+ " found code " + found.code);
    this.rules = [];
  }


  private add() {
    if (!this.selectedRule.code || !this.selectedRule.expression) {
      this.logger.log("no rule selected");
      return;
    } else {
      this.logger.log("selected rule " + this.selectedRule.code);
    }

    let addedRule = new AssignmentUniqueRule(this.selectedRule.code, this.selectedRule.dataOwner, this.selectedRule.lockCode, 0);
    addedRule.expression = this.selectedRule.expression;
    this.ruleAdded.emit(addedRule);
    this.statusChanged.emit(true);
    this.selectedRule = new AMHRule("", "");
    this.selectedExpression = "";
    this.ruleErrors.set(addedRule.code, ["Rule sequence is incorrect"]);
    this.logger.log(" adding " + addedRule.code);
  }

  private updateRules(filterText) {
    if (!filterText) {
      this.rules = [];
      return;
    }

    let assignedRuleCodeMap = {};

    this.selectedExpression = "";
    this.selectedRule.expression = "";
    
    this.existingRules.forEach(rule => assignedRuleCodeMap[rule.code] = true);

    let originalRulesMinusAssignedRules = this.originalRules.filter(rule => {
      return !assignedRuleCodeMap[rule.code];
    });

    let valueToFind = filterText.toUpperCase();

    let filteredRuleExpresssion:Array<any> = originalRulesMinusAssignedRules.filter((item:any) =>
     {  
      return item["expression"] && item["expression"].toUpperCase().match(valueToFind);
     }
      );

    let filteredRuleCode:Array<any> = originalRulesMinusAssignedRules.filter((item:any) =>
      item["code"] && item["code"].toUpperCase().match(valueToFind));
    
    let filteredData =  filteredRuleExpresssion.concat(filteredRuleCode);

    let uniqueList : Array<any>= [];
    filteredData.forEach( item => {
        let found = uniqueList.find((value, index, array) => { return item["code"] == value["code"]; });
        if (!found) {
          uniqueList.push(item);
        }
    });

    this.rules = uniqueList.map( rule => { return rule.cloneWithUpperCaseCode(); });
  }

  private errorMsg(code : string) {
    if (this.ruleErrors.get(code) && this.ruleErrors.get(code).length > 0) {
      return this.ruleErrors.get(code)[0];
    }

    return "";
  }

  private isInteger(x: number): boolean {
    return x % 1 === 0;
  }

  private getRulesUniquenessErrorMsgs(): any[] {
    if (!this.existingRules) {
      return [];
    }
    let hashRuleCode = {};
    let errorMessages2 : Array<any> = [];

    let errorMessages: any[] = this.existingRules.filter(rule => {
      if (hashRuleCode[rule.sequence]) {
        errorMessages2.push({"code": rule.code, "msg":rule.sequence + " is used by " +hashRuleCode[rule.sequence].code.toUpperCase()});
        return true;
      }

      hashRuleCode[rule.sequence] = rule;
      return false;
    }).map(rule => {
      return {"code": rule.code, "msg":rule.sequence + " is used by " +rule.code.toUpperCase()};
    });

    return errorMessages2;
  }
  
  private ruleSequenceUpdate(ruleCode: string, inputElem: HTMLInputElement) {
    
    let numericSeqValue: number = + inputElem.value;
    let rule = this.existingRules.find((rule) => { return rule.code === ruleCode; });

    if (rule === undefined) {
      this.logger.log("rulecode " + ruleCode + " not found in assignment rule list ");
      this.ruleErrors.set(rule.code, ["rule code" + ruleCode + " does not exist un assignment rule list"]);
      this.statusChanged.emit(true);
      return;
    }
    this.logger.log("emmiting assignment from content");
    // this.assignmentUpdate.emit(this.assignment);

    this.ruleErrors.set(rule.code, []);
    let oldSequence = rule.sequence;

    if (!numericSeqValue || !this.isInteger(numericSeqValue) || numericSeqValue < 0) {
      this.logger.log(" not valid sequence " + numericSeqValue + " old value " + rule.sequence);
      this.logger.log("++++setting is not good " + ruleCode);
      this.ruleErrors.set(rule.code, ["Rule sequence is incorrect"]);
      this.statusChanged.emit(true);
      return;
    }
    rule.sequence = numericSeqValue;
    // this.isDirty = true;
    let uniquenessErrors = this.getRulesUniquenessErrorMsgs();

    if (uniquenessErrors.length > 0) {
      this.logger.log("+++++has uniqueness errors returning old value of " + oldSequence);
      this.ruleErrors.set(rule.code, uniquenessErrors.map(obj => obj.msg));
      this.statusChanged.emit(true);
      // this.isDirty = false;
    } else {
      this.logger.log("there is no duplicates sequences on assignment rules ");
      let statusToSend = false;
      let tmpStatus = true;
      this.ruleErrors.forEach( (value, key, map) => {
          tmpStatus = false;
          if (value && value.length > 0) {
            tmpStatus = true;
            let nonDuplicateError = value.find( (msg) => { return msg.indexOf("is used by") > 0; });
            if (nonDuplicateError) {
              this.ruleErrors.set(key, []);
              tmpStatus = false;
            }
          }
          statusToSend = statusToSend || tmpStatus; 
      });
     
      this.statusChanged.emit(statusToSend);
    }
  }

  private deleteRule(rule: AMHRule) {
    this.confirmRuleDeletion();
    this.ruleCodeToDelete = rule.code;
  }
  
  actionNavigationEmit(rule : AMHRule) {
    this.logger.debug("going to rule edition");
    this.ruleNavigate.emit(rule.code);
  }

  confirmRuleDeletion() {
    this.alert.waitIcon = false;
    this.alert.alertFooter = true;
    this.alert.cancelButton = false;
    // this.alert.cancelButtonText = "Cancel";
    this.alert.yesButton = true;
    this.alert.yesButtonText = "Yes";
    this.alert.yesButtonResponse = 21;
    this.alert.okButton = true;
    this.alert.okButtonText = "No";
    this.alert.okButtonResponse = 22;
    this.alert.alertHeader = true;
    this.alert.alertTitle = " Alert ";
    this.alert.message = "Are you sure you want to unassign this rule?";
    this.alert.open();
  }

  confirmClose(data) {
    this.logger.log("confirming ------------  " + data);
    if (data === 0 ) {
      return;
    }

    if (data == 21) {
        this.logger.log("deleting... " + this.ruleCodeToDelete);
        this.existingRules = this.existingRules.filter((item: any) =>
            item["code"] !== this.ruleCodeToDelete);
        this.ruleErrors.set(this.ruleCodeToDelete, []);
        // this.isDirty = true;
        this.ruleDeleted.emit(this.ruleCodeToDelete);
        this.ruleCodeToDelete = "";
        let uniquenessErrors = this.getRulesUniquenessErrorMsgs();
        this.statusChanged.emit(uniquenessErrors.length > 0);
        let updatedRuleErrosFlag = false;
        // let codeWithErrors : Array<string> = [];
        // this.ruleErrors.forEach(function (value, key, map) {
        //   if (value && value.length > 0) codeWithErrors.push(key);
        // });
        this.ruleErrors = new Map<string, string[]>();
        uniquenessErrors.forEach(error => {
          this.ruleErrors.set(error.code, [error.msg]);
        });
        this.table.setPage(1, 5);
        // this.assignmentUpdate.emit(this.assignment);
    }

    if (data == 22) {
      this.logger.log("no deletion for rule "+this.ruleCodeToDelete);
      this.ruleCodeToDelete = "";
    }
  }
}
