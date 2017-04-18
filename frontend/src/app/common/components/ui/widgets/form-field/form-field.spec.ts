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
import {FormFieldComponent} from './form-field.component';

describe('FormInputTextComponent', () => {
  // provide our implementations or mocks to the dependency injector
  beforeEachProviders(() => [
    FormFieldComponent
  ]);

  it('should log ngOnInit', inject([ FormFieldComponent ], (formFieldComponent) => {
    spyOn(console, 'log');
    expect(console.log).not.toHaveBeenCalled();

    formFieldComponent.ngOnInit();
    expect(console.log).toHaveBeenCalled();
  }));

});
