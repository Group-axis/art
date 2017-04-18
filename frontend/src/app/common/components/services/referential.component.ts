import { Http } from '@angular/http';
import { Injectable, provide, Provider } from '@angular/core';
import { Observable} from 'rxjs/Observable';
import {Config, Store, Logger} from './';

@Injectable()
export class PageReferential {
  constructor(private http: Http, private store : Store, private config : Config) {
  }

  public getSAARules() : Observable<any> {
    let sibesRules = this.store.retrieveValue("sibes-rules");
    if (sibesRules) {
      return Observable.from([JSON.parse(sibesRules)]);
    }

    return this.http.get(this.config.get("esBackUrl")+"/referentials/sibes-rules/v1")
      .map(res => res.json())
      .map(res => { return {"found" : res.found , "source" : res._source} })
      .flatMap(
        res => {
          if (res.found) {
              sibesRules = JSON.stringify(res.source);
              this.store.storeValue("sibes-rules", sibesRules);
              return Observable.from([res.source]);
          } else {
            //this.logger.error("no referential for sibes-rules found ");
            return Observable.from([{}]);
          }
        }
      );
  } 
}

// export var REFERENTIAL_PROVIDERS : Provider[] = [
//       provide(PageReferential, {useClass: PageReferential}),
//     ];