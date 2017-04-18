import {Directive, Attribute, ElementRef, Inject, OnInit, Renderer, Input, ViewContainerRef, TemplateRef, DynamicComponentLoader} from 'angular2/core';
import {Router, RouterOutlet, ComponentInstruction} from 'angular2/router';
import { Auth, hasPermissions, Logger } from '../components/services';

abstract class basePermissions {
    protected permissions: Array<string>;
    protected auth: Auth;
    protected viewContainerRef: ViewContainerRef;
    protected templateRef: TemplateRef<any>;
    protected element: ElementRef;
    protected logger : Logger;

    abstract doUserNotDefined(): void;
    abstract doUserDoesNotHavePermission(): void;
    abstract doUserHasPermission(): void;

    protected setPermissions(permissions) {
        this.permissions = permissions;
        let codeResp =  this.auth.hasPermission(permissions);
       // this.logger.debug("asking for permissions...resp "+ codeResp);

        switch(codeResp) {
            case -1: //No user defined
                this.doUserNotDefined();
            break;
            case 1: // with permissions
                this.doUserHasPermission();
            break;
            case 0: // without permissions
                this.doUserDoesNotHavePermission();
            break;

        }
        
    }

}

@Directive({ selector: '[gpIf]' })
export class Permissions extends basePermissions {

    constructor( @Inject(ViewContainerRef) viewContainerRef: ViewContainerRef, @Inject(TemplateRef) templateRef: TemplateRef<any>
    , @Inject(Auth) auth: Auth, @Inject(Logger) logger : Logger) {
        super();
        
        this.viewContainerRef = viewContainerRef;
        this.templateRef = templateRef;
        this.auth = auth;
    }

    @Input()
    set gpIf(permissions) {
        this.setPermissions(permissions);
    }

    doUserNotDefined() {
        this.viewContainerRef.clear();
    }

    doUserHasPermission() {
        this.viewContainerRef.createEmbeddedView(this.templateRef);
    }

    doUserDoesNotHavePermission() {
        this.viewContainerRef.clear();
    }

}

@Directive({ selector: '[gpNotIf]' })
export class NotPermissions extends basePermissions {

    constructor( @Inject(ViewContainerRef) viewContainerRef: ViewContainerRef, @Inject(TemplateRef) templateRef: TemplateRef<any>
    , @Inject(Auth) auth: Auth, @Inject(Logger) logger : Logger) {
        super();
      //  this.logger.debug("directive not permissions constructor ");
        this.viewContainerRef = viewContainerRef;
        this.templateRef = templateRef;
        this.auth = auth;
    }

    @Input()
    set gpNotIf(permissions) {
        this.setPermissions(permissions);
    }

    doUserNotDefined() {
        this.viewContainerRef.clear();
    }

    doUserHasPermission() {
        this.viewContainerRef.clear();
    }

    doUserDoesNotHavePermission() {
        this.viewContainerRef.createEmbeddedView(this.templateRef);
    }

}

@Directive({
    selector: '[gpEnabled]'
})
export class DisablePermissions extends basePermissions {

    constructor(element: ElementRef, @Inject(Auth) auth: Auth, @Inject(Logger) logger : Logger) {
        super();
        this.element = element;
        this.auth = auth;
    }

    @Input()
    set gpEnabled(permissions) {
        this.setPermissions(permissions);
    }

    doUserNotDefined() {
        this.element.nativeElement.disabled = true;
        return;
    }


    doUserHasPermission() {
        this.element.nativeElement;
    }

    doUserDoesNotHavePermission() {
        this.element.nativeElement.disabled = true;
    }

}