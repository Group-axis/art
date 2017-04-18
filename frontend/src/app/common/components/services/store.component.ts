import { Injectable } from 'angular2/core';

@Injectable()
export class Store {
  private _AMH_ENV_KEY = "amh-env";
  private _AMH_VER_KEY = "amh-version";
  private persistence: any;
  private cache: any = {};

  constructor() {
    this.persistence = sessionStorage;
  }

  public getCurrentEnv(): string { return this.retrieveJSONValue(this._AMH_ENV_KEY); }
  public setCurrentEnv(env: string) { this.storeJSONValue(this._AMH_ENV_KEY, env); }
  public getCurrentVersion(): string { return this.retrieveJSONValue(this._AMH_VER_KEY); }
  public setCurrentVersion(version: string) { this.storeJSONValue(this._AMH_VER_KEY, version); }

  storeValue(key: string, value: any) {
    this.persistence.setItem(key, value);
    this.cache[key] = value;
  }

  retrieveValue(key: string): any {
    let value = this.cache[key];
    if (!value) {
      value = this.persistence.getItem(key);
    }

    return value;
  }

  removeValue(key: string) {
    this.cache[key] = undefined;
    this.persistence.removeItem(key);
  }

  private storeJSONValue(key: string, value : any) {
    if ( !key || !value ) {
       //console.warn("key "+ key + " or value "+ value+ " not defined");
       return;
    }

    this.storeValue(key, JSON.stringify(value));
  }

  private retrieveJSONValue(key: string): any {
    let value = this.retrieveValue(key);
    if (value) {
      return JSON.parse(value);
    }

    return {};
  }
}
