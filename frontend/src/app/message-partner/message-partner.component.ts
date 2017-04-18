import { Component } from '@angular/core';
import { ROUTER_DIRECTIVES, RouteConfig, RouteParams } from 'angular2/router';

@Component({
  selector: 'message-partner',
  directives: [ROUTER_DIRECTIVES],
  template: `
  <router-outlet></router-outlet>
  `
})
@RouteConfig([
  { path: '/', name: 'MessageHome', loader: () => require('es6-promise!./message-home')('MessageHome'), useAsDefault: true },
  { path: '/partner-overview', name: 'PartnerOverview', loader: () => require('es6-promise!./partner-overview')('PartnerOverview') },
  { path: '/routing-keyword', name: 'RoutingKeyword', loader: () => require('es6-promise!./routing-keyword')('RoutingKeyword') },
  { path: '/rule-overview', name: 'RuleOverview', loader: () => require('es6-promise!./rule-overview')('RuleOverview') }
])

export class MessagePartner {

  constructor() { }

  ngOnInit() {
  }

}
