import {Headers, RequestOptions, Http} from 'angular2/http';
import {Injectable} from 'angular2/core';
import {Observable} from 'rxjs/Observable';
import {IText, IdCodeDescription} from '../models/referential';
import {UserList, User} from '../models/users';
import {TreeNode} from '../common/components/ui/widgets/tree-view';
import {Config, Logger} from '../common/components/services';
import {Point, PointList} from "../models/routing";
import 'rxjs/add/operator/filter';

@Injectable()
export class UserService {
  private query: string = ` 
    {
      "fields": [
        "active",
        "username",
        "firstName",
        "lastName",
        "profiles",
        "email"
      ],
  "query": {
    "bool": {
      "should": [
        {
          "match": {
            "username": {
              "query": "##TO_REPLACE##",
              "analyzer": "standard",
              "operator": "and"
            }
          }
        },
        {
          "match": {
            "firstName": {
              "query": "##TO_REPLACE##",
              "analyzer": "standard",
              "operator": "and"
           }
         }
       },
        {
          "match": {
            "lastName": {
              "query": "##TO_REPLACE##",
              "analyzer": "standard",
              "operator": "and"
           }
         }
       }

      ]
    }
  },
  "from": 0,
  "size": 1000,
  "sort": [
    {
      "username": {
        "order": "asc"
      }
    },
    {
      "firstName": {
        "order": "asc"
      }
    },
    {
      "lastName": {
        "order": "asc"
      }
    }

  ]
}
  `;

  private userQuery: string = `
   {
      "fields": [
        "active",
        "username",
        "firstName",
        "lastName",
        "permissions",
        "profiles",
        "email"
      ],
        "filter" : {
            "query" : {
                "query_string" : {
                    "query" : "##TO_REPLACE##"
                }
            }
        }
      }
  `;
  private queryAll: string = ` 
    {
      "fields": [
        "active",
        "username",
        "firstName",
        "lastName",
        "profiles",
        "email"
      ],
  "sort": [
    {
      "username": {
        "order": "asc"
      }
    },
    {
      "firstName": {
        "order": "asc"
      }
    },
    {
      "lastName": {
        "order": "asc"
      }
    }

  ]
}
  `;

  constructor(private http: Http, private config: Config, private logger : Logger) {

  }

  //TODO: 
  /*
   *  + remove limit size=100 for all http request, use pagination instead  
  */

  findUserByUsername(username :string): Observable<any> {
    this.logger.debug("in findUSerByname");
    let payload = this.userQuery.replace(/##TO_REPLACE##/g, username);
    let found = undefined;
    return this.http.post(this.config.get("esBackUrl") + "/authentication/routingusers/_search", payload)
      .map(res => res.json())
      .flatMap(this.toFields)
      .map(user => {
        this.logger.debug(" username "+ username.length + " user.username "+  user.username.length + " equals "+ (username == user.username));
        this.logger.debug(" result "+(username == user.username && user.username !== "NOT_FOUND")+" found by username [" + username + "] " + JSON.stringify(user));
        return { "found": username == user.username && user.username !== "NOT_FOUND", "value": user }
      });


  }

  findUserMatches(text?: string): Observable<any> {
    return this.findUserMatchesJson(text)
      .flatMap(this.toFields)
      .filter(user => user.username != "system" && user.username != "testing" && user.username != "NOT_FOUND");
  }
  
  private findProfiles() : Observable<any> {
    return this.http.get(this.config.get("esBackUrl")+"/authentication/routingprofiles/_search?size=1000").map(res => res.json());
  }

  private toSearchResult(data : any)  : (any) {
    let total = data['hits'].total;
    if (total == 0 ) {
        return {"found":false, "value":[]};
    }

    let hitArray = data['hits'].hits;
    
    return {"found":true, "value":hitArray.map( s => s._source)};
  }


  findAllProfiles(): Observable<IdCodeDescription> {
    return this.findProfiles()
    .map(this.toSearchResult)
    .flatMap(
      res => {
        if (res.found) {
          return Observable.from( res.value.map(profile => new IdCodeDescription(profile.id, "N/A", profile.name)));
        } else {
          return Observable.from([]);
        }
      }
    );
    
  }

  private toFields(match): Observable<any> {
    let total = match['hits'].total;
    let userNotFound = { "username": "NOT_FOUND" };

    let result : Observable<any> =  total <= 0 ?
      Observable.from([userNotFound])
      : Observable.from(match['hits'].hits).map(hit => hit["fields"] || userNotFound);

    return result.map(user => { 
      user.active = user.active && user.active[0];
      return user;
    });      
  }

  private findUserMatchesJson(text?: string): Observable<any> {
    if (text) {
      let payload = this.query.replace(/##TO_REPLACE##/g, text);
      //this.logger.debug("findPointMatches payload "+ payload);
      return this.http.post(this.config.get("esBackUrl") + "/authentication/routingusers/_search?size=1000", payload).map(res => res.json());
    } else {
      return this.http.post(this.config.get("esBackUrl") + "/authentication/routingusers/_search?size=100", this.queryAll).map(res => res.json());
    }
  }

private replacerFor(fields: Array<string>): (key: string, value: any) => any {

    let replacer =
      function (key: string, value: any): any {
        fields.forEach(field => {
          if (value && value[field]) {
            //this.logger.log(" [replacer] instanceof " + (value[field] instanceof Array));
            if (value[field] instanceof Array) {
              value[field] = value[field][0];
            } else {
             // this.logger.log(field + "  [replacer] is not array");
            }
          }
        });

        return value;
      };

    return replacer;
  }

  saveUser(user: User): Observable<any> {
    var headers = new Headers();
    headers.append('Content-Type', 'application/json; charset=utf-8');

    return this.getPermissions(user.profiles).flatMap(
      permissions => {
        // let payload : string = JSON.stringify(user.toUpdateModel());
        let payload: string = JSON.stringify({
          "doc": {
            "active": user.active,
            "username": user.username,
            "firstName": user.firstName,
            "lastName": user.lastName,
            "profiles": user.profiles,
            "permissions": permissions,
            "email" : user.email
          }
        }, this.replacerFor(["username"]));
        this.logger.log(" save user sent: " + payload);

        return this.http.post(this.config.get("esBackUrl") + "/authentication/routingusers/" + user.username + "/_update", payload, headers).map(res => res.json());
        //   return this.http.put(this.config.get("saaBackUrl")+"/points/"+rule.routingPoint+"/rules/"+rule.sequence, payload, options).map(res =>  { this.logger.log(" from save "+res.json()); res.json(); })
      }
    );
  }

  createUser(user: User): Observable<any> {
    var headers = new Headers();
    headers.append('Content-Type', 'application/json; charset=utf-8');

    return this.getPermissions(user.profiles).flatMap(
      permissions => {
        // let payload : string = JSON.stringify(user.toUpdateModel());
        let payload: string = JSON.stringify({
          "active": user.active,
          "password": user.username,
          "oldPassword": user.username,
          "newPassword": "",
          "username": user.username,
          "firstName": user.firstName,
          "lastName": user.lastName,
          "profiles": user.profiles,
          "permissions": permissions,
          "email": user.email
        }, this.replacerFor(["username"]));
        this.logger.log(" create user sent: " + payload);
        return this.http.post(this.config.get("esBackUrl") + "/authentication/routingusers/" + user.username, payload, headers).map(res => res.json());
      }
    );

  }

  updateUserAudit(originalUser : User, user : User): Observable<any> {
    var headers = new Headers();
    headers.append('Content-Type', 'application/json; charset=utf-8');
    let payload : string = JSON.stringify({
      time : Date.now(),
      oldUserEntity : originalUser.toAuditModel,
      oldUserProfiles : originalUser.toAuditProfiles,
      newUserEntity : user.toAuditModel,
      newUserProfiles : user.toAuditProfiles
    });
    return this.http.put(this.config.get("auditBackUrl") + "/audit/user", payload, headers )
    .map(res => res.json())
  }

  createUserAudit(user : User): Observable<any> {
    var headers = new Headers();
    headers.append('Content-Type', 'application/json; charset=utf-8');
    let payload : string = JSON.stringify({
      time : Date.now(),
      newUserEntity : user.toAuditModel,
      newUserProfiles : user.toAuditProfiles
    });
    
    return this.http.put(this.config.get("auditBackUrl") + "/audit/user", payload, headers )
    .map(res => res.json())
  }

  private getPermissions(profiles: Array<number>): Observable<Array<string>> {
    let dbProfiles = Observable.from([
      { "id":1, "name": "Admin", "active": true, "permissions": ["amh.import", "amh.export", "amh.modify.rule", "amh.modify.assignment", "user.modify", "sibes.import", "sibes.export", "sibes.modify.rule", "sibes.modify.point"] },
      { "id":2, "name": "Support", "active": true, "permissions": ["amh.read.only", "user.read.only", "sibes.read.only"] },
      { "id":3, "name": "Operation", "active": true, "permissions": ["amh.modify.rule", "amh.modify.assignment", "sibes.modify.rule", "sibes.modify.point"] }
    ]);

  
 return this.findProfiles()
    .map(this.toSearchResult)
    .flatMap( res => res.found ?  Observable.from( res.value):Observable.from([]))
    .filter(profile => { this.logger.debug("p=> " + JSON.stringify(profile)); return profiles.indexOf(profile.id) >= 0; } )
    .flatMap(profile =>{ this.logger.debug("permissions=> " + JSON.stringify(profile.permissions)); return profile.permissions; })
    .reduce((acc, R) => { acc.push(R.toString()); return acc; }, new Array<string>());

  }

  public resetPassword(user : User) : Observable<any> {
    var headers = new Headers();
    headers.append('Content-Type', 'application/json; charset=utf-8');
    this.logger.debug("user received " + JSON.stringify(user));
    let updatedUser = user.resetPassword().toUpdateModel();

    this.logger.debug(" updated user " + JSON.stringify(updatedUser));
    return this.http.post(this.config.get("esBackUrl")+"/authentication/routingusers/"+user.username, updatedUser, headers).map(res => res.json());
  }

}