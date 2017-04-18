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
import {GPControlErrors} from './ui-controls.component';

describe('GPControlErrors', () => {
  // provide our implementations or mocks to the dependency injector
  beforeEachProviders(() => [
    GPControlErrors
  ]);

  it('should log ngOnInit', inject([ GPControlErrors ], (gpControlErrors) => {
    spyOn(console, 'log');
    expect(console.log).not.toHaveBeenCalled();

    gpControlErrors.ngOnInit();
    expect(console.log).toHaveBeenCalled();
  }));

});
