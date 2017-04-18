
export class UserList {
  public username: string;
  public firstName: string;
  public lastName: string;
  public profiles : string;
  public active : boolean;
  
 constructor(username: string, active : boolean, firstName: string, lastName: string, profiles : string) {
   this.username=username;
   this.active = active;
   this.firstName=firstName;
   this.lastName=lastName;
   this.profiles = profiles;
 }

 static getFromSource(data : any)  : UserList {
    let esUser = data['_source'];
    return UserList.getFromObject(esUser);
  }

  static getFromObject(obj : any)  : UserList {
    return new UserList(obj.username, obj.active, obj.firstName, obj.lastName, obj.profiles);
  }
 
  private display(msg: string) {
    //console.log(msg + " "+ JSON.stringify(this));
  }
}