/*
 * Angular 2 decorators and services
 */
import {Component, ViewEncapsulation} from '@angular/core';
import {RouteConfig, Router} from 'angular2/router';
import { CustomRouterOutlet } from './common/directives';
import {Home} from './home';
import {Login} from './login';
import {Header} from './header';
import {AppState} from './app.service';
import {RouterActive} from './router-active';
import {enableProdMode} from "@angular/core"; enableProdMode();

//declare var jQuery:JQueryStatic;

/*
 * App Component
 * Top Level Component
 */
//, => <router-outlet></router-outlet>
@Component({
  selector: 'app',
  pipes: [ ],
  providers: [ ],
  directives: [ RouterActive, CustomRouterOutlet, Header ],
  encapsulation: ViewEncapsulation.None,
  styles: [`
    body {
      margin: 0;
    }
    md-toolbar ul {
      display: inline;
      list-style-type: none;
      margin: 0;
      padding: 0;
      width: 60px;
    }
    md-toolbar li {
      display: inline;
    }
    md-toolbar li.active {
      background-color: lightgray;
    }
  `],
  template: `
    <header></header>
    <custom-router-outlet></custom-router-outlet>
    <footer></footer>
  `
})
@RouteConfig([
  { path: '/',      name: 'Login', component: Login, useAsDefault: true },
  { path: '/home',  name: 'Home',  component: Home },
  // Async load a component using Webpack's require with es6-promise-loader and webpack `require`
  { path: '/about', name: 'About', loader: () => require('es6-promise!./about')('About') },
  { path: '/sibes-routing/...', name: 'SibesRouting', loader: () => require('es6-promise!./sibes-routing')('SibesRouting') },
  { path: '/saa-routing/...', name: 'SAARouting', loader: () => require('es6-promise!./saa-routing')('SAARouting') },
  { path: '/amh-routing/...', name: 'AMHRouting', loader: () => require('es6-promise!./amh-routing')('AMHRouting') },
  { path: '/message-partner/...', name: 'MessagePartner', loader: () => require('es6-promise!./message-partner')('MessagePartner') },
  { path: '/user-admin/...', name: 'UserAdministration', loader: () => require('es6-promise!./user-admin')('UserAdmin') }
])
export class App {
  angularclassLogo = 'assets/img/angularclass-avatar.png';
  name = 'Group Suite';
  url = 'https://twitter.com/AngularClass';
  display = true;
  constructor(public appState: AppState) {} //, public rr: Router

  ngOnInit() {
  //  console.log('Initial App State', this.appState.state);
  }

}

/*
 * Please review the https://github.com/AngularClass/angular2-examples/ repo for
 * more angular app examples that you may copy/paste
 * (The examples may not be updated as quickly. Please open an issue on github for us to update it)
 * For help or questions please contact us at @AngularClass on twitter
 * or our chat on Slack at https://AngularClass.com/slack-join
 */
