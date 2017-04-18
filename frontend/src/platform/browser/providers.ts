/*
 * These are globally available services in any component or any other service
 */

// Angular 2
import { FORM_PROVIDERS, HashLocationStrategy, LocationStrategy } from '@angular/common';
// Angular 2 Http
import { HTTP_PROVIDERS } from '@angular/http';
// Angular 2 Router
import { ROUTER_PROVIDERS } from '@angular/router-deprecated';

export const pegjs = require('pegjs');
//require('pegjs-require');
/*
* Application Providers/Directives/Pipes
* providers/directives/pipes that only live in our browser environment
*/
//export const RuleValidationParser = require('../../assets/pegjs-files/AMHRuleGrammar.pegjs');
//export const RuleEvaluationParser = require('../../assets/pegjs-files/AMHRuleGrammarEvaluation.pegjs');

export const APPLICATION_PROVIDERS = [
  ...FORM_PROVIDERS,
  ...HTTP_PROVIDERS,
  ...ROUTER_PROVIDERS,
  {provide: LocationStrategy, useClass: HashLocationStrategy }
];

export const PROVIDERS = [
  ...APPLICATION_PROVIDERS
];
