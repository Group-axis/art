import {
  it,
  inject,
  injectAsync,
  describe,
  beforeEachProviders
  
} from '@angular/core/testing';

import {Component, provide} from 'angular2/core';

// Load the implementations that should be tested
import {SibesRouting} from './sibes-routing.component';

describe('SibesRouting', () => {
  // provide our implementations or mocks to the dependency injector
  beforeEachProviders(() => [
    SibesRouting
  ]);

  it('should log ngOnInit', inject([ SibesRouting ], (sibesRouting) => {
    spyOn(console, 'log');
    expect(console.log).not.toHaveBeenCalled();

    sibesRouting.ngOnInit();
    expect(console.log).toHaveBeenCalled();
  }));

});
