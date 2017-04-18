import { Injectable } from 'angular2/core';
import { Store } from './store.component';
import { Logger } from './log.component';

@Injectable()
export class Context {
  private _AMH_ENV_KEY = "amh-env";
  private _AMH_VER_KEY = "amh-version";
  private _cache: Object;

  constructor(private store : Store, private logger : Logger) {
  }

  public getCurrentEnv() : string { return this.retrieveValue(this._AMH_ENV_KEY); }
  public setCurrentEnv(env: string) { this.storeValue(this._AMH_ENV_KEY, env); }
  public getCurrentVersion() : string { return this.retrieveValue(this._AMH_VER_KEY); }
  public setCurrentVersion(version: string) { this.storeValue(this._AMH_VER_KEY, version); }


  private retrieveValue(key : string) : any {
    let value = this._cache[key];
    
    if (!value) {
       value = this.store.retrieveValue(key);
       this._cache[key] = value; 
    }

    if (value) {
      return JSON.parse(value);
    }

    this.logger.warn("no value found for "+key);
    return {};
  }

  private storeValue(key : string, value : any) : any {
    let stringifyValue = JSON.stringify(value);
    this.store.storeValue(key, stringifyValue);
    this._cache[key] = stringifyValue;
  }

  // get(key: any) {
  //   return this.retrieveValue(key);
  // }
};