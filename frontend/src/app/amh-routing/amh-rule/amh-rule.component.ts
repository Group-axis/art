import {ElementRef, Component, ViewChild, Directive, provide} from 'angular2/core';
import { NG_VALIDATORS, NgClass, Validators, Control, ControlGroup, FormBuilder, CORE_DIRECTIVES, FORM_DIRECTIVES, FORM_PROVIDERS } from '@angular/common';
import { ACCORDION_DIRECTIVES, BUTTON_DIRECTIVES } from 'ng2-bootstrap';
import { Router, RouteParams} from 'angular2/router';
import {FormFieldComponent} from '../../common/components/ui/widgets/form-field';
import {FormLabelComponent} from '../../common/components/ui/widgets/label';
import {AMHRule} from '../../models/routing-amh';
import {AMHRoutingService} from "../";
import {Observable} from 'rxjs/Observable';
import {FormFieldComponentExample, BootstrapFormDirective, BootstrapInputDirective, LabelsComponent, LabelsValueAccessor, notEmpty} from '../../common/components/ui/controls';
import {TreeView, TreeNode, TreeViewAccessor, TreeSelectionService} from '../../common/components/ui/widgets/tree-view';
import {Alert} from '../../common/components/ui/widgets/modal';
import {CustomValidatorsComponent} from '../../common/components/ui/controls';
import {Config, Store, Auth, Logger} from '../../common/components/services';
import {MenuConfig} from '../../models/menu';
import {HeaderSecondary} from '../../header-secondary';
// import {RuleValidationParser} from '../../../platform/browser';

//this.logger.log('`AMHRule` component loaded asynchronously');

@Component({
  selector: 'amh-rule',
  styles: [`
    h1 {
      font-family: Arial, Helvetica, sans-serif
    }
  `],
  template: require('./amh-rule.html'),
  directives: [ACCORDION_DIRECTIVES, BUTTON_DIRECTIVES, CORE_DIRECTIVES, FORM_DIRECTIVES, HeaderSecondary,  
    FormFieldComponent, FormLabelComponent, FormFieldComponentExample, BootstrapFormDirective, BootstrapInputDirective, LabelsComponent, LabelsValueAccessor, NgClass,
    TreeView, TreeViewAccessor, Alert],
  providers: [AMHRoutingService, FORM_PROVIDERS, TreeSelectionService, Store, Auth]
})
export class AMHRuleComponent {
  @ViewChild(Alert) alert;
  @ViewChild('textCriteria') criteriaTextElement;
  
  private ruleForm: ControlGroup;
  private rule: AMHRule = new AMHRule("", "");
  private ruleDataSource: Observable<AMHRule>;
  private criteriaDataSource: Observable<TreeNode>;
  private criteriaSearchText: string;
  private criterias: TreeNode[] = [];
  private originalCriterias: TreeNode[] = [];
  private nodeSelected: string = "N/A";
  private returnToString: string;
  private returnParameters: any = { };
  private parameters = {};
  private isCreation: boolean = false;
  private criteriaPosition: number;
  private isDirty : boolean = false;
  private navigateUrl: string;
  private hasUserPermissions : boolean = true;
  private currrentCriteriaPosition : any = {"x":0,"y":1};
  private parseMessage : string = "";
  private isRuleValid : boolean = true;
  private st : string = undefined;
  private assignmentCode : string = undefined;
  private RuleValidationParser : any = undefined;
  private menuConfig : Array<MenuConfig> = [
    new MenuConfig("fa fa-home","Home","Home"),
    new MenuConfig("fa fa-sitemap","AMHHome","AMH Routing"),
    new MenuConfig("fa fa-cogs","","Rule Management")];

  constructor(private router: Router, private amhRoutingService: AMHRoutingService, 
        routeParams: RouteParams, fb: FormBuilder, private treeSelectionService: TreeSelectionService
        , private store : Store, private auth: Auth, private logger : Logger) {
          
   
    this.isCreation = routeParams.params['code'] === undefined || routeParams.params['code'] === '';
    this.ruleDataSource = this.amhRoutingService.findRuleByCode(routeParams.params['code']);
    this.criteriaDataSource = this.amhRoutingService.findAllCriterias();
    this.returnToString = routeParams.params['return_to'] || 'AMHHome';
    this.setReturnToParameters(routeParams.params['params']);

    this.hasUserPermissions = auth.hasPermission(["amh.modify.rule"]) == 1;
    let ruleCodeCtrl = new Control('', Validators.required);
    
    if (this.isCreation && this.hasUserPermissions) {
      ruleCodeCtrl = new Control('', Validators.compose([Validators.required,CustomValidatorsComponent.validCode]), CustomValidatorsComponent.ruleCodeDuplication); 
    }
    let criteriaCtrl = new Control('', Validators.required);

    this.ruleForm = fb.group({
      'code': ruleCodeCtrl,
      'criteria': criteriaCtrl
    });

  }
 
 actionRuleChanged() {
   
   this.logger.log(" change ...");
 }

 loadParser() {
   this.amhRoutingService.loadAMHRuleGrammar("AMHRuleGrammar.pegjs")
    .subscribe(
      parser =>{  this.RuleValidationParser = parser; this.actionParse();},
      error => console.error(JSON.stringify(error))
    );
 }

  actionParse() {
    if (this.RuleValidationParser== undefined || !this.rule.expression || this.rule.expression.length == 0) {
      this.logger.info("Nothing to parse");
      this.parseMessage = "Rule is valid";
      this.isRuleValid = true;
      return;
    }
    this.logger.debug("Parsing...");

    try {
        this.RuleValidationParser.parse(this.rule.expression);
        this.parseMessage = "Rule is valid";
        this.isRuleValid = true;
    } catch(syntaxError) {
        this.logger.debug(" msgObj "+JSON.stringify(syntaxError));
        let location = syntaxError.location.start; 
        let errorMsg = "Line "  + location.line + ", Column " + (location.column-1) + ": "; 
        errorMsg += syntaxError.message || "Error while parsing";
        this.parseMessage = errorMsg;
        this.isRuleValid = false; 
        this.logger.debug(errorMsg);
    }

  //  this.logger.debug(" is rule O.K. : "+RuleValidationParser.parse(this.rule.expression));    
  }

  // private addReturnParamter(name : string, value : any) {
  //   if (!value) {
  //     return;
  //   }

  //   this.parameters[name] = value;
  // }

  private setReturnToParameters(params: string) {

    this.logger.log(" receiving params to return = " + params);

    if (!params || params.length == 0) {
      return;
    }


    let parameters = params.split('&');
    parameters.forEach(pair => {
      let pairValues = pair.split("=");
      this.parameters[pairValues[0]] = pairValues[1];
      if (pairValues[0]=='st') {
        this.st = pairValues[1];
      } else if (pairValues[0] == 'code') {
        this.assignmentCode = pairValues[1];
      }
      this.logger.log(" paramteres to return " + pairValues[0]+" = "+ pairValues[1]);
    });

  }

  ngOnInit() {
     this.loadParser();
    this.logger.log('hello `AMH Routing Rule` component');
    // Get the data from the server
    this.isDirty = false;
    this.ruleDataSource.subscribe(
      data => {
        this.logger.log("Rule retrieved from service " + data + " type " + (typeof data));
        this.rule = AMHRule.fromJson(data);
      },
      err =>
        this.logger.log("Can't get a rule. Error code: %s, URL: %s ", err.status, err.url),
      () => {
        this.rule = this.rule || new AMHRule("", "");
        this.logger.log('Rule(s) done ' + this.rule.expression);
        this.criteriaPosition = this.rule.expression.length;
        this.logger.log('Rule(s) are retrieved');
        this.actionParse();
      }
    );

    this.criteriaDataSource.subscribe(
      data => {
        if (Array.isArray(data)) {
          this.logger.log("Array " + data);
          //  this.data=data;
        } else {
          this.criterias.push(data);
          this.originalCriterias.push(data);
        }
      },
      err =>
        this.logger.log("Can't get a criteria. Error code: %s, URL: %s ", err.status, err.url),
      () => this.logger.log('Criteria(s) are retrieved')
    );
  }

  asyncDataWithWebpack() {
  }

  private setTextInTextArea(inputElem: ElementRef, selectionStart: number, selectionEnd: number) {
    let input = inputElem.nativeElement;
    if (input.setSelectionRange) {
      input.focus();
      input.setSelectionRange(selectionStart, selectionEnd);
    }
    else if (input.createTextRange) {
      var range = input.createTextRange();
      range.collapse(true);
      range.moveEnd('character', selectionEnd);
      range.moveStart('character', selectionStart);
      range.select();
    }
  }

  private selectedNode(value: string) {
    this.logger.log(" selectedNode.value " + value);
  }

  private blurM(position: number) {
    this.logger.log("now!!!! blur!!! " + position);
    this.criteriaPosition = position;
  }

  private updateCriteriaTree(value: string) {
    this.criterias = this.filterChildren(TreeNode.cloneTree(this.originalCriterias), value.replace(/\s/g, ''));
    this.treeSelectionService.selectionDone("");
  }

  private addToCriteriaTextField() {
    if (!this.hasUserPermissions) {
        return;
    }
    let value = this.treeSelectionService.getSelection();
    if (!value || value.length == 0) {
        this.logger.log("no critera selected");
        return;
    }
    this.logger.log("addToCriteriaTextField called " + this.nodeSelected + " from service " + value);

    let output = [this.rule.expression.slice(0, this.criteriaPosition), value, this.rule.expression.slice(this.criteriaPosition)].join('');
    this.rule.expression = output;
    
    this.setTextInTextArea(this.criteriaTextElement, this.criteriaPosition, this.criteriaPosition + 5);
    this.isDirty = true;
    this.actionParse();
    this.logger.log(" position " + this.criteriaPosition);
  }

  private updateBackends(filterText: string) {
  }


  private actionUpdateCursorPosition(el) {
    this.actionParse();
    if ('selectionStart' in el) {
        this.currrentCriteriaPosition.x = el.selectionStart;
    } else if ('selection' in document) {
        el.focus();
        var Sel = document["selection"].createRange();
        var SelLength = document["selection"].createRange().text.length;
        Sel.moveStart('character', -el.value.length);
        this.currrentCriteriaPosition.x = Sel.text.length - SelLength;
    }
    
} 
//   private changeFilter(data: any, config: any): any {
//     if (!config.filtering || !config.filtering.filterString) {
//       this.logger.log("empty filter  " + config.filtering);
//       return data;
//     }
// this.logger.log("changeFilter before filtering...");
//     let filteredData: Array<any> = data.filter((item: any) => item[config.filtering.columnName].toLowerCase().match(config.filtering.filterString.toLowerCase()));
// this.logger.log("changeFilter after filtering...");
//     return filteredData;
//   }


  private filterChildren(data: Array<any>, filterString: string) {

    let filteredData = data.filter(item => {
      let myself: boolean = item["searchCode"].toLowerCase().match(filterString.toLowerCase());
      if (myself) {
        return true;
      }

      if (!item["children"] || item["children"].length == 0) {
        return false;
      }

      let filteredChildren: Array<any> = this.filterChildren(item["children"], filterString);
      item["children"] = filteredChildren;

      return filteredChildren.length > 0;
    });

    return filteredData;
  }

  alertOpen() {
    this.alert.alertFooter = false;
    this.alert.cancelButton = false;
    this.alert.okButton = false;
    this.alert.alertHeader = true;
    this.alert.alertTitle = "Alert";
    this.alert.message = "Save in progress...";
    this.alert.cancelButtonText = "Ok";
    this.alert.open();
  }

  confirmClose(data) {
    this.logger.log("confirming ------------  " + data);
    switch(data) {
      case 300: // Navigate Cancel 
        //Do nothing
      break;
      case 301: //Navigate yes, lost changes
        this.router.parent.navigate([this.navigateUrl, this.getParameters()]);
      break;
    }
  }

  closeAlert() {
    setTimeout(() => this.alert.cancel(), 1000);
    setTimeout(() => this.router.parent.navigate([this.returnToString, this.getParameters()]), 1200);
  }

  private getParameters() : any {
    return this.parameters;
  }
  private disableSaveButton() {
    // this.ruleForm. 
  }

  private save() {
    this.logger.log("saving rule ..." + this.rule.code + " isCreation " + this.isCreation);
    this.rule.setEnvAndVersion(this.store.getCurrentEnv(), this.store.getCurrentVersion());
    this.logger.debug(" rule "+this.rule.version + " store "+ this.store.getCurrentVersion())
    this.alertOpen();
    let send;
    if (this.isCreation) {
      send = this.amhRoutingService.createRule(this.rule, this.auth.getUser());
    } else {
      send = this.amhRoutingService.saveRule(this.rule, this.auth.getUser());
    }
    send.subscribe(
      data => {
        this.logger.log("[saveRule] %s", data);
      },
      err => {
        this.logger.log("[saveRule] The rule cannot be saved. Error code: %s, URL: %s ", err.status, err.url);
        this.alert.message = "The rule cannot be saved !!";
        this.closeAlert();
      },
      () => {
        this.logger.log("[saveRule] assignment's rules from backend [%s,%s+] retrieved");
        this.alert.message = "The rule saved successfuly !!";
        this.closeAlert();
      }
    );

  }

  private actionCancel() {
    this.verifyAndNavigate(this.returnToString, 300, 301);
  }

  private getActions = (): string[] => {
    //TODO: service call to server returning json string
    let actionsJSON: string = `[ 
          { "text": "None", "id": 0, "code":"NONE", "children": []},
			{ "text": "Dispose to", "code":"ACTION_TYPE_ROUTING_POINT", "id":1,
                "children": [
                    { "text": "SARMTR", "code":"SARMTR", "id": 11 },
                    { "text": "EMRGZR", "code":"EMRGZR", "id": 12 },
                    { "text": "SEDED", "code":"SEDED", "id": 13 },
                    { "text": "MRTTRE", "code":"MRTTRE", "id": 14 }
                    ]},
            { "text": "Complete", "id": 2, "code":"ACTION_TYPE_COMPLETE", "children": []},
            { "text": "To adresse", "id": 3, "code":"ACTION_TYPE_ADDRESSEE", "children": []}
        ]`;

    return JSON.parse(actionsJSON);
  }

private alertPreventLostChanges(cancelResponse: number, yesResponse : number) {
    this.alert.waitIcon = false;
    this.alert.alertFooter = true;
    this.alert.cancelButton = true;
    this.alert.cancelButtonText = "Cancel";
    this.alert.cancelButtonResponse = cancelResponse;
    this.alert.yesButton = true;
    this.alert.yesButtonText = "Yes";
    this.alert.yesButtonResponse = yesResponse;
    this.alert.okButton = false;
    this.alert.alertHeader = true;
    this.alert.alertTitle = " Alert ";
    this.alert.message = "All changes will be lost, Do you want to continue?";
    this.alert.open();
  }
  
  private verifyAndNavigate(link : string, cancelResponse : number, yesResponse : number) {
     
    if (this.isDirty) {
        this.logger.debug("inside if "+ this.isDirty);
        this.navigateUrl = link;
        this.alertPreventLostChanges(cancelResponse, yesResponse);
        return;
    }

    this.logger.debug("it was not dirty going to "+link); 
    this.router.parent.navigate([link, this.getParameters()]);
  }


   actionNavigate(link : string) {
     this.verifyAndNavigate(link, 300, 301);
   }
}


