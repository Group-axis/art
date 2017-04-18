import {BaseAudit} from '../audit'

export class User extends BaseAudit {
  public username: string;
  public oldPassword: string;
  public newPassword: string;
  public firstName: string;
  public lastName: string;
  public permissions : Array<string>;
  public profiles : Array<number>;
  public active : boolean;
  public email : string;
  //UI fields
  public confirmNewPassword: string;
  
 constructor(username: string, oldPassword: string, newPassword: string, firstName?: string, lastName?: string, permissions?: Array<string>, profiles?: Array<number>, active? : boolean, email? : string) {
   super();
   this.username=username;
   this.oldPassword=oldPassword;
   this.newPassword=newPassword;
   this.firstName=firstName;
   this.lastName=lastName;
   this.permissions = permissions || [];
   this.profiles = profiles || [];
   this.active = active;
   this.email = email;
 }

 static getFromSource(data : any)  : User {
    let esUser = data['_source'];
    return User.getFromObject(esUser);
  }


  // static toString(value : any) : string {
  //   if (value ) {
  //     if (value.isArray && value.length > 0) {
  //       return value[0];
  //     }
  //     if (value.isArray && value.length == 0) {
  //       return "";
  //     }

  //     return value;
  //   }

  //   return "";
  // }

  static getFromObject(obj : any)  : User {
    //console.info(" in getFromObject");
    // console.debug(" username == "+this.toString(obj.username));
    // return new User(this.toString(obj.username), this.toString(obj.oldPassword), this.toString(obj.newPassword), this.toString(obj.firstName), this.toString(obj.lastName), obj.permissions, obj.profiles, obj.active, this.toString(obj.email));
    return new User(obj.username, obj.oldPassword, obj.newPassword, obj.firstName, obj.lastName, obj.permissions, obj.profiles, obj.active, obj.email);
  }
 
  hasSamePassword(password : string) {
    this.display("hasSame with " + password);
    return (password == this.oldPassword && !this.newPassword) || 
           (password == this.newPassword);
  }

  isResetPasswordNeeded() {
    this.display("isReset");
    return this.oldPassword && this.oldPassword.length > 0 && !this.newPassword;
  }

  isNewPasswordMismatched() {
    this.display("isNewPass");
    return this.newPassword !== this.confirmNewPassword;
  }

  resetAllPasswords() {
    this.newPassword = "";
    this.oldPassword = "";
    this.confirmNewPassword = "";
  }

  arePasswordFilled() {
    return this.oldPassword && this.newPassword && this.confirmNewPassword;
  }

  newPasswordLength() {
    return this.newPassword ? this.newPassword.length : 0; 
  }

  isNewPasswordDifferentThanOldPassword() {
    return this.newPassword !== this.oldPassword; 
  }

  toUpdateModel() {
    // console.info("from toUpdateModel "+this);
    //return String(this);
    // return JSON.stringify({"username":User.toString(this.username), "oldPassword":User.toString(this.oldPassword), "newPassword":User.toString(this.newPassword), "firstName":User.toString(this.firstName), "lastName":this.lastName, "permissions":this.permissions, "profiles":this.profiles, "active":this.active, "email":User.toString(this.email)});
    return JSON.stringify({"username":this.username, "oldPassword":this.oldPassword, "newPassword":this.newPassword, "firstName":this.firstName, "lastName":this.lastName, "permissions":this.permissions, "profiles":this.profiles, "active":this.active, "email":this.email});
  }
  
  resetPassword() : User {
    this.oldPassword = this.username;
    this.newPassword = "";
    return this;
  }

  isNotActive() {
    return !this.active;
  }

  getLoggedName() {
    return this.firstName + " " + this.lastName;
  }

  static empty() {
    return new User("","","");
  }

  private display(msg: string) {
    //console.log(msg + " "+ JSON.stringify(this));
  }

  toAuditProfiles() : Array<any> {
     return this.profiles
      .map(id => { 
        return {"id":id, 
                "module":"user",
              "name":"",
              "active":"Y"}; 
        }); 
  }

  toAuditModel() : any {
    return {"id":this.username, 
    "firstName":this.firstName, 
    "lastName":this.lastName, 
    "email":this.email, 
    "active":this.active ? 'Y' : 'N'
     };
  }
}