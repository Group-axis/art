import { Http, Headers } from 'angular2/http';
import { Injectable } from 'angular2/core';
import { Observable } from 'rxjs/Observable';
import { Router } from 'angular2/router';
import { Store, Config } from './';
import { Logger } from './log.component';
import { User } from '../../../models/users';


@Injectable()
export class Auth {
//, private store : Store
private storage = sessionStorage;
  constructor(private http: Http, private router : Router, private config : Config, private logger : Logger) { }

  getUser() : User {
   // this.logger.log("... retrieving user name: " + this.storage.getItem('logged_user'));
    let retrievedUser = this.storage.getItem('logged_user');
    if (retrievedUser) {
       return User.getFromObject(JSON.parse(retrievedUser));
    } 
     return undefined;
  }

  setUser(user: User) {
     this.logger.log("... storing user name  "+user.toUpdateModel());
    //  this.sss.storeValue('logged_user', user);
    this.storage.setItem('logged_user', user.toUpdateModel());
  }

  removeUser() {
    this.logger.log("... removing logged user ");
    // this.sss.removeValue('logged_user');
    this.storage.removeItem('logged_user');
  }

  checkUser() : Promise<boolean> {
    let user : User = this.getUser();
    this.logger.log("checking user " + user);
    
    if (!user) {
      return new Promise((resolve, reject) => resolve(false));
    }

    return new Promise((resolve, reject) => {
        this.findUserByUsername(user.username)
       .subscribe(
         response => {
            this.logger.log("returning user found from checkUser");
            resolve(true);
         },
         error => {
           this.logger.log("returning user not found from checkUser");
            resolve(false);
         });
      });
  }

  findUserByUsername(username : string) : Observable<User> {
    return this.http.get(this.config.get("esBackUrl")+"/authentication/routingusers/"+username).map(res =>  {
       let response = res.json();
       //this.logger.log("found from ES user " + JSON.stringify(response));
       return User.getFromSource(response);
    });
  }

  updateUserNewPassword(user : User) : Observable<any> {
    var headers = new Headers();
    headers.append('Content-Type', 'application/json; charset=utf-8');
    return this.http.post(this.config.get("esBackUrl")+"/authentication/routingusers/"+user.username, user.toUpdateModel(), headers).map(res => res.json());
  }

  hasPermission(permissions : Array<string>) : number {
    let user = this.getUser();
    
    if (!user) {
        this.logger.warn("No user found in session!");
        return -1;
    }

    return hasPermissions(user.permissions, permissions) ? 1 : 0;
  }
  
}

export function hasPermissions(userPermissions: Array<string>, requiredPermissions: Array<string>): boolean {
    let found = requiredPermissions.find(rp => userPermissions.indexOf(rp) > -1);
    //console.debug("permission found " + found);
    return found !== undefined;
}
