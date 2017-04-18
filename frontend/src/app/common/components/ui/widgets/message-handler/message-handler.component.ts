import { Component, ViewChild, Input, Output, EventEmitter, OnChanges, SimpleChange } from 'angular2/core';
import { NgClass, CORE_DIRECTIVES, Control } from '@angular/common';
import { DataTable, DataTableDirectives } from 'angular2-datatable/datatable';
import { Alert, Modal } from '../modal';
import { AMHRoutingService } from "../../../../../amh-routing/amh-routing.service";
import { LimitPipe } from '../../controls';
import { Message } from '../../../../../models/simulation';
import { Auth, Logger } from '../../../services';
import { SingleMessageModalObjectMetadata, SingleMessageModalComponent } from '../single-message-modal';
import { GroupMessageModalObjectMetadata, GroupMessageModalComponent } from '../group-message-modal';

//this.logger.log('`MessageHandler` component loaded asynchronously');

@Component({
  selector: 'message-handler',
  styles: [`
    h1 {
      font-family: Arial, Helvetica, sans-serif
    }
  `],
  template: require('./message-handler.html'),
  directives: [CORE_DIRECTIVES, NgClass, Alert, Modal, DataTableDirectives],
  providers: [Auth, AMHRoutingService],
  pipes: [LimitPipe]
})
export class MessageHandlerComponent implements OnChanges {
  @ViewChild(Alert) alert;
  @ViewChild(Modal) modal;
  @ViewChild(DataTable) table;
  @Output() public messasgeSelectedChange: EventEmitter<Array<Message>> = new EventEmitter<Array<Message>>();
  @Input("selected-messages") public inputMessagesSelected: Array<Message>;

  private messages: Array<Message> = [];
  private originalMessages: Array<Message> = [];
  // private groupMessagesIdMap: Map<string, Array<string>> = new Map<string, Array<string>>();
  // private groupMessagesContentMap: Map<string, Array<string>> = new Map<string, Array<string>>();
  private totalSelectedMessages: number = 0;

  private messageToDelete: Message;
  private selectedMessages: Array<Message> = [];
  private selectAllMessages: boolean = false;
  private messageTextInput: Control;
  // private messageTextFilter: string;
  constructor(private amhRoutingService: AMHRoutingService, private auth: Auth, private logger: Logger) {

    // this.messageTextFilter = "";
    this.messageTextInput = new Control("");
    let tempText = "";
    this.messageTextInput.valueChanges
      .debounceTime(50)
      .switchMap(filterText => {
        this.actionMultiSelection(false);
        this.updateTotalSelectedMessages();
        this.table.setPage(1, this.table.getPage().rowsOnPage);
        tempText = filterText;
        return this.amhRoutingService.findMessageMatches(filterText);
      })
      .subscribe(
      data => {
        this.messages = this.changeFilter(data, { filtering: { filterString: tempText, columnName: "name" } });
        this.messages.filter(msg => msg.group != undefined && msg.group.length > 0)
          .forEach(element => {
            let found = this.originalMessages.find(mm => mm.name == element.name);
            if (found) element.messages = found.messages;
          });
        this.logger.debug("response from findMessageMatches with text " + tempText + " size " + this.messages.length);
      },
      err =>
        this.logger.log("Can't get messages. Error code: %s, URL: %s ", err.status, err.url),
      () => {
        this.logger.debug(this.messages.length + ' message(s) are retrieved from ES with text ' + tempText);
      }
      );

  }

  /* SimpleChange
        previousValue: any;
        currentValue: any;
  */
  ngOnChanges(changes: { [propertyName: string]: SimpleChange }) {
    let inputMessages : SimpleChange = changes["inputMessagesSelected"];
    if (inputMessages) {
      this.logger.debug("inputMessagesSelected has changed "+JSON.stringify(inputMessages.currentValue.map(m=>{return m.name})));
      this.selectedMessages = inputMessages.currentValue;
      this.updateTotalSelectedMessages();
      this.selectedMessages.forEach(message => {
        let messageFound = this.messages.find(m => { return m.id == message.id});
        if (messageFound) messageFound.selected = true;
      });
    }
  }

  ngOnInit() {
    this.logger.log('onInit `Message Handler` component');
    this.loadMessages();
  }

  private loadMessages() {
    setTimeout(() => this.loadMessagesFromES(), 700); //wait for ES to be refreshed
  }

  private loadMessagesFromES() {
    this.originalMessages = [];
    this.totalSelectedMessages = 0;
    this.selectedMessages = [];
    this.messasgeSelectedChange.emit(this.selectedMessages);
    this.selectAllMessages = false;
    // Get the data from the server
    this.amhRoutingService.findMessages()
      .subscribe(
      message => {
        //this.logger.debug("adding a message into originalMessages, which has a size of  "+this.originalMessages.length);
        //  this.logger.debug("adding a message into the list name: "+message.name+" param "+ JSON.stringify(message.itemMap));
        this.originalMessages.push(message);

        // let ids = message.group ? [] : [message.id];
        // this.groupMessagesIdMap.set(message.id, ids);

      },
      err => this.logger.error("error in loading messages " + err.message),
      () => {
        this.logger.debug(this.originalMessages.length + " message(s) loaded.");
        this.messages = this.originalMessages;
      }
      );
  }


  private loadGroupMessages() {

    this.amhRoutingService.loadGroupMessages()
      .subscribe(
      resp => {
        if (resp.found) {
          //  console.debug("found resp OK " + resp.messages.size);
          resp.messages.forEach((messages, groupName) => {
            //    console.debug("Looking for group name "+ groupName);
            let groupMessage = this.originalMessages.find(m => m.name == groupName)
            if (groupMessage) {
              //    console.debug( groupName + " Found and adding messages");
              groupMessage.messages = messages
            } else {
              // console.debug( groupName + " Not Found :/");
            }
          })
        } else {
          //console.debug("No group messages found");
        }
      })

  }


  modalOpen(message?: Message) {
    let params = new Map<string, string>();
    if (message) {
      this.modal.modalTitle = "Edit Message";
      params = params.set('messageId', String(message.id));
    } else {
      this.modal.modalTitle = "Create Message";
      params = params.set('messageId', '');
    }

    this.modal.parameters = params;
    this.modal.createMetadata(SingleMessageModalObjectMetadata);
    this.modal.open(SingleMessageModalComponent);
  }

  modalImportOpen() {
    this.modal.modalTitle = "Import Message";
    this.modal.parameters = new Map<string, string>();
    this.modal.createMetadata(GroupMessageModalObjectMetadata);
    this.modal.open(GroupMessageModalComponent);
  }

  getData(childModalComponent) {
    this.logger.debug(" from modal get data calling loadMessages...");
    this.loadMessages();
  }

  closeAlert() {
    setTimeout(() => this.alert.cancel(), 1000);
    //setTimeout(() => this.router.parent.navigate(["AMHHome"]), 1200);
  }

  private messagesSelectionUpdate(value: boolean) {
    this.selectedMessages = this.messages
      .map(message => { message.selected = value; return message; })
      .filter(message => value);
    this.messasgeSelectedChange.emit(this.selectedMessages);
  }

  private updateTotalSelectedMessages() {
    this.totalSelectedMessages =
      this.selectedMessages
        .reduce((acc, msg) => {
          let toAdd = msg.groupCount ? msg.groupCount : 1;
          return acc + toAdd;
        }, 0);

    let totalMessages = this.messages
      .reduce((acc, msg) => {
        let toAdd = msg.groupCount ? msg.groupCount : 1;
        return acc + toAdd;
      }, 0);

    this.selectAllMessages = totalMessages > 0 && totalMessages == this.totalSelectedMessages;
  }

  actionMultiSelection(selected) {
    this.logger.debug("select all messages " + selected);
    this.messagesSelectionUpdate(selected);
    this.updateTotalSelectedMessages();
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

  alertResponse(resp) {
    switch (resp) {
      case 0: //Delete message cancel
        break;
      case 1: //Delete message Yes
        this.deleteMessage();
        break;
    }
  }

  actionDelete(message: Message) {
    this.logger.debug("deleting message id " + message.id);
    this.messageToDelete = message;
    this.alertYesCancel(1, 0, "Message will be deleted, Do you want to continue?")
  }

  private deleteMessage() {
    if (!this.messageToDelete) {
      this.logger.log("No message defined to be deleted");
      return;
    }
    // let ids = this.groupMessagesIdMap.get(this.messageToDelete.id);
    let ids = this.messageToDelete.group ? [] : [this.messageToDelete.id];
    let groupId = this.messageToDelete.group ? this.messageToDelete.id : undefined;

    let nextPage = 1;
    let pageEvent = this.table.getPage();
    let rowOnPage = pageEvent.rowsOnPage;

    this.logger.log("total of messasges = " + pageEvent.dataLength + " per row : " + rowOnPage);
    if (pageEvent.activePage > 1) {
      let inLastPage = pageEvent.dataLength % rowOnPage;
      let currentPage = Math.floor(pageEvent.dataLength / rowOnPage) + 1;
      nextPage = inLastPage == 1 ? currentPage - 1 : currentPage;
    }
    this.logger.log("nextPage = " + nextPage + " rowOnPage: " + rowOnPage);
    this.table.setPage(nextPage, rowOnPage);

    this.amhRoutingService.deleteMessages(ids, this.auth.getUser(), groupId)
      .subscribe(
      succ => {
        this.logger.log(" deletion SUCCESS!!");
        this.messageToDelete = undefined;
      },
      error => {
        this.logger.error(" Error on delete message " + error.message);
        this.messageToDelete = undefined;
      },
      () => {
        this.logger.log(" delete done!");
        this.loadMessages();
      });
  }



  actionEdit(message: Message) {
    if (message.group) {
      return;
    }
    this.modalOpen(message);
  }

  actionNewMessage() {
    this.modalOpen();
  }

  actionMessageSelection(message: Message, selected) {
    this.logger.debug(" message name " + message.name+" selected " + selected);
    message.selected = selected;
    if (selected) {
      this.selectedMessages.push(message);
    } else {
      this.deleteFromArray(this.selectedMessages, (m) => message.id == m.id);
    }
    this.messasgeSelectedChange.emit(this.selectedMessages);
    this.updateTotalSelectedMessages();
  }

  private changeFilter(data: any, config: any): any {
    if (!config.filtering) {
      return data;
    }

    let valueToFind = config.filtering.filterString.toUpperCase();
    return data.filter((item: any) =>
      item[config.filtering.columnName].toUpperCase().match(valueToFind));
  }

  private deleteFromArray(array: Array<any>, predicate: (v) => boolean) {
    let index = array.findIndex(predicate);
    this.logger.log("index found " + index);
    if (index > -1) {
      array.splice(index, 1);
    }
  }

}