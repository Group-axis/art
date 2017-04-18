import { Component } from '@angular/core';
import { Router, ROUTER_DIRECTIVES} from 'angular2/router';
import { CORE_DIRECTIVES, FORM_DIRECTIVES, FORM_PROVIDERS } from '@angular/common';
import { Http, Headers } from '@angular/http';
import { Auth, Store, Logger } from '../common/components/services';

/*
 * We're loading this component asynchronously
 * We are using some magic with es6-promise-loader that will wrap the module with a Promise
 * see https://github.com/gdi2290/es6-promise-loader for more info
 */

//this.logger.log('`Header` component loaded asynchronously');

@Component({
  selector: 'header',
   directives: [ ROUTER_DIRECTIVES, CORE_DIRECTIVES, FORM_DIRECTIVES ],
   styles: [`
    h1 {
      font-family: Arial, Helvetica, sans-serif
    }
  `],
  template: require('./header.html'),
  providers : [ ]
})
export class Header {
  private loggedUser = "";
  
  constructor(private router: Router, private http: Http, private auth : Auth, private logger : Logger) {
    this.logger.log("Header constructor ");
  }

  ngAfterContentChecked() {    
    let retrievedUser = this.auth.getUser();
    this.loggedUser = retrievedUser ? retrievedUser.getLoggedName() : ""; 
  }

  ngOnInit() {
    this.logger.log('hello `Header` component');
  }

  asyncDataWithWebpack() {
    
  }

  logout() {
    this.logger.log(" logging out user "+ this.auth.getUser());
    this.auth.removeUser();
    this.logger.log("re-directing to login page");
    this.router.navigateByUrl('/');

  }

  }
