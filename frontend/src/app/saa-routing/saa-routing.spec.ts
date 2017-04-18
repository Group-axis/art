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
import {SAARouting} from './saa-routing.component';

describe('SAARouting', () => {
  // provide our implementations or mocks to the dependency injector
  beforeEachProviders(() => [
    SAARouting
  ]);

  it('should log ngOnInit', inject([ SAARouting ], (saaRouting) => {
    spyOn(console, 'log');
    expect(console.log).not.toHaveBeenCalled();

    saaRouting.ngOnInit();
    expect(console.log).toHaveBeenCalled();
  }));

});
