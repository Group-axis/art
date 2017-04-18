import {
  it,
  inject,
  injectAsync,
  describe,
  beforeEachProviders,
  TestComponentBuilder
} from 'angular2/testing';

import {Component, provide} from 'angular2/core';

// Load the implementations that should be tested
import {SibesRoutingHome} from './sibes-routing-home.component';

describe('SibesRoutingHome', () => {
  // provide our implementations or mocks to the dependency injector
  beforeEachProviders(() => [
    SibesRoutingHome
  ]);

  it('should log ngOnInit', inject([ SibesRoutingHome ], (sibesRoutingHome) => {
    spyOn(console, 'log');
    expect(console.log).not.toHaveBeenCalled();

    sibesRoutingHome.ngOnInit();
    expect(console.log).toHaveBeenCalled();
  }));

});
