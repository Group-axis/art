import {Component, OnInit} from 'angular2/core';
import { Validators, ControlGroup, FormBuilder, CORE_DIRECTIVES, FORM_DIRECTIVES, FORM_PROVIDERS } from '@angular/common';
import { ACCORDION_DIRECTIVES } from 'ng2-bootstrap';
import { RouteParams} from 'angular2/router';
import {FormFieldComponentExample, BootstrapFormDirective, BootstrapInputDirective, LabelsComponent, LabelsValueAccessor, notEmpty} from '../../common/components/ui/controls';
import {Rule} from '../../models/routing';

console.log('`SibesRoutingRule` component loaded asynchronously');

@Component({
  selector: 'sibes-routing-rule',
  styles: [`
    h1 {
      font-family: Arial, Helvetica, sans-serif
    }
  `],
  template: require('./sibes-routing-rule.html'),
  directives: [ACCORDION_DIRECTIVES, CORE_DIRECTIVES, FORM_DIRECTIVES, 
                FormFieldComponentExample, BootstrapFormDirective, BootstrapInputDirective, LabelsComponent, LabelsValueAccessor],
  providers: [FORM_PROVIDERS]
})
export class SibesRoutingRule {
    companyForm : ControlGroup;
    descriptionForm : ControlGroup;
    conditionForm : ControlGroup;
    actionForm : ControlGroup;
    rule : Rule;
    
  constructor(routeParams: RouteParams, fb : FormBuilder) {
      this.company = new Company('id', 'some name', ['IT','Computers','Maintenance','Software']);
      this.rule = this.getRule(routeParams.params['sequence']);
      
      this.companyForm = fb.group({
          'name':['', Validators.required],
          'tags':['', notEmpty]
      });
  }

  ngOnInit() {
    console.log('hello `Sibes  Routing Rule` component');
    // static data that is bundled
    // var mockData = require('assets/mock-data/mock-data.json');
    // console.log('mockData', mockData);
    // if you're working with mock data you can also use http.get('assets/mock-data/mock-data.json')
    // this.asyncDataWithWebpack();
  }
  asyncDataWithWebpack() {
    // you can also async load mock data with 'es6-promise-loader'
    // you would do this if you don't want the mock-data bundled
    // remember that 'es6-promise-loader' is a promise
    // var asyncMockDataPromiseFactory = require('es6-promise!assets/mock-data/mock-data.json');
    // setTimeout(() => {
    //
    //   let asyncDataPromise = asyncMockDataPromiseFactory();
    //   asyncDataPromise.then(json => {
    //     console.log('async mockData', json);
    //   });
    //
    // });
  }
  
  private getRule(sequence:string) {
      let rule : Rule;
      let sequenceNumber = -1;
      
      if (sequence) {
          sequenceNumber = +sequence;
          //load rule from service by sequence
          //rule = ruleService.getBySequence(sequenceNumber);
          return rule;
      }
      
      return Rule.empty();
  }
  
  public company : Company; 

 public oneAtATime:boolean = true;
  public items:Array<string> = ['Item 1', 'Item 2', 'Item 3'];

  public status:Object = {
    isFirstOpen: true,
    isFirstDisabled: false,
    open: true
  };

  public groups:Array<any> = [
    {
      title: 'Dynamic Group Header - 1',
      content: 'Dynamic Group Body - 1'
    },
    {
      title: 'Dynamic Group Header - 2',
      content: 'Dynamic Group Body - 2'
    }
  ];

  public addItem():void {
    this.items.push(`Items ${this.items.length + 1}`);
  }

}

export class Company {
    constructor(public id: string, public name: string, public tags: Array<string>) { }
}
