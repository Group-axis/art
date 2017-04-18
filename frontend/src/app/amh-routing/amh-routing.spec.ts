import {
  it,
  inject,
  injectAsync,
  describe,
  beforeEachProviders
  
} from 'angular2/testing';

import {Component, provide} from 'angular2/core';

// Load the implementations that should be tested
import {AMHRouting} from './amh-routing.component';

describe('AMHRouting', () => {
  // provide our implementations or mocks to the dependency injector
  beforeEachProviders(() => [
    AMHRouting
  ]);

  it('should log ngOnInit', inject([ AMHRouting ], (amhRouting) => {
    spyOn(console, 'log');
    expect(console.log).not.toHaveBeenCalled();

    amhRouting.ngOnInit();
    expect(console.log).toHaveBeenCalled();
  }));

});
