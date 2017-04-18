import {Component} from 'angular2/core';
import {ROUTER_DIRECTIVES, RouteConfig, RouteParams} from 'angular2/router';

//console.log('`AMHRouting` component loaded asynchronously');

@Component({
  selector: 'amh-routing',
//   styles: [`
//     h1 {
//       font-family: Arial, Helvetica, sans-serif
//     }
//   `],
  directives: [ROUTER_DIRECTIVES],
  template: `
    <router-outlet></router-outlet>
  `
})
@RouteConfig([
  { path: '/', name: 'AMHHome', loader: () => require('es6-promise!./amh-home')('AMHHome'), useAsDefault: true },
  { path: '/assignment-list', name: 'AMHAssignmentList', loader: () => require('es6-promise!./amh-assignment-list')('AMHAssignmentList') },
  { path: '/assignment/create', name: 'AMHAssignmentCreate', loader: () => require('es6-promise!./amh-assignment')('AMHAssignmentComponent') },
  { path: '/assignment/edit', name: 'AMHAssignmentEdit', loader: () => require('es6-promise!./amh-assignment')('AMHAssignmentComponent') },  
  { path: '/rule/create', name: 'AMHRuleCreate', loader: () => require('es6-promise!./amh-rule')('AMHRuleComponent') },
  { path: '/rule/:code/edit', name: 'AMHRuleEdit', loader: () => require('es6-promise!./amh-rule')('AMHRuleComponent') },
  { path: '/export', name: 'AMHExport', loader: () => require('es6-promise!./amh-export-import')('AMHExportImportComponent') },
  { path: '/import', name: 'AMHImport', loader: () => require('es6-promise!./amh-export-import')('AMHExportImportComponent') },
  { path: '/simulation', name: 'AMHSimulation', loader: () => require('es6-promise!./amh-simulator')('AMHSimulatorComponent') },
  { path: '/rule-overview', name: 'AMHRuleOverview', loader: () => require('es6-promise!./amh-rule-overview')('AMHRuleOverviewComponent') }
])
export class AMHRouting {
  constructor() {
  }

  ngOnInit() {
    // console.log('hello `AMH Routing` component');
    // static data that is bundled
    // var mockData = require('assets/mock-data/mock-data.json');
    // console.log('mockData', mockData);
    // if you're working with mock data you can also use http.get('assets/mock-data/mock-data.json')
    // this.asyncDataWithWebpack();
  }
  asyncDataWithWebpack() {
    // you can also async load mock data with 'es6-promise-loader'
    // you would do this if you don't want the mock-data bundled
    // remember that 'es6-promise-loader' is a promise
    // var asyncMockDataPromiseFactory = require('es6-promise!assets/mock-data/mock-data.json');
    // setTimeout(() => {
    //
    //   let asyncDataPromise = asyncMockDataPromiseFactory();
    //   asyncDataPromise.then(json => {
    //     console.log('async mockData', json);
    //   });
    //
    // });
  }

}
