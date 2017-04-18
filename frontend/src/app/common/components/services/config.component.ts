import { Http } from '@angular/http';
import { Injectable } from '@angular/core';
import { Observable} from 'rxjs/Observable';
import { Store } from './store.component';

@Injectable()
export class Config {
  private _config: Object;
  private _env: Object;
  // private persistence = sessionStorage;

  constructor(private http: Http, private store : Store) { //, private persistence: Store
  }

  load() {
    //console.log("... LOADING properties");
    let _config = this.store.retrieveValue("conf-config");
    let _env = this.store.retrieveValue("conf-env");
    // this._config = this.persistence.getItem("conf-config");
    // this._env = this.persistence.getItem("conf-env");
    
    if (_config && _env) {
      // console.log("config retrieved from store ");
      this._config = JSON.parse(_config);
      this._env = JSON.parse(_env);
      //console.log("_config  "+_config+"_env  "+_env);
      return new Promise((resolve, reject) => resolve(true));
    } else {
      // console.log("accessing config parameters");
      return new Promise((resolve, reject) => {
        this.http.get('assets/config/env.json')
          .map(res => res.json())
          .subscribe((env_data) => {
            this._env = env_data;
            // console.log("accessing parameters from "+env_data.env + " environment.");
            this.http.get('assets/config/' + env_data.env + '.json')
              .map(res => res.json())
              .subscribe((data) => {
                this._config = data;
                // console.log("...  properties LOADED");
                this.store.storeValue("conf-config", JSON.stringify(this._config));
    	          this.store.storeValue("conf-env", JSON.stringify(this._env));
                resolve(true);
              });
          });
      });
    }  
  }
  

  getEnv(key: any) {
    return this._env[key];
  }

  get(key: any) {
    if(!this._config) {
      // console.log(" config not defined yet returnig ");
      return "no_value";
    }
    return this._config[key];
  }

  getOrElse(key : string, defaultValue: number) : number {
 //this.config.get("simulationBackUrl")
    if(!this._config) {
      // console.log(" config not defined yet returnig ");
      return defaultValue;
    }
    return this._config[key] == undefined ? defaultValue : this._config[key];
  }
};