import {Component} from 'angular2/core';
import {ROUTER_DIRECTIVES, RouteConfig, RouteParams} from 'angular2/router';

// console.log('`SibesRouting` component loaded asynchronously');

@Component({
  selector: 'sibes-routing',
  styles: [`
    h1 {
      font-family: Arial, Helvetica, sans-serif
    }
  `],
  directives: [ROUTER_DIRECTIVES],
  template:  `
    <router-outlet></router-outlet>
  `
})
@RouteConfig([
  { path: '/', name: 'SibesRoutingHome', loader: () => require('es6-promise!./sibes-routing-home')('SibesRoutingHome'), useAsDefault: true },  
  { path: '/routing-point/create', name: 'PointCreate', loader: () => require('es6-promise!./sibes-routing-point')('SibesRoutingPoint') },
  { path: '/routing-point/:id/edit', name: 'PointEdit', loader: () => require('es6-promise!./sibes-routing-point')('SibesRoutingPoint') },
  { path: '/routing-rule/create', name: 'RuleCreate', loader: () => require('es6-promise!./sibes-routing-rule')('SibesRoutingRule') },
  { path: '/routing-rule/:sequence/edit', name: 'RuleEdit', loader: () => require('es6-promise!./sibes-routing-rule')('SibesRoutingRule') }
])
export class SibesRouting {
  constructor() {

  }

  ngOnInit() {
    // console.log('hello `sibes Routing` component');
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
