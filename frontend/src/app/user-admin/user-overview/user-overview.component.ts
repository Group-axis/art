import {Component, ViewChild} from '@angular/core';
import {DataTable, DataTableDirectives} from 'angular2-datatable/datatable';
import {UserService} from "../user.service";
import {UserList} from "../../models/users";
import {Observable} from 'rxjs/Observable';
import {Option} from '../../models/referential/option';
import {MenuConfig} from '../../models/menu';
import {HeaderSecondary} from '../../header-secondary';
import { RouteParams} from 'angular2/router';
import {Permissions, NotPermissions} from '../../common/directives';
import {Logger} from '../../common/components/services';
import {ProfileLabelPipe} from '../../common/components/ui/controls';

//this.logger.log('`User overview` component loaded asynchronously');

@Component({
  selector: 'user-overview',
  providers: [ UserService ],
  styles: [`
    h1 {
      font-family: Arial, Helvetica, sans-serif
    }
  `],
  template: require('./user-overview.html'),
  pipes : [ProfileLabelPipe],
  directives: [DataTableDirectives, HeaderSecondary, Permissions, NotPermissions]
})
export class UserOverviewComponent {
  @ViewChild("filterText") filterValue;
  @ViewChild(DataTable) table;
    
  private data: Array<UserList> = [];
  private original: Array<UserList> = [];
  private menuConfig: Array<MenuConfig> = [
    new MenuConfig("fa fa-home", "/home", "Home"),
    new MenuConfig("fa fa-users", "/user-admin", "User administration"),
    new MenuConfig("fa fa-list", "", "Users Overview")
  ];
  private profileLabelMap : Map<number,string> = new Map<number,string>();

  constructor(routeParams: RouteParams, private userService: UserService, private logger : Logger) {

    this.userService.findAllProfiles()
    .map(r => { 
        //TODO: Find a better way to reset the map only once
        if (this.profileLabelMap.size == 0)
          this.profileLabelMap = new Map<number,string>();
        return r;
      })
    .subscribe(profile => this.profileLabelMap.set(profile.id,profile.description));
  }

  ngOnInit() {
    this.logger.log('hello `Users overview` component');
    this.loadUsers();
    // Get the data from the server
  }


  private loadUsers() {

    this.original = [];
    this.userService.findUserMatches().subscribe(
      data => {
        this.original = this.original.concat(data);
      },
      err =>
        this.logger.log("Can't get users. Error code: %s, URL: %s ", err.status, err.url),
      () => {
         this.logger.log(this.original.length + ' user(s) are retrieved');
         this.data = this.original;
      }
    );

  }

  updateData(filterText) {
    if (!filterText) {
      this.data = this.original;
      return;
    }
    let temp =  [];
    this.userService.findUserMatches(filterText).subscribe(
      data => {
        this.logger.debug('data '+JSON.stringify(data));
        temp = temp.concat(data);
      },
      err =>
        this.logger.log("Can't get users. Error code: %s, URL: %s ", err.status, err.url),
      () => {
        this.data = temp;
        this.logger.debug(temp.length + ' users(s) are retrieved from ES with text '+filterText);
        // this.data = this.changeFilter(temp, { filtering: { filterString: filterText, columnName: "username" } });
        this.logger.log(this.data.length + ' user(s) are retrieved');
      }
    );
  }

  private changeFilter(data: any, config: any): any {
    if (!config.filtering) {
      return data;
    }

    let valueToFind = config.filtering.filterString.toUpperCase();
    let filteredUsername: Array<any> = data.filter((item: any) =>
      item[config.filtering.columnName].toUpperCase().match(valueToFind));

    let filteredFirstName: Array<any> = data.filter((item: any) =>
      (item["firstName"] || "").toUpperCase().match(valueToFind));

    let filteredLastName: Array<any> = data.filter((item: any) =>
      (item["lastName"] || "").toUpperCase().match(valueToFind));

    let filteredData = filteredUsername.concat(filteredFirstName).concat(filteredLastName);

    let uniqueList: Array<any> = [];
    filteredData.forEach(item => {
      let found = uniqueList.find((value, index, array) => {
        return item["username"] == value["username"];
      });
      if (!found) {
        uniqueList.push(item);
      }
    });

    return uniqueList;
  }

}
