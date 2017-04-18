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
import {SibesRoutingRule} from './sibes-routing-rule.component';

describe('SibesRoutingPoint', () => {
  // provide our implementations or mocks to the dependency injector
  beforeEachProviders(() => [
    SibesRoutingRule
  ]);

  it('should log ngOnInit', inject([ SibesRoutingRule ], (sibesRoutingRule) => {
    spyOn(console, 'log');
    expect(console.log).not.toHaveBeenCalled();

    sibesRoutingRule.ngOnInit();
    expect(console.log).toHaveBeenCalled();
  }));

});
