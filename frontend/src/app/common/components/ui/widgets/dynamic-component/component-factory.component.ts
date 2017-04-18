import {Directive, ReflectiveInjector, ViewContainerRef, ComponentResolver, ComponentMetadata, Type, ComponentFactory, Component, ViewEncapsulation, ComponentRef, DynamicComponentLoader,ElementRef, Input, EventEmitter, Output} from 'angular2/core';
import {Logger} from "../../../services";

export function createComponentFactoryOriginal(resolver: ComponentResolver, metadata: ComponentMetadata): Promise<ComponentFactory<any>> {
    const cmpClass = class DynamicComponent {};
    const decoratedCmp = Component(metadata)(cmpClass);
    return resolver.resolveComponent(decoratedCmp);
}

export function createComponentFactory(comp : Type, resolver: ComponentResolver, metadata: ComponentMetadata): Promise<ComponentFactory<any>> {
    const decoratedCmp = Component(metadata)(comp);
    return resolver.resolveComponent(decoratedCmp);
}

@Directive({
    selector: 'dynamic-html-outlet'
})
export class DynamicHTMLOutlet {
  @Input() meta: ComponentMetadata;
  @Input('childComponent') comp: Type;
  @Output() componentCreation : EventEmitter<any> = new EventEmitter<any>();
  
  constructor(private vcRef: ViewContainerRef, private resolver: ComponentResolver, private logger : Logger) {
  }
  
  ngOnChanges() {

    if (!this.comp || !this.meta) return;
    //+JSON.stringify(this.comp)+" meta - " +JSON.stringify(this.meta)
    this.logger.log("passing to createComponent ");
    
    createComponentFactory(this.comp, this.resolver, this.meta)
      .then(factory => {
        const injector = ReflectiveInjector.fromResolvedProviders([], this.vcRef.parentInjector);
        let componentCreated = this.vcRef.createComponent(factory, 0, injector, []);
        this.componentCreation.emit(componentCreated);
      });
  }
}