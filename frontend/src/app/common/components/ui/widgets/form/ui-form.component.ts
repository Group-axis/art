import {Component} from 'angular2/core';
import {Validators, ControlGroup, FormBuilder, NgControl, FORM_PROVIDERS} from '@angular/common';

@Component({
  selector: 'gp-form',
  template: `<form class="form-validate form-horizontal" 
        [ngFormModel]="formName" >
            <ng-content></ng-content>
        </form>
  `,    
  providers: [FORM_PROVIDERS],
  inputs: ['formName']
})
export class GPForm {
  formName: string;
  form : ControlGroup;
  
  constructor(fb : FormBuilder) {
    this.form = fb.group({
        'controlForName': ['', Validators.required]
    });    
  }
  
  
  
}

