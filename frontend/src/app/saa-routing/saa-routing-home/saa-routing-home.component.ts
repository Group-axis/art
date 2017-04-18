import {Component} from 'angular2/core';
import { Router, RouteParams} from 'angular2/router';
import {MenuConfig} from '../../models/menu';
import {HeaderSecondary} from '../../header-secondary';
import {Permissions, NotPermissions} from '../../common/directives';
// console.log('`SAA ROUTING HOME` component loaded asynchronously');

@Component({
  selector: 'saa-routing-home',
  styles: [`
    h1 {
      font-family: Arial, Helvetica, sans-serif
    }
  `],
  directives : [HeaderSecondary, Permissions, NotPermissions],
  template: require('./saa-routing-home.html')
})
export class SAARoutingHome {
   private menuConfig : Array<MenuConfig> = [
    new MenuConfig("fa fa-home","/home","Home"),
    new MenuConfig("fa fa-sitemap","","SAA Routing")
    ];

  constructor(private router: Router) {

  }

  ngOnInit() {
    // console.log('hello `SAA Routing Home` component');
  }
  
  asyncDataWithWebpack() {
    
  }

}
