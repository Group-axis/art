import {Component} from 'angular2/core';
import {Permissions} from '../../common/directives';
import {MenuConfig} from '../../models/menu';
import {HeaderSecondary} from '../../header-secondary';
import {Logger} from '../../common/components/services';

//this.logger.log('`User HOME` component loaded asynchronously');

@Component({
  selector: 'user-home',
  styles: [`
    h1 {
      font-family: Arial, Helvetica, sans-serif
    }
  `],
  directives: [HeaderSecondary, Permissions],
  template: require('./user-home.html')
})

export class UserHome {
 private menuConfig : Array<MenuConfig> = [
    new MenuConfig("fa fa-home","/home","Home"),
    new MenuConfig("fa fa-cloud-sitemap","","User Administration")];

  constructor(private logger : Logger) {

  }

  ngOnInit() {
    this.logger.log('hello `User Home` component');
    
    // static data that is bundled
    // var mockData = require('assets/mock-data/mock-data.json');
    // this.logger.log('mockData', mockData);
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
    //     this.logger.log('async mockData', json);
    //   });
    //
    // });
  }

}
