import {
  it,
  inject,
  injectAsync,
  describe,
  beforeEachProviders
  
} from 'angular2/testing';

import {Component, provide} from 'angular2/core';

// Load the implementations that should be tested
import {SAARoutingHome} from './saa-routing-home.component';

describe('SAARoutingHome', () => {
  // provide our implementations or mocks to the dependency injector
  beforeEachProviders(() => [
    SAARoutingHome
  ]);

  it('should log ngOnInit', inject([ SAARoutingHome ], (saaRoutingHome) => {
    spyOn(console, 'log');
    expect(console.log).not.toHaveBeenCalled();

    saaRoutingHome.ngOnInit();
    expect(console.log).toHaveBeenCalled();
  }));

});
