import {Directive, Attribute, ElementRef, ViewContainerRef, DynamicComponentLoader} from 'angular2/core';
import {Router, RouterOutlet, ComponentInstruction} from 'angular2/router';
import { Config, Auth, Store, Logger } from '../components/services';

@Directive({
    selector: 'custom-router-outlet'
})
export class CustomRouterOutlet extends RouterOutlet {
    publicRoutes: any;
    private parentRouter: Router;

    constructor(_elementRef: ElementRef, _viewContainerRef: ViewContainerRef, _loader: DynamicComponentLoader,
        _parentRouter: Router, @Attribute('name') nameAttr: string, private _config: Config, private _auth : Auth
        , private persistence : Store, private logger : Logger) {
        super(_viewContainerRef, _loader, _parentRouter, nameAttr);
        this.parentRouter = _parentRouter;
    }

    activate(instruction: ComponentInstruction) {
        let url = instruction.urlPath;
        this.logger.log(" going to " + url);
        return this._config.load().then( () => { 
            if (!url || url.length == 0) {
                this._auth.removeUser();
            }
            return this._auth.checkUser().then( 
                (found) =>  {
                    if (found) {
                        this.logger.log("... go ahead!");
                        return super.activate(instruction);
                    } else {
                        this.logger.log("... No user found .... redirecting to HOME");
                        if (url && url.length > 0) {
                            this.parentRouter.navigateByUrl('/');    
                        } else {
                            return super.activate(instruction);
                        }
                    }
                });
        });
    }
}