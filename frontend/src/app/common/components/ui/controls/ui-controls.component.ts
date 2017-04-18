import { Injectable, Provider, ContentChild, Host, Component, Directive, ElementRef, Renderer, Input, Output, EventEmitter, provide, Pipe, PipeTransform} from '@angular/core';
import { NG_VALUE_ACCESSOR, ControlValueAccessor, CORE_DIRECTIVES, NgFormControl, NgForm, NG_VALIDATORS} from '@angular/common';
import { Logger } from '../../services';
import {forwardRef} from '@angular/core/src/di';

function validateEmail(emailControl) {
  if (!emailControl.value || /^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+$/.test(emailControl.value)) {
    return null;
  } else {
    return { 'invalidEmail': true };
  }
}


/*
 * Usage:
 *   value | profileLabel:mappingValues
 * Example:
 *   {{ 2 |  profileLabel:"{1:"Admin",2:"Support"}"}}
 *   formats to: "Support"
*/
@Pipe({name: 'profileLabel'})
export class ProfileLabelPipe implements PipeTransform {
  transform(value: Array<number>, map: Map<number,string>): Array<string> {
    return value
        .map(id => " "+map.get(id))
        .filter(res => res.indexOf("undefined") == -1);
  }
}

/*
 * Usage:
 *   value | limit:maxCharacters
 * Example:
 *   {{ "luna park" |  limit:4}}
 *   formats to: "luna"
*/
@Pipe({name: 'limit'})
export class LimitPipe implements PipeTransform {
  transform(value: string, limit: number): string {
    if (!value) {
      return "";
    }
    return value.length >= limit ? value.substring(0, limit) : value;
  }
}

/*
 * Usage:
 *   value | numberFormat:formatType
 * Example:
 *   {{ 0 |  numberFormat}}
 *   formats to: "" <- EMPTY STRING
*/
@Pipe({name: 'numberFormat'})
export class NumberFormatPipe implements PipeTransform {
  transform(value: number, format?: number): string {
    if (!value) {
      return "";
    }
    //TODO: use the format parameter to select type of format
    return value == 0 ? "" : String(value);
  }
}

@Directive({
  selector: '[email-input]',
  providers: [provide(NG_VALIDATORS, { useValue: validateEmail, multi: true })]
})
export class EmailValidator {}

@Component({
  template: `<div>{{currentError}}</div>`,
  selector: 'gp-control-errors',
  inputs: ['control', 'errors']
})
export class GPControlErrors {
  errors: Object;
  control: string;
  constructor(@Host() private formDir: NgForm) {}
  get currentError() {
    let control = this.formDir.controls[this.control];
    let errorMessages = [];
    if (control && control.touched) {
      errorMessages = Object.keys(this.errors)
        .map(k => control.hasError(k) ? this.errors[k] : null)
        .filter(error => !!error);
    }
    return errorMessages.pop();
  }
}



@Directive({
    selector: 'input:not([noBootstrap]), textarea:not([noBootstrap])'
})
export class BootstrapInputDirective {
  constructor(el: ElementRef, renderer: Renderer) {
    renderer.setElementClass(el.nativeElement, 'form-control', true);
  }
};

@Directive({
  selector: 'form:not([noBootstrap])'
})
export class BootstrapFormDirective {
  constructor(el: ElementRef, renderer: Renderer) {
    renderer.setElementClass(el.nativeElement, 'form-validate', true);
    renderer.setElementClass(el.nativeElement, 'form-horizontal', true);
    
  }
};

const CUSTOM_VALUE_ACCESSOR = new Provider(NG_VALUE_ACCESSOR, {useExisting: forwardRef(() => LabelsValueAccessor), multi: true});

export function notEmpty(control) {
  if(control.value == null || control.value.length===0) {
    return {
      notEmpty: true
    }
  }

  return null;
}

@Component({
  selector: 'field',
  template: require('./ui-field.html'),
  directives:[CORE_DIRECTIVES],
  styles: [
    `.glyphicon-refresh-animate {
      -animation: spin .7s infinite linear;
      -webkit-animation: spin2 .7s infinite linear;
    }`,
    `@-webkit-keyframes spin2 {
       from { -webkit-transform: rotate(0deg);}
      to { -webkit-transform: rotate(360deg);}
    }`
  ]
})
export class FormFieldComponentExample {
  @Input()
  label: string;

  @Input()
  feedback: boolean;

  @ContentChild(NgFormControl) state;

  constructor(private eltRef:ElementRef) {

  }

  isStateNotValid() {
    return this.label && this.state && !this.state.valid
       && !this.state.control.pending;
  }

  isFeedbackValid() {
    return this.state && this.feedback &&
       !this.state.control.pending && this.state.valid;
  }

  isFeedbackNotValid() {
    return this.state && this.feedback &&
       !this.state.control.pending && !this.state.valid;
  }

  isFeedbackPending() {
    return this.state && this.feedback && this.state.control.pending;
  }
};

@Component({ 			
  selector: 'labels',
  template: `
    <div *ngIf="values" >
      <span *ngFor="#value of values" style="font-size:14px"
          class="label label-default" (click)="removeValue(value)">
        {{value}} <span class="fa fa-times" aria-hidden="true"></span>
      </span>
      <span> | </span>
      <span style="display:inline-block;">
        <input [(ngModel)]="valueToAdd" style="width: 50px; font-size: 14px;" class="custom"/>
        <em class="fa fa-arrow-circle-left" aria-hidden="true" (click)="addValue(valueToAdd)"></em>
      </span>
    </div>
  ` 			
}) 			
export class LabelsComponent  {
   			
  @Input() 			
  values:string[]; 			

  @Output() 			
  totoChange: EventEmitter<string[]>; 			

  valueToAdd : string;
  
  constructor(private logger : Logger) {
    this.totoChange = new EventEmitter<string[]>(); 			
  } 			

 removeValue(label:string) {
    var index = this.values.indexOf(label, 0);
    if (index != undefined) {
      this.values.splice(index, 1);
      this.totoChange.emit(this.values);
    }
  }

  addValue(value:string) {
    this.values.push(this.valueToAdd);
    this.totoChange.emit(this.values);
    this.valueToAdd = '';
  }
  
  writeLabelsValue(labels:string[]) {
    this.values = labels;
    this.logger.log("cv " + this.values);
  }
  
}


@Directive({
  selector: 'labels',
  host: {'(totoChange)': 'onChange($event)'},
  providers: [CUSTOM_VALUE_ACCESSOR]
})
export class LabelsValueAccessor implements ControlValueAccessor {
  onChange = (_) => {};
  onTouched = () => {};
      
  constructor(private host: LabelsComponent, private logger : Logger) {

  }

  writeValue(value: any): void {
      this.logger.log("writing...");
    this.host.writeLabelsValue(value);
  }

  registerOnChange(fn: (_: any) => void): void { this.onChange = fn; }
  registerOnTouched(fn: () => void): void { this.onTouched = fn; }
}