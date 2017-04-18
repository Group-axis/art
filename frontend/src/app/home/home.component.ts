import {Component} from 'angular2/core';
import {AppState} from '../app.service';
import {AlertComponent} from 'ng2-bootstrap';
import {MenuConfig} from '../models/menu';
import {HeaderSecondary} from '../header-secondary';
import {Permissions} from '../common/directives';
import {Logger} from "../common/components/services";

import {Title} from './title';
import {XLarge} from './x-large';

@Component({
  selector: 'home',  // <home></home>
  // We need to tell Angular's Dependency Injection which providers are in our app.
  providers: [
    Title
  ],
  // We need to tell Angular's compiler which directives are in our template.
  // Doing so will allow Angular to attach our behavior to an element
  directives: [
    XLarge, AlertComponent, HeaderSecondary, Permissions
  ],
  // We need to tell Angular's compiler which custom pipes are in our template.
  pipes: [ ],
  // Our list of styles in our component. We may add more to compose many styles together
  styles: [ require('./home.css') ],
  // Every Angular template is first compiled by the browser before Angular runs it's compiler
  template: require('./dashboard.html')
})
export class Home {
  private menuConfig : Array<MenuConfig> = [
    new MenuConfig("fa fa-home","/home","Home"),
    new MenuConfig("fa fa-laptop","","Dashboard")];

  public alerts:Array<Object> = [
    {
      type: 'danger',
      msg: 'Oh snap! Change a few things up and try submitting again.'
    },
    {
      type: 'success',
      msg: 'Well done! You successfully read this important alert message.',
      closable: true
    }
  ];

  public closeAlert(i:number):void {
    this.alerts.splice(i, 1);
  }

  public addAlert():void {
    this.alerts.push({msg: 'Another alert!', type: 'warning', closable: true});
  }  
    
  // Set our default values
  localState = { value: '' };
  // TypeScript public modifiers
  constructor(public appState: AppState, public title: Title, private logger : Logger) {

  }

  ngOnInit() {
    this.logger.log('hello `Home` component');
    // this.title.getData().subscribe(data => this.data = data);
  }

  submitState(value) {
    this.logger.log('submitState', value);
    this.appState.set('value', value);
  }

}
