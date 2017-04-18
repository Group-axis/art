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
import {SAARoutingPoint} from './saa-routing-point.component';

describe('SAARoutingPoint', () => {
  // provide our implementations or mocks to the dependency injector
  beforeEachProviders(() => [
    SAARoutingPoint
  ]);

  it('should log ngOnInit', inject([ SAARoutingPoint ], (saaRoutingPoint) => {
    spyOn(console, 'log');
    expect(console.log).not.toHaveBeenCalled();

    saaRoutingPoint.ngOnInit();
    expect(console.log).toHaveBeenCalled();
  }));

});
