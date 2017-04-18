import { ElementRef, Component, ComponentMetadata, ViewChild } from 'angular2/core';
import { NgClass, CORE_DIRECTIVES, NgFormControl, Control } from '@angular/common';
import { ButtonRadioDirective } from 'ng2-bootstrap';
import { Router, RouteParams } from 'angular2/router';
import { FormFieldComponent } from '../../common/components/ui/widgets/form-field';
import { FormLabelComponent } from '../../common/components/ui/widgets/label';
import { AMHRule } from '../../models/routing-amh';
import { AMHRoutingService } from "../amh-routing.service";
import { AMHAssignmentService } from "../amh-service/amh-assignment.service";
import { Observable } from 'rxjs/Observable';
import { Subject } from 'rxjs/Subject';
import { Scheduler } from 'rxjs/Scheduler';
import { NumberFormatPipe } from '../../common/components/ui/controls';
import { Alert, Modal } from '../../common/components/ui/widgets/modal';
import { AssignType } from "../../models/routing-amh";
import { Option } from '../../models/referential/option';
import { MenuConfig } from '../../models/menu';
import { SimulationJob, Message, SimulationResult } from '../../models/simulation';
import { HeaderSecondary } from '../../header-secondary';
import { FileUploadService, FileDownloader, Config, Auth, Logger } from '../../common/components/services';
import { DataTable, DataTableDirectives } from 'angular2-datatable/datatable';
import { AMHSelectionTableComponent } from '../amh-selection-table';
import { AssignmentUnique, AssignmentUniqueRule, AssignmentUniqueBackend, BackendPK } from "../../models/routing-amh";
import { AMHAssignmentSearchComponent } from '../amh-assignment-search';
import { MessageHandlerComponent } from '../../common/components/ui/widgets/message-handler';

import { Progressbar } from '../../common/components/ui/widgets/progress-bar'

//this.logger.log('`AMHSimulator` component loaded asynchronously');



@Component({
  selector: 'amh-simulator',
  styles: [`
    h1 {
      font-family: Arial, Helvetica, sans-serif
    }
  `],
  template: require('./amh-simulator.html'),
  directives: [CORE_DIRECTIVES, ButtonRadioDirective,
    FormFieldComponent, FormLabelComponent, NgClass, Alert, HeaderSecondary, DataTableDirectives
    , AMHSelectionTableComponent, AMHAssignmentSearchComponent, Progressbar, MessageHandlerComponent],

  providers: [Auth, AMHAssignmentService, AMHRoutingService, FileUploadService, FileDownloader],
  pipes: [NumberFormatPipe]
})
export class AMHSimulatorComponent {
  @ViewChild(Alert) alert;
  @ViewChild(DataTable) table;

  private originalMessages: Array<Message> = [];
  private totalSelectedMessages: number = 0;
  private totalSelectedRules: number = 0;
  private returnToString: string;
  private parameters = {};

  private isImport: boolean;
  private filePath: string;
  private uploadProgress: number = 0;
  private fileList: File[] = new Array<File>();
  private menuConfig: Array<MenuConfig> = [
    new MenuConfig("fa fa-home", "/home", "Home"),
    new MenuConfig("fa fa-sitemap", "/amh-routing", "AMH Routing"),
    new MenuConfig("fa fa-play-circle-o", "", "Routing Simulator")];

  private selectionTables: Array<Option> = [
    new Option(AssignType.BK_CHANNEL, "BACKEND", "Backend Channel"),
    new Option(AssignType.DTN_COPY, "DISTRIBUTION", "Distribution Copy"),
    new Option(AssignType.FEED_DTN_COPY, "FEEDBACK", "Feedback Distribution Copy")
  ];
  private defaultOption: Option;

  private assignment: AssignmentUnique;
  private receivedAssignmentCode: string;
  private receivedRuleCode: string = "";
  private assignmentTextFilter: string;
  private executionResults: Array<SimulationResult> = [];
  private selectedAssignments: Array<AssignmentUnique> = [];
  private resultPendingMessage: string = "";
  private resultPendingStatus: number = 0;
  private currentJobId: number = 0;
  private loadedRules: Array<SimulationResult> = [];
  private selectedRules: Array<SimulationResult> = [];
  //private assignmentRules: Array<SimulationResult> = [];
  private selectedMessages: Array<Message> = [];
  private selectedMatch: string = "1"; //1 => first match; 2 => match all
  private selectAllRules: boolean = false;
  private loadedParams: any = undefined;
  private loadedParamSelectedMessages: Array<Message> = [];
  private hitMessage: string = "";
  private RuleEvaluationParser: any = {};
  constructor(private router: Router, private amhRoutingService: AMHRoutingService,
    private fileUploadService: FileUploadService,
    private fileDownloader: FileDownloader,
    private amhAssignmentService: AMHAssignmentService,
    private config: Config,
    routeParams: RouteParams,
    private auth: Auth,
    private logger: Logger) {
    this.logger.log(" selectionTable " + routeParams.params['st'] + " receivedAssignmentCode " + this.receivedAssignmentCode);

    this.loadAMHRuleGrammar();

    this.receivedAssignmentCode = routeParams.params['code'];
    this.receivedRuleCode = routeParams.params['ruleCode'] || "";
    //  this.assignmentTextFilter = this.receivedAssignmentCode || '';
    this.createNewAssignment();

    this.defaultOption = this.selectedAssignmentType(+ routeParams.params['st']);

    //TODO: REMOVE THIS AND PUT IT IN MESSAGE-HANDLER Component
    this.amhRoutingService.findMessages()
      .subscribe(
      message => { this.originalMessages.push(message); })

  }

  loadAMHRuleGrammar() {
    this.amhRoutingService.loadAMHRuleGrammar("AMHRuleGrammarEvaluation.pegjs")
      .subscribe(
      parser => this.RuleEvaluationParser = parser,
      error => console.error(JSON.stringify(error))
      );

  }

  ngOnInit() {
    this.logger.log('hello `AMH Simulator` component');
    this.loadAssignment(this.defaultOption, this.receivedAssignmentCode);
    this.loadAssignments(this.defaultOption);
    this.loadJobs();
  }

  private selectedAssignmentType(assignType: AssignType): Option {
    let typeSelected = this.selectionTables.find((type) => { return type.id === assignType; });
    return typeSelected ? typeSelected : this.selectionTables[0];
  }

  private getPendingMessage(status: number): string {
    let msg = "";
    switch (status) {
      case 1:
      case 2:
      case 6:
        msg = "You have an asynchronous execution in progress status. Please click on refresh button for updating status.";
        break;
      case 5:
        msg = "Your last asynchronous execution is canceled.";
        break;
    }
    return msg;
  }

  private parseParams(params: string): any {
    if (!params || params.length == 0) {
      this.logger.warn("no parameters found in job result");
      return;
    }

    this.logger.debug("treating " + params);

    return params.split("\n")
      .map(pair => {
        let values = pair.split("=");
        this.logger.debug("values " + values);
        return { "key": values[0], "value": values[1] };
      })
      .reduce((acc, pairValue) => {
        acc.set(pairValue.key, pairValue.value);
        this.logger.debug("key " + pairValue.key + " value " + pairValue.value);
        return acc;
      }, new Map<string, string>());

  }

  private toAssignType(paramCode: string): AssignType {
    switch (paramCode) {
      case "'BACKEND'": return AssignType.BK_CHANNEL;
      case "'DTN_COPY'": return AssignType.DTN_COPY;
      case "'FEED_DTN_COPY'": return AssignType.FEED_DTN_COPY;
      default: return AssignType.BK_CHANNEL;
    }
  }

  private showParams() {
    this.assignmentTextFilter = "";
    let allMessages = this.loadedParams.get('ALL_MESSAGES');
    let messageIds = this.loadedParams.get('MESSAGE_ID') || '';
    let messageGroupIds = this.loadedParams.get('MESSAGE_GROUP') || '';
    this.logger.debug("allMessages " + allMessages + " messageIds = " + messageIds + "  messageGroupIds " + messageGroupIds);
    this.loadedParamSelectedMessages = [];
    setTimeout(() => {
      if ('YES' == allMessages) {
        this.logger.log("multiselectiontrue orignalMessages.size " + this.originalMessages.length);
        ///TODO: REMOVE THIS AND ADD AN INPUT UPDATE ALL into the component
        //Remove the issue of linked arrays between component by create a copy of the array
        this.loadedParamSelectedMessages = this.originalMessages.map(a => a);
        this.selectedMessages = this.loadedParamSelectedMessages;
      } else {
        let ids = messageIds.split(',').map(id => id);
        let groupNames = messageGroupIds.split(',')
          .filter(name => name.length > 2)
          .map(name => name.substring(1, name.length - 1));
        // let messagesById = this.messages
        let messagesById = this.originalMessages
          .filter(message => ids.find(id => id == message.id))
          .map(message => { message.selected = true; return message; });
        this.logger.debug("original size " + this.originalMessages.length);
        let messagesByGroupName = this.originalMessages
          .filter(message => groupNames.find(name => { return name == message.name; }))
          .map(message => { message.selected = true; return message; });

        this.logger.debug("messagesByGroupName size " + messagesByGroupName.length + " messagesById size " + messagesById.length);
        this.selectedMessages = messagesById.concat(messagesByGroupName);
        this.logger.debug("this.selectedMessages size " + this.selectedMessages.length);

        this.loadedParamSelectedMessages = this.selectedMessages;
        // this.logger.debug("restoring "+JSON.stringify(this.selectedMessages))
      }
      this.updateTotalSelectedMessages();
    }, 350);

    let selectionTable = this.loadedParams.get('SELECTION_TYPE') || "'BACKEND'";
    this.defaultOption = this.selectedAssignmentType(this.toAssignType(selectionTable));
    this.loadAssignments(this.defaultOption);
    this.selectedMatch = this.loadedParams.get('MATCH_SELECTION') || '1';

    let assignmentName = this.loadedParams.get('BACKENDNAME') || '';

    this.assignmentTextFilter = assignmentName.length > 2 ? assignmentName.substring(1, assignmentName.length - 1) : '';

    this.selectedAssignments
      .map(assign => assign.code)
      .filter(code => { return this.assignmentTextFilter != code; })
      .forEach(code => { this.actionDeleteAssign(undefined); });

    this.logger.log("this.assignmentTextFilter " + this.assignmentTextFilter);

    //this.selectedRules.length
    setTimeout(() => {
      this.actionRuleMultiSelection(false);
      let ruleNames = this.loadedParams.get('SELECTED_RULES') || '';
      ruleNames.split(',').forEach(ruleName => {
        this.logger.debug("ruleName " + ruleName);
        this.assignment.rules
          .filter(rule => rule.code == ruleName)
          .map(rule => { rule.selected = true; return rule; });
      }
      );

      this.selectedRules =
        this.assignment.rules
          .filter(rule => rule.selected)
          .map(rule => this.toSimulationResult(this.assignment, rule, selectionTable));

      this.updateTotalSelectedRules();

    }, 350);

  }



  private loadJobs() {

    this.logger.debug("reinitializing result array");
    this.executionResults = [];
    this.resultPendingStatus = 0;
    this.resultPendingMessage = "";
    let instantLimit = this.config.getOrElse("max_instant_messages", 101);
    this.amhRoutingService.findJobs(this.auth.getUser(), SimulationJob.AMH_SYSTEM_ID, instantLimit)
      .flatMap(job => {
        let resp;

        if (!this.loadedParams) {
          this.loadedParams = this.parseParams(job.params);
        }

        this.currentJobId = job.id;
        this.resultPendingStatus = job.status;
        this.resultPendingMessage = this.getPendingMessage(job.status);

        if (job.status == 3) {
          this.logger.debug("job status == 3 output array length == " + job.numOfMessages);
          if (job.numOfMessages > instantLimit) {
            let OutOfLimitMessage = "Result size has exceded the limit of " + instantLimit + ". Please download the file  result.";
            resp = new SimulationResult(-1, "", -1, "", OutOfLimitMessage, -1, "", "", "");
            resp.hit = false;
            resp.withError = true;
            resp.status = job.status;
            this.resultPendingStatus = 99;
            this.resultPendingMessage = OutOfLimitMessage;
            return Observable.from([]);
          }
          /*File name;Message Reference;Selection Type;Assignment Sequence;Assignment Code;Backend Channel;Rule Sequence;Rule Code;Rule Expression*/
          /*File name;Message Reference;Selection Sequence;Selection Code;Rule Sequence;Rule Name;Rule Expression*/
          //new SimulationResult(assignment.sequence, assignment.code, rule.sequence, rule.code, rule.expression, -1, backendCodes);
          return Observable.from(
            job.outputAsArray.map(line => {
              let values = line.split(";");

              let resp = new SimulationResult(+values[3], values[4], +values[6], values[7], values[8], -1, values[5], values[2], values[1]);
              this.logger.debug("simulation loaded " + JSON.stringify(resp));
              resp.hit = resp.selectionSequence > 0 && resp.ruleSequence > 0 && resp.selectionCode.length > 0 && resp.ruleName.length > 0;
              resp.withError = false;
              resp.messageNameHit = values[0];
              resp.status = job.status;
              return resp;
            }));
        } else if (job.status == 4) {
          let errorMessage = "An error has occurred while processing the batch simulation.";
          resp = new SimulationResult(-1, "", -1, "", errorMessage, -1, "", "", "");
          resp.hit = false;
          resp.withError = true;
          resp.status = job.status;
          this.resultPendingStatus = 4;
          this.resultPendingMessage = errorMessage;
          return Observable.from([]);
        } else {
          resp = new SimulationResult(-1, "", -1, "", "", -1, "", "", "");
          resp.hit = false;
          resp.withError = false;
          resp.status = job.status;
          return Observable.from([]);
        }
      })
      //.filter(job => job.status >= 2 || job.status <= 6)
      .subscribe(
      res => {
        this.logger.debug("a hit from jobload ");
        this.executionResults = this.executionResults.concat([res]);
      },
      error => {
        this.logger.error("Error while loading jobs " + error.message);
      },
      () => {
        this.logger.log(this.executionResults.length + " job(s) loaded.");
        let hits = this.executionResults.reduce((acc, ele, index, ara) => { return ele.hit ? acc + 1 : acc; }, 0);
        this.hitMessage = hits > 0 ? hits + " hit(s) on " + this.executionResults.length + " simulation(s)" : " No rule(s) hit";
      }
      )

  }

  private loadAssignment(optionType: Option, code: string) {
    if (!code || code.length == 0) {
      this.createNewAssignment();
      this.updateTotalSelectedRules();
      return;
    }

    this.amhAssignmentService.findAssignmentByCode(optionType.id, code)
      .subscribe(
      data => {
        this.logger.log("[loadAssignment] Data retrieved from service: %s ", data.code);
        this.assignment = AssignmentUnique.fromJson(data);
        this.assignment.rules = this.assignment.rules.sort(
          (rule1, rule2) => rule1.sequence - rule2.sequence);

        this.rulesSelectionUpdate(true, optionType.code, this.receivedRuleCode);

      },
      err => {
        this.logger.warn("[loadAssignment] Can't get assignment. Error code: %s, URL: %s ", err.status, err.url);
        this.createNewAssignment();
        this.updateTotalSelectedRules();
      },
      () => {
        this.logger.log("[loadAssignment] assignment code [%s] retrieved", code);
        if (!this.assignment) {
          this.createNewAssignment();
        } else {
          this.selectedAssignments = [this.assignment];
        }
        this.updateTotalSelectedRules();
      }
      );

  }

  private createNewAssignment() {
    this.logger.log("creating new assignment");
    this.assignment = new AssignmentUnique(false, new BackendPK("", ""), "", "", "", "", undefined, undefined, "", "", "", [], []);
  }

  private executeOnAssignments(option: Option) {
    if (!option) {
      option = this.selectionTables[0];
    }
    let executionResult: Array<any> = [];
    this.amhAssignmentService.findAssignments(option.id).subscribe(
      data => {
        let resp = AMHAssignmentService.getFromSource(data);
        resp.map(assign => {
          // this.logger.debug(" loading "+assign.code)
          executionResult = executionResult.concat(this.executeOnAssignment(option.id, assign));
        });

        this.logger.debug("final size " + executionResult.length);
        this.executionResults = executionResult;
      },
      err =>
        this.logger.log("Can't get assignments. Error code: %s, URL: %s ", err.status, err.url),
      () => this.logger.log('Assignment(s) are retrieved')
    );

  }

  private executeOnAssignment(type: AssignType, assign: AssignmentUnique) {
    //TODO: apply the resulution
    return {};
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


  closeAlert() {
    setTimeout(() => this.alert.cancel(), 1000);
    //setTimeout(() => this.router.parent.navigate(["AMHHome"]), 1200);
  }

  private disableBatchSimulationButton(): boolean {
    //1. Verify that at least one msg is selected
    return this.selectedMessages.length == 0 || this.totalSelectedRules == 0;
  }

  private disableInstantSimulationButton(): boolean {
    return this.disableBatchSimulationButton() || this.totalSelectedMessages > this.config.getOrElse("max_instant_messages", 101);
  }

  actionExportInstanceResults() {
    if (this.executionResults.length == 0 || this.resultPendingStatus == 99) {
      this.logger.log(" there is no results to export ");
      return;
    }
    this.logger.log(" exporting results ");
    this.alertOpen();
    this.amhRoutingService
      .exportSimulationResults(this.executionResults, this.auth.getUser())
      .subscribe(
      data => {
        this.logger.log("[EXPORT_CSV] %s", data);
        this.fileDownloader.download(this.config.get("simulationBackUrl") + "/jobs/amh/export/" + data.fileName, data.fileName);
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

  actionExportResult() {
    if (this.resultPendingStatus != 3 && this.resultPendingStatus != 99) {
      return;
    }
    this.alertOpen();
    this.logger.log("export import ..." + " isImport " + this.isImport + " selected file " + this.filePath);
    let send;
    this.logger.log("[EXPORT_CSV] just before calling export post on server");
    send = this.amhRoutingService.exportJobResults(this.currentJobId, this.auth.getUser());
    this.logger.log("[EXPORT_CSV] after returning observer of export post");
    send.subscribe(
      data => {
        this.logger.log("[EXPORT_CSV] %s", data);
        this.fileDownloader.download(this.config.get("simulationBackUrl") + "/jobs/amh/export/" + data.fileName, data.fileName);
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
    this.logger.log("[EXPORT] after the subscribe finished! ");
  }

  actionLoadFile() {
    this.alertOpen();
    this.logger.log("export import ..." + " isImport " + this.isImport + " selected file " + this.filePath);
    let send;
    if (this.isImport) {
      this.uploadHandler().then(data => {
        this.alert.message = "Simulation Done.";
        this.closeAlert();
        this.logger.log(" PROMISE " + data);
      }, error => {
        this.logger.log(" ERROR while importing " + error);
        this.alert.message = " An error has occurred while processing the Simulation.";
        this.closeAlert();
      });
      //   let fromIndex = this.filePath.lastIndexOf('\\');
      //  send = this.amhRoutingService.import("c:/demo/"+this.filePath.substr(fromIndex+1));
    } else {
      this.logger.log("[EXPORT] just before calling export post on server");
      send = this.amhRoutingService.export("UNKNOWN", "DEFAULT", this.filePath, this.auth.getUser());
      this.logger.log("[EXPORT] after returning observer of export post");
      send.subscribe(
        data => {
          this.logger.log("[Simulation] %s", data);
          this.fileDownloader.download(this.config.get("backUrl") + "/amhrouting/export/" + data.response, data.response);

          // this.amhRoutingService.getExportedFile(data.response);
        },
        err => {
          this.logger.log("[Simulation] Can't be done. Error code: %s, URL: %s ", err.status, err.url),
            this.alert.message = " An error has occurred while processing the Simulation";
          this.closeAlert();
        },
        () => {
          this.logger.log("[Simulation]  from backend done");
          this.alert.message = "Simulation Done.";
          this.closeAlert();

        });
      this.logger.log("[EXPORT] after the subscribe finished! ");
    }
  }

  public uploadHandler(): Promise<any> {
    let result: any;

    this.fileUploadService.getObserver()
      .subscribe(progress => {
        this.uploadProgress = progress;
      });

    try {
      let extraParam: Map<string, string> = new Map<string, string>();
      extraParam.set("userId", this.auth.getUser().username);
      extraParam.set("time", "" + Date.now());
      return this.fileUploadService.upload(this.config.get("backUrl") + "/amhrouting/import", this.fileList, extraParam);
    } catch (error) {
      this.logger.error("An error has occurred while importing: " + error);
      document.write(error)
    }

  }

  private toSimulationResult(assignment: AssignmentUnique, rule: AssignmentUniqueRule, selectionTable: string): SimulationResult {
    let backendCodes = assignment.backendPrimaryKey ? assignment.backendPrimaryKey.code : assignment.backends
      .map(backend => backend.code)
      .reduce((acc, value) => acc + ", " + value, "");
    return new SimulationResult(assignment.sequence, assignment.code, rule.sequence, rule.code, rule.expression, -1, backendCodes, selectionTable, "");
  }

  private rulesSelectionUpdate(value: boolean, selectionOption: string, ruleCode?: string) {
    let selectAll = !ruleCode;
    //this.selectedRules = [];
    this.logger.debug("updating this.selectedRules v = " + value + " ruleCode " + ruleCode);

    let tmp = this.assignment.rules
      .map(rule => {
        if (selectAll || rule.code == ruleCode) { rule.selected = value; }
        return rule;
      });


    this.selectedRules = this.assignment.rules
      .filter(rule => rule.selected)
      .map(rule => { return this.toSimulationResult(this.assignment, rule, selectionOption); })

    this.assignment.rules = tmp;
    //this.assignment.rules.forEach(e => this.logger.log("------ "+e.code+" sel "+e.selected));
    this.logger.log("this.selectedRules.length = " + this.selectedRules.length);

  }

  private cancel() {
    this.router.parent.navigate(["AMHHome"]);
  }

  private calculTotalMessages(): number {
    return this.originalMessages
      .reduce((acc, msg) => {
        let toAdd = msg.groupCount ? msg.groupCount : 1;
        return acc + toAdd;
      }, 0);
  }


  /*
  SELECTION_TYPE='BACKEND'\nBACKENDNAME='xxxxbacknd'\nALL_MESSAGES=NO\nMESSAGE_GROUP='MT_103','MT_105'\nMESSAGE_ID=1,2,3\nSEND_MAIL=krajan@groupaxis.fr
   */
  actionExecuteAsBatch() {
    let user = this.auth.getUser();
    if (user.email) {
      this.alertYesNoCancel(100, 101, 0, "Do you want to receive an status update message by email ?");
    } else {
      this.alertYesCancel(101, 0, "Please add an email address to your profile to receive an email", "Ok");
    }
  }

  private executeAsBatchWithChoice(sendEmail: boolean) {

    let user = this.auth.getUser();
    let params = this.createParameters(sendEmail ? user.email : undefined);
    let job = SimulationJob.fromJson(
      {
        id: -1,
        user: user.username,
        status: 1,
        numOfMessages: this.totalSelectedMessages,
        params: params,
        jobLauncherSystem: SimulationJob.AMH_SYSTEM_ID
      });

    this.amhRoutingService.createJob(job, user)
      .subscribe(
      resp => {
        this.logger.log("job saved");
        this.resultPendingStatus = 0;
        this.executionResults = [];
        this.loadJobs();
        //this.upd
      },
      error => { this.logger.error("error while saving a job " + error.message); },
      () => { this.loadedParams = undefined; this.logger.log("job saved done!"); }
      );

    ///TODO:  send post to insert a job 
    //TODO: add user email logic
  }

  private selectionTableParam(): string {
    let selectionTable = "";
    switch (this.defaultOption.id) {
      case AssignType.BK_CHANNEL:
        selectionTable = "BACKEND";
        break;
      case AssignType.DTN_COPY:
        selectionTable = "DTN_COPY";
        break;
      case AssignType.FEED_DTN_COPY:
        selectionTable = "FEED_DTN_COPY";
        break;
    }
    return selectionTable;
  }

  private createParameters(userEmail?: string) {
    let params = "SELECTION_TYPE='" + this.selectionTableParam() + "'"
    if (this.selectedAssignments.length > 0) {
      params += "\nBACKENDNAME='" + this.assignment.code + "'";
      params += "\nSELECTED_RULES=" + this.selectedRules.map(r => r.ruleName).join(",");
    }

    let areAllMessagesSelected = this.calculTotalMessages() == this.totalSelectedMessages;
    params += "\nALL_MESSAGES=" + (areAllMessagesSelected ? "YES" : "NO");

    if (!areAllMessagesSelected) {
      let allMessageIds = this.selectedMessages
        .filter(message => !message.group || message.group.length == 0)
        .map(msg => msg.id);

      let messageGroup = this.selectedMessages
        .filter(message => message.group && message.group.length > 0)
        .map(msg => "'" + msg.name + "'");

      params += "\nMESSAGE_ID=" + allMessageIds.join(",");
      params += "\nMESSAGE_GROUP=" + messageGroup.join(",");
    }

    params += "\nMATCH_SELECTION=" + this.selectedMatch;
    params += "\nSEND_MAIL=" + (userEmail ? userEmail : "");

    return params;
  }

  actionCancelJob() {
    if (this.resultPendingStatus != 1 && this.resultPendingStatus != 2 && this.resultPendingStatus != 6) {
      return;
    }
    this.amhRoutingService.cancelJob(this.currentJobId, this.auth.getUser())
      .subscribe(
      ok => {
        this.resultPendingStatus = 5;
        this.resultPendingMessage = "Your asynchronous execution has been canceled.";
      },
      error => { },
      () => { }
      );
  }

  actionReExecuteJob() {
    if (this.resultPendingStatus != 3 && this.resultPendingStatus != 4 && this.resultPendingStatus != 5) {
      return;
    }
    this.amhRoutingService.reExecuteJob(this.currentJobId, this.auth.getUser())
      .subscribe(
      ok => {
        this.resultPendingStatus = 6;
        this.resultPendingMessage = "Your asynchronous execution has been launched.";
      },
      error => { },
      () => { }
      );
  }

  actionExecuteInstantOrNot() {
    let maxInstantMessages = this.config.getOrElse("max_instant_messages_alert", 25000);
    if (this.totalSelectedMessages > maxInstantMessages) {
      this.alertYesCancel(201, 0, "Your session has a risk of non-response, do you want to continue?", "Yes", " ! Degradation warning more than " + maxInstantMessages + " messages ! ");
    } else {
      this.actionExecute();
    }
  }

  actionExecute() {
    this.logger.log("executiing simulation");
    let start = new Date().getTime();
    this.resultPendingStatus = 0;
    //  Observable.interval(1).subscribe(x => this.currentValue = this.currentValue + 1);
    let progressObserver;
    let progress$ = Observable.create(observer => {
      progressObserver = observer
    })
      //     .map(function(value) {
      //  return value.observeOn(Scheduler.default);
      // })
      .share();

    // let progress$: Subject<number> = new Subject<number>();
    let startCounter = 0;
    let rulesProcessed = 0;

    // progress$.subscribe(p => {
    //  let newProgress  = Math.floor(p);
    //   this.currentValue = newProgress == 0 ? this.totalSelectedRules : newProgress;
    //   this.logger.debug("with Share rules "+p+"% treated. newValue "+this.currentValue);
    // },
    // err => this.logger.error("progress err : "+ err.message,
    // () => this.logger.log("progress$ done!")));
    // this.logger.log("progress.subscribe done!")

    //let executionResults = [];
    this.executionResults = [];
    let notHitMessages: Array<SimulationResult> = [];

    this.mergeAllMessages().subscribe(
      messages => {
        this.logger.debug("total messages selected to simulate = " + messages.length);
        this.messagesToProcess(messages).forEach(messageObs => {

          let process = messageObs.flatMap(message => {
            let toParse = "params = " + message.itemMap + ' ';
            // ms = message.name;

            return this.rulesToProcess()
              .map(result => {
                result.expressionToEvaluate = toParse + result.ruleExpression;
                result.paramsSize = toParse.length;
                result.messageName = message.name;
                try {
                  result.messageReference = JSON.parse(String(message.itemMap))["swiftParameters_requestReference"];
                } catch (e) { }

                return result;
              });
          })
            .map(result => { let value = this.processRule(result); return value; });

          //TODO: How to do to avoid double execution of process observable (one for each observer) 
          // Adding a subject does not help
          //let subject : Subject<SimulationResult> = new Subject<SimulationResult>(undefined, process);

          let alreadyAddedResultMap: Map<string, boolean> = new Map<string, boolean>();
          let alreadyRemovedResultMap: Map<string, boolean> = new Map<string, boolean>();

          let notHitObservableArray: Observable<Array<SimulationResult>> = process
            .filter(result => {
              if (result.hit) {
                // this.logger.debug( result.messageName + " has a hit ");
                if (alreadyAddedResultMap.has(result.messageName)) {
                  let removeSuccess = alreadyAddedResultMap.delete(result.messageName);
                  // this.logger.debug(" Removing  " + result.messageName + " with success ? "+ removeSuccess);
                }
                alreadyRemovedResultMap = alreadyRemovedResultMap.set(result.messageName, true);
                return false;
              }
              else {
                // this.logger.debug(" No hit received " + result.messageName);
                if (!alreadyAddedResultMap.has(result.messageName) && !alreadyRemovedResultMap.has(result.messageName)) {
                  // this.logger.debug(" adding " + result.messageName + " already removed? " + (alreadyRemovedResultMap.has(result.messageName)));
                  alreadyAddedResultMap = alreadyAddedResultMap.set(result.messageName, true);
                  return true;
                } else {
                  return false;
                }
              }
            })
            .toArray();

          if (this.selectedMatch == "1") { //first match
            let defaultValue = new SimulationResult(0, "", 0, "", "No rule(s) hit", 0, "", "", "");
            process = process.first((result, index) => result.hit, (result) => result, defaultValue);
          }

          process.subscribe(
            res => {

              // this.currentValue = this.currentValue + 1;
              //this.logger.debug("receving "+this.currentValue + " for rule " + res.ruleName+" msg "+res.messageNameHit);        
              if (res.errorMsg) {
                this.logger.error(" ruleCode " + res.ruleName + " => " + res.hit + " err " + res.errorMsg);
              }

              if (res.hit) {
                this.executionResults = this.executionResults.concat([res]);
                this.resultPendingStatus = 0;
              }

            },
            err => {
              if (this.selectedMatch == "1" && err.name === "EmptyError") {
                this.logger.log("No hit found ");
                // if (this.executionResults.length == 0) {
                //   this.resultPendingStatus = -1;
                //   this.resultPendingMessage = "No hits found with current rule(s) and message(s) selection.";
                // }
              } else {
                this.logger.error("error " + JSON.stringify(err) + " msg " + err.message);
              }
            },
            () => {
              // this.currentValue = 0;
              let end = new Date().getTime();
              this.logger.debug("finished after " + (end - start) + "milliseconds");
              if (this.executionResults.length == 0) {
                this.resultPendingStatus = -1;
                this.resultPendingMessage = "No hits found with current rule(s) and message(s) selection.";
              }

              notHitObservableArray.subscribe(re =>
                //Use map and filter instead of forEach ?? performance ?? 
                re.forEach(r => {
                  if (alreadyAddedResultMap.get(r.messageName)) {
                    let noHitRes = new SimulationResult(0, "", 0, "", "No rule(s) hit", 0, "", "", "");
                    noHitRes.messageNameHit = r.messageName;
                    notHitMessages = notHitMessages.concat([noHitRes]);
                  }
                  //              this.logger.log("finally- " + r.messageName + " hit? " + alreadyAddedResultMap.get(r.messageName));
                })
              );

            }
          );
        });
        this.logger.warn("THE FOR IS SYNCHRONOUS!!! ");
        let hits = this.executionResults.length;
        this.hitMessage = hits ? hits + " hit(s) on " + (hits + notHitMessages.length) + " simulation(s)" : " No rule(s) hit ";
        this.executionResults = this.executionResults.concat(notHitMessages);
      }
    )

  }

  private processRule(res: SimulationResult): SimulationResult {
    try {
      //      this.logger.debug(" evaluating " + res.expressionToEvaluate);
      res.hit = this.RuleEvaluationParser.parse(res.expressionToEvaluate);
      //     this.logger.debug(" after " + res.hit);
      if (res.hit) res.messageNameHit = res.messageName;
      res.withError = false;
      res.errorMsg = "";
    } catch (syntaxError) {
      let location = syntaxError.location.start;
      // this.logger.log(" col " + location.column + " paramSize " + res.paramsSize);
      let errorMsg = "Line " + location.line + ", Column " + (location.column - res.paramsSize) + ": ";
      errorMsg += syntaxError.message || "Error while parsing";
      res.hit = false;
      res.withError = true;
      res.errorMsg = errorMsg;
      //this.logger.error(errorMsg);
    }

    return res.clone();
  }

  private flatMap<T, E>(array: Array<T>, f: (t: T) => Array<E>): Array<E> {
    return array.reduce((ys: any, x: any) => {
      return ys.concat(f.call(array, x))
    }, [])
  }

  private groupMessages = message => message.group && message.group.length > 0;
  private singleMessages = message => !message.group || message.group.length == 0;

  private mergeAllMessages(): Observable<Array<Message>> {
    let selectedGroupIds = this.selectedMessages
      .filter(this.groupMessages)
      .map(msg => msg.group);

    return this.amhRoutingService.loadFlatGroupMessages(selectedGroupIds)
      .flatMap(
      resp => {
        if (resp.found) {
          this.logger.debug("found resp OK " + JSON.stringify(resp.messages[0]));
          return Observable.from([this.selectedMessages
            .filter(this.singleMessages).concat(resp.messages)]);

        } else {
          this.logger.debug("No group messages found");
          return Observable.from([this.selectedMessages.filter(this.singleMessages)]);
        }
      });

    // let allMessages = this.flatMap(this.selectedMessages,
    //   message => message.group ? message.messages : [message]);
  }

  private messagesToProcess(allMessages: Array<Message>): Array<Observable<Message>> {
    if (this.selectedMatch == "1") { //first match
      return allMessages.map(message => Observable.from([message]));
    } else {
      return [Observable.from(allMessages)];
    }
  }

  updateTotalSelectedRules() {
    //this.logger.debug("thisLoadedRules: "+this.loadedRules.length);
    this.totalSelectedRules = this.assignment.rules.length == 0 ? this.loadedRules.length : this.selectedRules.length;
    this.selectAllRules = this.totalSelectedRules == this.assignment.rules.length && this.assignment.rules.length > 0;
  }

  private rulesToProcess(): Observable<SimulationResult> {
    if (this.assignment.rules.length == 0) {
      this.logger.debug("Processing a total of " + this.loadedRules.length + " rules. ");
      return Observable.from(this.loadedRules);
    } else {
      this.logger.debug("Processing " + this.selectedRules.length + " selected rules.");
      return Observable.from(this.selectedRules);
    }
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

  private alertYesCancel(yesResponse: number, cancelResponse: number, message: string, yesLabel?: string, alertTitle: string = " Alert ") {
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
    this.alert.alertTitle = alertTitle;
    this.alert.message = message;
    this.alert.open();
  }

  private alertYesNoCancel(yesResponse: number, noResponse: number, cancelResponse: number, message: string) {
    this.alert.waitIcon = false;
    this.alert.alertFooter = true;
    this.alert.cancelButton = true;
    this.alert.cancelButtonText = "Cancel";
    this.alert.cancelButtonResponse = cancelResponse;
    this.alert.yesButton = true;
    this.alert.yesButtonText = "Yes";
    this.alert.yesButtonResponse = yesResponse;
    this.alert.okButton = true;
    this.alert.okButtonText = "No";
    this.alert.okButtonResponse = noResponse;
    this.alert.alertHeader = true;
    this.alert.alertTitle = " Alert ";
    this.alert.message = message;
    this.alert.open();
  }

  alertResponse(resp) {
    switch (resp) {
      case 0: //Delete message cancel
        // this.saveStatus = "Cancel";
        break;
      // case 1: //Delete message Yes
      //   this.deleteMessage();
      //   break;
      case 100: //Send an email 'Yes' response
        this.executeAsBatchWithChoice(true)
        break;
      case 101: //Send an email 'No' response.
        this.executeAsBatchWithChoice(false);
        break;
      case 201: //Continue execution on max instant limit execeded
        this.actionExecute();
        break;
    }
  }

  actionUpdateSelectionTable(selectionOption: Option) {
    this.logger.log("selection table changed " + JSON.stringify(selectionOption));
    this.defaultOption = selectionOption;
    this.actionDeleteAssign({});
    this.loadAssignments(selectionOption);
    this.assignmentTextFilter = "";
  }

  private loadAssignments(selectionOption: Option) {
    this.loadedRules = [];
    this.amhAssignmentService.findAssignments(selectionOption.id)
      .subscribe(data => {
        let loadedAssignments = AMHAssignmentService.getFromSource(data);
        // let array : Array<AssignmentUnique> = new Array(loadedAssignments);
        loadedAssignments.forEach(
          assign => {
            assign.rules = assign.rules.sort((r1, r2) => r1.sequence - r2.sequence);
            assign.rules.forEach(rule => this.loadedRules.push(this.toSimulationResult(assign, rule, selectionOption.code))
            );
          });
      },
      err => {
        this.updateTotalSelectedRules();
        this.logger.error("while loading assignment type " + selectionOption.id)
      },
      () => {
        this.logger.debug(this.loadedRules.length + " rules(s) retrieved. ");
        this.updateTotalSelectedRules();
        //this.logger.debug("first result: " + JSON.stringify(this.loadedRules[0]));
      });
  }

  actionAssignmentSelected(assignmentCode: string) {
    this.loadAssignment(this.defaultOption, assignmentCode);
  }

  actionDeleteAssign(assignmnet) {
    this.selectedAssignments = [];
    this.createNewAssignment();
    this.updateTotalSelectedRules();
    this.assignmentTextFilter = "";
  }

  actionRuleMultiSelection(selected) {
    this.logger.debug("multi rule selection " + selected);
    this.rulesSelectionUpdate(selected, this.defaultOption.code);
    this.updateTotalSelectedRules();
  }

  actionRuleSelection(rule: AssignmentUniqueRule, selected) {
    this.processSelection(this.selectedRules, rule, selected, this.defaultOption.code);
    this.updateTotalSelectedRules();
  }

  private updateTotalSelectedMessages() {
    this.totalSelectedMessages =
      this.selectedMessages
        .reduce((acc, msg) => {
          let toAdd = msg.groupCount ? msg.groupCount : 1;
          return acc + toAdd;
        }, 0);
  }
  actionMessageSelection(messages: Array<Message>) {
    this.selectedMessages = messages;
    this.updateTotalSelectedMessages();
    this.logger.info("after update on selection " + this.originalMessages.length);
  }

  // actionMessageSelectionOriginal(message: Message, selected) {
  //   this.logger.debug(" message selected " + message.name);
  //   message.selected = selected;
  //   if (selected) {
  //     this.selectedMessages.push(message);
  //   } else {
  //     this.deleteFromArray(this.selectedMessages, (m) => message.id == m.id);
  //   }
  //   // this.updateTotalSelectedMessages();
  // }

  // private changeFilter(data: any, config: any): any {
  //   if (!config.filtering) {
  //     return data;
  //   }

  //   let valueToFind = config.filtering.filterString.toUpperCase();
  //   return data.filter((item: any) =>
  //     item[config.filtering.columnName].toUpperCase().match(valueToFind));
  // }

  private processSelection(array: Array<any>, rule: AssignmentUniqueRule, selected: boolean, selectionOption: string) {
    rule.selected = selected;
    let item = this.toSimulationResult(this.assignment, rule, selectionOption);
    if (selected) {
      array.push(item);
      array.forEach(e => this.logger.log("e " + e.ruleSequence));
      array = array.sort((a, b) => a.ruleSequence - b.ruleSequence);
      array.forEach(e => this.logger.log("e " + e.ruleSequence));
    } else {
      this.deleteFromArray(array, (r) => {
        //this.logger.log("item rs="+item.ruleSequence+" item.s="+item.sequence+" r.rs="+r.ruleSequence+" r.s="+item.sequence);
        // let found = item.ruleSequence == r.sequence || item.sequence == r.sequence
        // || item.ruleSequence == r.ruleSequence || item.sequence == r.ruleSequence;
        this.logger.log("item rs=" + item.ruleSequence + " r.rs=" + r.ruleSequence);
        let found = item.ruleSequence == r.ruleSequence;
        this.logger.log("foud===" + found);
        return found;
      });
      this.logger.debug("LLLLLLLLLL AFTER DELETING LLLLLLLLLLL");
      array.forEach(e => this.logger.log("         seq " + e.ruleSequence + " name " + e.ruleName));
    }
  }

  private deleteFromArray(array: Array<any>, predicate: (v) => boolean) {
    let index = array.findIndex(predicate);
    this.logger.log("index found " + index);
    if (index > -1) {
      array.splice(index, 1);
    }
  }

  // private currentValue: number;
  // private type: string;
  // private generateNewProgressValues() {
  //   let value = Math.floor((Math.random() * 100) + 1);
  //   let type: string;

  //   if (value < 20) {
  //     type = 'success';
  //   } else if (value < 40) {
  //     type = 'info';
  //   } else if (value < 60) {
  //     type = 'warning';
  //   } else {
  //     type = 'danger';
  //   }
  //   this.currentValue = value;
  //   this.type = type;
  // };

}