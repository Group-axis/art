import { Component } from '@angular/core';
import { Router, ROUTER_DIRECTIVES} from 'angular2/router';
import { NgIf, CORE_DIRECTIVES, FORM_DIRECTIVES, FORM_PROVIDERS } from '@angular/common';
import { Http, Headers } from '@angular/http';
import { Auth, Logger } from '../common/components/services';
import { User } from '../models/users';
/*
 * We're loading this component asynchronously
 * We are using some magic with es6-promise-loader that will wrap the module with a Promise
 * see https://github.com/gdi2290/es6-promise-loader for more info
 */

//this.logger.log('`Login` component loaded asynchronously');

@Component({
  selector: 'login',
   directives: [ ROUTER_DIRECTIVES, CORE_DIRECTIVES, FORM_DIRECTIVES, NgIf ],
   styles: [`
    h1 {
      font-family: Arial, Helvetica, sans-serif
    }
  `],
  template: require('./login.html'),
  providers : [ ]
})
export class Login {
  private errorMessages : Map<number, string> = new Map<number, string>();
  private displayError : boolean = false;
  private resetPassword : boolean = false;
  private errorNumber : number = 0;
  private user : User = new User("","","");

  constructor(private router: Router, private http: Http, private auth : Auth, private logger : Logger) {
    this.errorMessages.set(1,"Invalid old password, please try again");
    this.errorMessages.set(2,"New password mismatch, please try again");
    this.errorMessages.set(3,"New and Old passwords must be different");
    this.errorMessages.set(4,"New Password must have at least 6 characters");
    this.errorMessages.set(5,"New Password must have at must 15 characters");
    this.errorMessages.set(20,"Invalid user name or password");
    this.errorMessages.set(21,"The user is currently inactive");
    this.errorMessages.set(99,"An error has occurred while saving user password");
  }

  ngOnInit() {
    this.logger.log('hello `Login` component');
  }

  asyncDataWithWebpack() {
    
  }

  doLogin(event) {
    event.preventDefault();
    // let body = JSON.stringify({ username, password });
    // this.http.post('http://localhost:3001/sessions/create', body, { headers: contentHeaders })
    //   .subscribe(
    //     response => {
    //       localStorage.setItem('id_token', response.json().id_token);
    //       this.router.navigate(['/home']);
    //     },
    //     error => {
    //       alert(error.text());
    //       this.logger.log(error.text());
    //     }
    //   );
    this.logger.log("accepting user "+JSON.stringify(this.user));
    
    this.auth.findUserByUsername(this.user.username)
    .subscribe(
        foundUser => {
          if (foundUser.isNotActive()) {
           this.errorNumber = 21; 
           this.displayError = true;
           this.user = new User("","","");
          } else if (foundUser.hasSamePassword(this.user.oldPassword)) {
             if (foundUser.isResetPasswordNeeded()) {
                this.user.oldPassword = "";
                this.resetPassword = true;
              } else  {
                this.auth.setUser(foundUser);
                this.displayError = false;
                this.router.navigateByUrl('/home');
              }
          } else {
           this.errorNumber = 20; 
           this.displayError = true;
           this.user = new User("","",""); 
          }
        },
        error => {
          this.errorNumber = 20;
           this.displayError = true;
           this.user = new User("","","");
           this.logger.log(error.status);
        }
      );
  }

  doResetPassword(event) {
    event.preventDefault();
    this.logger.log("reseting user password "+JSON.stringify(this.user));
    
    if (this.user.isNewPasswordMismatched()) {
      this.errorNumber = 2;
      this.user.resetAllPasswords();
      return;
    }

    this.auth.findUserByUsername(this.user.username)
    .subscribe(
        foundUser => {
          if (!foundUser.hasSamePassword(this.user.oldPassword)) {
            this.user.resetAllPasswords();
            this.errorNumber = 1;
          } else if (!this.user.isNewPasswordDifferentThanOldPassword()) {
            this.user.resetAllPasswords();
            this.errorNumber = 3;
          } else if (this.user.newPasswordLength() < 6) {
            this.user.resetAllPasswords();
            this.errorNumber = 4;
          } else if (this.user.newPasswordLength() > 15) {
            this.user.resetAllPasswords();
            this.errorNumber = 5;
          } else  {
            foundUser.newPassword = this.user.newPassword;
            this.auth.updateUserNewPassword(foundUser)
             .subscribe(
                response => {
                  this.logger.log(" user new password saved! "+JSON.stringify(foundUser));
                  this.auth.setUser(foundUser);
                  this.errorNumber = 0;
                  this.resetPassword = false;
                  this.router.navigateByUrl('/home');
                },
                error => {
                  this.errorNumber = 99;
                  this.logger.log(error.status);
                }
            );
          } 
        },
        error => {
           this.errorNumber = 1;
           this.user = new User("","","");
           this.logger.log(error.status);
        }
      );
  }

  getErrorMessage() {
    if (this.errorNumber < 1) {
      return "";
    }

    return this.errorMessages.get(this.errorNumber) || "";
  }
 
}
