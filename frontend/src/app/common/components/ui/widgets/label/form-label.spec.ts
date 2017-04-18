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
import {FormLabelComponent} from './form-label.component';

describe('FormInputTextComponent', () => {
  // provide our implementations or mocks to the dependency injector
  beforeEachProviders(() => [
    FormLabelComponent
  ]);

  it('should log ngOnInit', inject([ FormLabelComponent ], (formLabelComponent) => {
    spyOn(console, 'log');
    expect(console.log).not.toHaveBeenCalled();

    formLabelComponent.ngOnInit();
    expect(console.log).toHaveBeenCalled();
  }));

});
