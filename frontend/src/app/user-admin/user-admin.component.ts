import {Component} from 'angular2/core';
import {ROUTER_DIRECTIVES, RouteConfig, RouteParams} from 'angular2/router';

//console.log('`UserAdmin` component loaded asynchronously');

@Component({
  selector: 'user-admin',
  directives: [ROUTER_DIRECTIVES],
  template: `
    <router-outlet></router-outlet>
  `
})
@RouteConfig([
  { path: '/', name: 'UserHome', loader: () => require('es6-promise!./user-home')('UserHome'), useAsDefault: true },
  { path: '/user-overview', name: 'UserOverview', loader: () => require('es6-promise!./user-overview')('UserOverviewComponent') },
  { path: '/user/create', name: 'UserCreate', loader: () => require('es6-promise!./user-creation')('UserManagementComponent') },
  { path: '/user/:username/edit', name: 'UserEdit', loader: () => require('es6-promise!./user-creation')('UserManagementComponent') }
  //,
  // { path: '/assignment/create', name: 'AMHAssignmentCreate', loader: () => require('es6-promise!./amh-assignment')('AMHAssignmentComponent') },
  // { path: '/assignment/edit', name: 'AMHAssignmentEdit', loader: () => require('es6-promise!./amh-assignment')('AMHAssignmentComponent') },  
  // { path: '/export', name: 'AMHExport', loader: () => require('es6-promise!./amh-export-import')('AMHExportImportComponent') },
  // { path: '/import', name: 'AMHImport', loader: () => require('es6-promise!./amh-export-import')('AMHExportImportComponent') }
])
export class UserAdmin {
  constructor() {

  }

  ngOnInit() {
    //console.log('hello `User Admin` component');
  }
  

}
