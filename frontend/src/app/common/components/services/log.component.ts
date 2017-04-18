import { Injectable, provide, Provider } from '@angular/core';
import { Config } from './';


@Injectable()
export class Logger {
  private isProdEnvironment : boolean = true; 
// private config : Config;
  constructor() {
//      console.log("DDD : "+this.config.get("backUrl"));
  }

  noLog(msg : string) {
    //console.log("no log "+msg)
  }

  get debug() {
      if (this.isProdEnvironment) {
        return this.noLog.bind(this);
      } else {
        return console.debug.bind(console);
      }
  }

  get error() {
    if (this.isProdEnvironment) {
        return this.noLog.bind(this);
      } else {
      return console.error.bind(console);
      }
  }

  get log() {
    if (this.isProdEnvironment) {
        return this.noLog.bind(this);
      } else {
      return console.log.bind(console);
      }
  }

  get warn() {
    if (this.isProdEnvironment) {
        return this.noLog.bind(this);
      } else {
      return console.warn.bind(console);
      }
  }

  get info() {
    if (this.isProdEnvironment) {
        return this.noLog.bind(this);
      } else {
      return console.info.bind(console);
      }
  }
}

export var LOGGING_PROVIDERS : Provider[] = [
      provide(Logger, {useClass: Logger}),
    ];