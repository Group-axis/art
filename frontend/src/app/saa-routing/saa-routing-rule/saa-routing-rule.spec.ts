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
import {SAARoutingRule} from './saa-routing-rule.component';

describe('SAARoutingRule', () => {
  // provide our implementations or mocks to the dependency injector
  beforeEachProviders(() => [
    SAARoutingRule
  ]);

  it('should log ngOnInit', inject([ SAARoutingRule ], (saaRoutingRule) => {
    spyOn(console, 'log');
    expect(console.log).not.toHaveBeenCalled();

    saaRoutingRule.ngOnInit();
    expect(console.log).toHaveBeenCalled();
  }));

});
