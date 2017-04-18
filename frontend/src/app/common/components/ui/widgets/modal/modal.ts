import {Type, ComponentMetadata, Component, ViewEncapsulation, ComponentRef, DynamicComponentLoader,ElementRef, Input, EventEmitter, Output} from 'angular2/core';
import {Open} from './open.component';
import {DynamicHTMLOutlet} from '../dynamic-component';
import {Logger} from "../../../services";

@Component({
  selector: 'modal',
  template: `
  <div class="modal fade" [open]="!isOpen" id="myModal" [attr.data-keyboard]="true" [attr.data-backdrop]="false" tabindex="-1" role="dialog" aria-labelledby="myModalLabel">
    <div class="modal-dialog" role="document" style="width:80%;">
      <div class="modal-content">
        <div class="modal-header" [hidden]=!modalHeader>
          <button type="button" class="close" data-dismiss="modal" (click)='close()' aria-label="Close"><span aria-hidden="true">&times;</span></button>
          <h4 class="modal-title text-center" id="myModalLabel">{{modalTitle}}</h4>
        </div>
        <dynamic-html-outlet (componentCreation)="saveComponent($event)" [meta]="meta" [childComponent]="modalChildComponent"></dynamic-html-outlet>
      </div>
    </div>
  </div>
`,
//
  providers: [],
  directives: [Open, DynamicHTMLOutlet],
  encapsulation: ViewEncapsulation.None,
  pipes: []
})
/**
  * API to an open modal window.
  */
export class Modal {
  public modalChildComponent : Type;
  public meta : ComponentMetadata;
  public componentCreated : any;
  public parameters : Map<string,string>;
  private doneSender : EventEmitter<any> = new EventEmitter<any>();
  private subscription;

  saveComponent(component) {
    this.logger.debug("component recevied in Modal "+component);
    this.logger.debug(" parameters => "+this.parameters.get("messageId"));
    this.componentCreated = component;
    this.componentCreated._hostElement.component.doneSender = this.doneSender;
    this.componentCreated._hostElement.component.initialize(this.parameters);
    this.subscription = this.doneSender.subscribe(msg => {
      if(msg == 'done') {
          this.submit();
      } else { 
          this.close();
        }
  });
  }

  private childComponentListener(msg : string) {
      if(msg == 'done') 
          this.submit();
        else 
          this.close();
  }

  public createMetadata(parameter : any) {
    this.meta = new ComponentMetadata(parameter);
  }
  /**
     * Caption for the title.
     */
  public modalTitle:string;
  /**
    * component which is to be loaded dynamically.
    */
  public component:any;
  /**
     * Describes if the modal contains Ok Button.
     * The default Ok button will close the modal and emit the callback.
     * Defaults to true.
     */
  public okButton:boolean = true;
  /**
     * Caption for the OK button.
     * Default: Ok
     */
  public okButtonText:string= 'Ok';
  /**
     * Describes if the modal contains cancel Button.
     * The default Cancelbutton will close the modal.
     * Defaults to true.
     */
  public cancelButton:boolean = true;
  /**
     * Caption for the Cancel button.
     * Default: Cancel
     */
  public cancelButtonText:string = 'Cancel';
  /**
     * if the modalMessage is true it will show the message inside modal body.
     */
  public modalMessage:boolean = true;
  /**
     * Some message/content can be set in message which will be shown in modal body.
     */
  public message:string;
  /**
    * if the value is true modal footer will be visible or else it will be hidden.
    */
  public modalFooter:boolean= true;
  /**
    * shows modal header if the value is true.
    */
  public modalHeader:boolean = true;
  /**
    * if the value is true modal will be visible or else it will be hidden.
    */
  public isOpen:boolean=false;
  /**
    * Emitted when a ok button was clicked
    * or when close method is called.
    */
  @Output() public modalOutput:EventEmitter<any> = new EventEmitter();
  constructor(public dcl:DynamicComponentLoader, public _elementRef: ElementRef, private logger : Logger){
  }
  //@ViewChild('target', {read: ViewContainerRef}) target;
  /**
       * Opens a modal window creating backdrop.
       * @param component The angular Component that is to be loaded dynamically(optional).
       */
  open(component?){
    this.isOpen= true;
    this.modalChildComponent  = component;
    //IIRH
    // if(component){
    // this.component = this.dcl.loadIntoLocation(component, this._elementRef, "child");
    // }
  }
  /**
     *  close method dispose the component, closes the modal and optionally emits modalOutput value.
     */
  close(data?:any){
    this.dispose();
    if(data){
      this.modalOutput.emit(data);
    }
  }
  /**
     *  ok method dispose the component, closes the modal and emits true.
     */
  submit(){
    this.modalOutput.emit(this.componentCreated._hostElement.component);
    this.dispose();
  }
  /**
     *  dispose method dispose the loaded component.
     */
  dispose(){
    this.isOpen = false;
    if(this.componentCreated != undefined){
      this.logger.debug("...from modal : destroying child component");
      this.componentCreated.destroy();
      this.logger.debug("...from modal : unsubscribing child component");
      this.subscription.unsubscribe();
      //IIRH
          //  this.component.then((componentRef:ComponentRef) => {
          //  componentRef.dispose();
          // return componentRef;
        //  });
        }
  }

}
 

