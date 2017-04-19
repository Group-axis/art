import {Component} from 'angular2/core';
import {ROUTER_DIRECTIVES, RouteConfig, RouteParams} from 'angular2/router';

// console.log('`SAARouting` component loaded asynchronously');

@Component({
  selector: 'saa-routing',
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
  { path: '/', name: 'SAARoutingHome', loader: () => require('es6-promise!./saa-routing-home')('SAARoutingHome'), useAsDefault: true },
  { path: '/routing-overview', name: 'PointsOverview', loader: () => require('es6-promise!./saa-points-overview')('SAAPointsOverviewComponent') },
  { path: '/routing-point/create', name: 'PointCreate', loader: () => require('es6-promise!./saa-routing-point')('SAARoutingPoint') },
  { path: '/routing-point/:id/edit', name: 'PointEdit', loader: () => require('es6-promise!./saa-routing-point')('SAARoutingPoint') },  
  { path: '/routing-rule/create', name: 'RuleCreate', loader: () => require('es6-promise!./saa-routing-rule')('SAARoutingRule') },
  { path: '/routing-rule/:pointName/:sequence/edit', name: 'RuleEdit', loader: () => require('es6-promise!./saa-routing-rule')('SAARoutingRule') },
  { path: '/export', name: 'SAAExport', loader: () => require('es6-promise!./saa-export-import')('SAAExportImportComponent') },
  { path: '/simulation', name: 'SAASimulation', loader: () => require('es6-promise!./saa-simulator')('SAASimulatorComponent') },
  { path: '/import', name: 'SAAImport', loader: () => require('es6-promise!./saa-export-import')('SAAExportImportComponent') },
  { path: '/message-partner/...', name: 'MessagePartner', loader: () => require('es6-promise!./message-partner')('MessagePartner') }
])
export class SAARouting {
  constructor() {

  }

  ngOnInit() {
    // console.log('hello `SAA Routing` component');
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
