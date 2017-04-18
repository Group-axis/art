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
import {GPForm} from './ui-form.component';

describe('GPForm', () => {
  // provide our implementations or mocks to the dependency injector
  beforeEachProviders(() => [
    GPForm
  ]);

  it('should log ngOnInit', inject([ GPForm ], (gpForm) => {
    spyOn(console, 'log');
    expect(console.log).not.toHaveBeenCalled();

    gpForm.ngOnInit();
    expect(console.log).toHaveBeenCalled();
  }));

});
