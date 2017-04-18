import {Component} from 'angular2/core';
import {Store, Logger} from '../../common/components/services';
import {Permissions} from '../../common/directives';
import {MenuConfig} from '../../models/menu';
import {HeaderSecondary} from '../../header-secondary';


@Component({
  selector: 'message-home',
  styles: [`
    h1 {
      font-family: Arial, Helvetica, sans-serif
    }
  `],
  directives: [HeaderSecondary, Permissions],
  template: require('./message-home.html')
})

export class MessageHome {
 private menuConfig : Array<MenuConfig> = [
    new MenuConfig("fa fa-home","/home","Home"),
    new MenuConfig("fa fa-cloud-sitemap","","Message partner")];

  constructor(private store : Store, private logger : Logger) {

  }

  ngOnInit() {
   this.logger.log('hello `message Home` component');
   this.store.setCurrentEnv("UNKNOWN");
   this.store.setCurrentVersion("DEFAULT");
  }
  asyncDataWithWebpack() {
  }

}
