import {Component, ViewEncapsulation, ComponentRef, DynamicComponentLoader,ElementRef, Input, EventEmitter, Output} from 'angular2/core';
import {NgIf} from '@angular/common';
import {Open} from './open.component';
import {Logger} from "../../../services";

@Component({
  selector: 'alert',
  template: `
  <div class="modal fade" [open]="!isOpen" id="myModal" tabindex="-1" role="dialog" aria-labelledby="myModalLabel">
    <div class="modal-dialog" role="document">
      <div class="modal-content">
        <div class="modal-header" [hidden]=!alertHeader>
          <button type="button" class="close" data-dismiss="modal" (click)='cancel()' aria-label="Close"><span aria-hidden="true">&times;</span></button>
          <h4 class="modal-title text-center" id="myModalLabel">{{alertTitle}}</h4>
        </div>
        <div class="modal-body">
          <div [hidden]=!alertMessage>
            <i *ngIf="waitIcon" class="fa fa-spinner" aria-hidden="true" style="font-size:2.1em;"></i>
            &nbsp;&nbsp;{{message}}
          </div>
        </div>
        <div class="modal-footer" [hidden]=!alertFooter>
          <span [hidden]=!yesButton >
            <button class="btn btn-primary" (click)="yes()">{{yesButtonText}}</button>
          </span>
          <span [hidden]=!okButton >
            <button class="btn btn-primary" (click)="ok()">{{okButtonText}}</button>
          </span>
          <span [hidden]=!cancelButton>
            <button class="btn btn-primary" (click)="cancel()">{{cancelButtonText}}</button>
          </span>
        </div>
      </div>
    </div>
  </div>
  `,
  providers: [],
  directives: [Open],
  encapsulation: ViewEncapsulation.None,
  pipes: []
})
/**
  * API to an open alert window.
  */
export class Alert{
  /**
     * Caption for the title.
     */
  public alertTitle:string;
  /**
     * Describes if the alert contains Ok Button.
     * The default Ok button will close the alert and emit the callback.
     * Defaults to true.
     */
  public okButton:boolean = true;
  /**
     * Describes if the alert contains a waiting Icon.
     * Defaults to true.
     */
  public waitIcon:boolean = true;
  /**
     * Caption for the OK button.
     * Default: Ok
     */
  public okButtonText:string= 'Ok';

  public okButtonResponse:number = 2;
  /**
     * Describes if the alert contains cancel Button.
     * The default Cancelbutton will close the alert.
     * Defaults to true.
     */
  public cancelButton:boolean = true;
  /**
     * Caption for the Cancel button.
     * Default: Cancel
     */
  public cancelButtonText:string = 'Cancel';
  public cancelButtonResponse:number = 0;
  /**
     * Describes if the alert contains yes Button.
     * The default Yesbutton will close the alert.
     * Defaults to false.
     */
  public yesButton:boolean = false;
  /**
     * Caption for the Cancel button.
     * Default: Cancel
     */
  public yesButtonText:string = 'Yes';
  public yesButtonResponse:number = 1;
  /**
     * if the alertMessage is true it will show the contentString inside alert body.
     */
  public alertMessage:boolean = true;
  /**
     * Some message/content can be set in message which will be shown in alert body.
     */
  public message:string;
  /**
    * if the value is true alert footer will be visible or else it will be hidden.
    */
  public alertFooter:boolean= true;
  /**
    * shows alert header if the value is true.
    */
  public alertHeader:boolean = true;
  /**
    * if the value is true alert will be visible or else it will be hidden.
    */
  public isOpen:boolean=false;
  /**
    * Emitted when a ok button was clicked
    * or when Ok method is called.
    */
  @Output() public alertOutput:EventEmitter<any> = new EventEmitter();

  constructor(public dcl:DynamicComponentLoader, public _elementRef: ElementRef, private logger : Logger){}
  /**
       * Opens a alert window creating backdrop.
       */
  open(){
    this.isOpen= true;
  }
  
  yes(){
    this.isOpen = false;
    this.alertOutput.emit(this.yesButtonResponse);
  }

  /**
     *  ok method closes the modal and emits modalOutput.
     */
  ok(){
    this.isOpen = false;
    this.alertOutput.emit(this.okButtonResponse);
  }
  /**
     *  cancel method closes the moda.
     */
  cancel(){
    this.isOpen = false;
    this.alertOutput.emit(this.cancelButtonResponse);
  }
}
