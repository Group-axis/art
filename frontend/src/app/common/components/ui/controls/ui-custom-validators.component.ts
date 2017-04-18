import {Control} from '@angular/common';
import {Http, HTTP_PROVIDERS} from 'angular2/http';
import {Injector, ReflectiveInjector} from 'angular2/core'
import {Observable} from 'rxjs/Rx';
import {ObservableInput} from 'rxjs/Observable';
import { PartialObserver } from 'rxjs/Observer';
import 'rxjs/Rx';
import {AMHRoutingService} from "../../../../amh-routing";
import {UserService} from "../../../../user-admin";
import {Config, Store, Logger} from "../../services";
//import {CHECK_RULE_CODE_ENDPOINT} from './api';

interface IValidator {
}

//TODO: Check how to render the asynchronous checking more generic
function asynchCheck<R>(control: Control, findFunc: (value: any, index: number) => ObservableInput<R>,
  observerOrNext?: PartialObserver<R> | ((value: R) => void), error?: (error: any) => void, complete?: () => void): Observable<IValidator> {
  let injector = ReflectiveInjector.resolveAndCreate([HTTP_PROVIDERS, Config, Store, AMHRoutingService, Logger]);
  let store: Store = injector.get(Store);
  let conf: Config = injector.get(Config);
  let logger: Logger = injector.get(Logger);
  conf.load();
  let http = injector.get(AMHRoutingService);
  let found = false;
  return new Observable((obs: any) => {
    control
      .valueChanges
      .debounceTime(700)
      .switchMap(findFunc) //value => http.findRuleByCode(value)
      .subscribe(observerOrNext, error, complete);
    // Any cleanup logic might go here
    return () => logger.log('disposed asych check')
  });
}

function messageNameCheck<R>(control: Control): Observable<IValidator> {
  let injector = ReflectiveInjector.resolveAndCreate([HTTP_PROVIDERS, Config, Store, AMHRoutingService, Logger]);
  let store: Store = injector.get(Store);
  let conf: Config = injector.get(Config);
  let logger: Logger = injector.get(Logger);
  conf.load();
  let http = injector.get(AMHRoutingService);
  let found = false;
  return new Observable((obs: any) => {
    control
      .valueChanges
      .debounceTime(700)
      .switchMap(name => http.findMessageByName(name))
      .subscribe(data => {
        // console.debug("from validator "+ JSON.stringify(data));
        if (data["found"]) {
          found = true;
          obs.next({ "messageNameDuplicated": true });
          logger.log(" message name found [" + data["message"]["name"] + "] ");
          obs.complete();
        } else {
          logger.log("message name " + control.value + " not found!");
          obs.next(null);
          obs.complete();
        }
      },
      error => {
        logger.error("an error ocurred while looking for massage name " + control.value + " : " + error.message);
        obs.next({ "messageNameDuplicatedError": true });
        obs.complete();
      },
      () => {
        logger.log("findMessageByName done");
        if (!found) {
          logger.log("message name " + control.value + " not found!");
          obs.next(null);
          obs.complete();
        }
      });
    // Any cleanup logic might go here
    return () => logger.log('disposed 2')
  });
}

function checkRule(control: Control, source: string): Observable<IValidator> {
  const CHECK_RULE_CODE_ENDPOINT = "";
  // Manually inject Http
  let injector = ReflectiveInjector.resolveAndCreate([HTTP_PROVIDERS, Config, Store, AMHRoutingService, Logger]);
  let store: Store = injector.get(Store);
  let conf: Config = injector.get(Config);
  let logger: Logger = injector.get(Logger);
  conf.load();
  let http = injector.get(AMHRoutingService);
  logger.log("in checkRule the AMHRoutingService is " + http);
  // Return an observable with null if the
  // rule code doesn't yet exist, or
  // an objet with the rejetion reason if they do
  let found = false;
  return new Observable((obs: any) => {
    control
      .valueChanges
      .debounceTime(700)
      .switchMap(value => http.findRuleByCode(value))
      //   .flatMap(value => http.post(CHECK_RULE_CODE_ENDPOINT, JSON.stringify({ [source]: value })))
      .subscribe(
      data => {
        logger.log(" subscriber received response " + data);
        found = data["code"];
        obs.next(found ? { "ruleCodeDuplicated": true } : null);
        logger.log(" rule code found [" + data["code"] + "] ");
        obs.complete();
      },
      error => {
        //   let message = error.json().message;
        //   let reason;
        //   if (message === 'Email taken') {
        //      reason = 'emailTaken';
        //   }
        logger.log("an error ocurred while looking for rule code " + control.value + " msg " + error.json().message);
        obs.next({ "ruleCodeDuplicatedError": true });
        obs.complete();
      },
      () => {
        logger.log("findRuleByCode done");
        if (!found) {
          logger.log("rule code " + control.value + " not found!");
          obs.next(null);
          obs.complete();
        }
      }
      );
    // Any cleanup logic might go here
    return () => logger.log('disposed 2')
  });
}

function injector(service) {
  // Manually inject Http

  let injector = ReflectiveInjector.resolveAndCreate([HTTP_PROVIDERS, Config, Store, service, Logger]);

  let store: Store = injector.get(Store);
  let conf: Config = injector.get(Config);
  let logger: Logger = injector.get(Logger);
  conf.load();
  return injector.get(service);

}

function checkUsername(control: Control, source: string): Observable<IValidator> {
  const CHECK_RULE_CODE_ENDPOINT = "";
  // Return an observable with null if the
  // rule code doesn't yet exist, or
  // an objet with the rejetion reason if they do
  let http = injector(UserService);
  // console.log("verifiying "+control.value);
  let found = false;
  return new Observable((obs: any) => {
    control
      .valueChanges
      .debounceTime(700)
      .switchMap(value => { 
        //console.debug(" in switchMap " + value); 
        return http.findUserByUsername(value); })
      //   .flatMap(value => http.post(CHECK_RULE_CODE_ENDPOINT, JSON.stringify({ [source]: value })))
      .subscribe(
      data => {
        found = data["found"] && control.value == data["value"].username;
        obs.next(found ? { "usernameDuplicated": true } : null);
        //console.log(" username found [" + data["value"].username + "] ");
        obs.complete();
      },
      error => {
        //console.log("an error ocurred while looking for username " + control.value + " msg " + error.json().message);
        obs.next({ "usernameDuplicatedError": true });
        obs.complete();
      },
      () => {
        //console.log("findUserByUsername done");
        if (!found) {
         // console.log("username " + control.value + " not found!");
          obs.next(null);
          obs.complete();
        }
      }
      );
    // Any cleanup logic might go here
    return () => ""
  });
}
export class CustomValidatorsComponent {

  static emailFormat(control: Control) {
    let pattern: RegExp = /\S+@\S+\.\S+/;
    return pattern.test(control.value) ? null : { "emailFormat": true };
  }

  static ruleCodeDuplication(control: Control) {
    return checkRule(control, "uselessForNow");
  }

  static usernameDuplication(control: Control) {
    return checkUsername(control, "uselessForNow");
  }

  private static valid(pattern: RegExp, value: string, errorObj: any) {
    //^,$ are the begining and the end of the string respectively
    //To test https://regex101.com/#javascript
    return pattern.test(value) ? null : errorObj;
  }

  static validCode(control: Control) {
    return CustomValidatorsComponent.valid(/^[A-Za-z0-9_\.\-]*$/, control.value, { "codeValid": true });
  }

  static validMessageName(control: Control) {
    let rr =  CustomValidatorsComponent.valid(/^[A-Za-z0-9_\.\-]*$/, control.value, { "messageNameValid": true });
    return rr;
  }

  static validUsername(control: Control) {
    return CustomValidatorsComponent.valid(/^[A-Za-z0-9_\.\-]*$/, control.value, { "usernameValid": true } );
  }

  static messageNameDuplication(control: Control) {
    return messageNameCheck(control);
  }
}