import {  ContentChild, Component,  ElementRef, Input } from 'angular2/core';
import { CORE_DIRECTIVES, NgFormControl } from '@angular/common';

@Component({
  selector: 'gp-field',
  template: require('./form-field.html'),
  directives:[CORE_DIRECTIVES],
  styles: [
    `.glyphicon-refresh-animate {
      -animation: spin .7s infinite linear;
      -webkit-animation: spin2 .7s infinite linear;
    }`,
    `@-webkit-keyframes spin2 {
       from { -webkit-transform: rotate(0deg);}
      to { -webkit-transform: rotate(360deg);}
    }`,`
    .vertical-center {
      border:solid 0px #055;
      display: flex;
      justify-content:flex-end;
      align-items: center;
    }`
  ]
})
export class FormFieldComponent {
  @Input()
  label: string;

  @Input()
  heightLabel:number = 34; //default
  
  @Input()
  required: boolean;
  
  @Input()
  touched: boolean = false;
  
  @Input()
  feedback: boolean;
  
  centerHeight: boolean = true;

  @ContentChild(NgFormControl) state;

  constructor(private eltRef:ElementRef) {

  }

  isStateNotValid() {
      if (this.touched) {
         return this.required && this.state && !this.state.valid && 
         this.state.touched && !this.state.control.pending; 
      }
      return this.required && this.state && !this.state.valid && !this.state.control.pending;
  }

  isFeedbackValid() {
    return this.required && this.state && this.feedback &&
       !this.state.control.pending && this.state.valid;
  }

  isFeedbackNotValid() {
    return this.required && this.state && this.feedback &&
       !this.state.control.pending && !this.state.valid;
  }

  isFeedbackPending() {
    return this.state && this.feedback && this.state.control.pending;
  }
}