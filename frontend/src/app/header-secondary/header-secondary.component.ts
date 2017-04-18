import { Component, Input, Output, EventEmitter } from '@angular/core';
import { Router, ROUTER_DIRECTIVES} from 'angular2/router';
import { CORE_DIRECTIVES } from '@angular/common';
import { Http, Headers } from '@angular/http';
import { MenuConfig } from '../models/menu';
import {AlertComponent} from 'ng2-bootstrap/components/alert';
import {Logger} from '../common/components/services';

//Logger.log('`Menu Header` component loaded asynchronously');

@Component({
  selector: 'menu-header',
   directives: [ ROUTER_DIRECTIVES, CORE_DIRECTIVES, AlertComponent ],
   template: require('./header-secondary.html'),
   styles: [`
    .alert-danger {
      opacity: 0.7;
    }
  `]
})
export class HeaderSecondary {
  @Input("header-menu")
  private menu : Array<MenuConfig>;
  @Input("header-navigate")
  private navigateOnClick : boolean = true;
  @Input("header-alert")
  private alert : any;

  @Output("navigate")
  private navigate : EventEmitter<string> = new EventEmitter<string>();

  
  constructor(private router: Router, private http: Http,private logger:Logger) { 

  }

  ngOnInit() {
    this.logger.log('hello `menu-Header` component');
  }

  asyncDataWithWebpack() {
  }

  routeTo(link : string) {
    if (this.navigateOnClick) {
      this.logger.debug("going to "+ link);
      this.router.navigateByUrl(link);
    } else {
      this.navigate.emit(link);
    }
  }

  closeAlert() {
    this.alert = undefined;
  }

  }
