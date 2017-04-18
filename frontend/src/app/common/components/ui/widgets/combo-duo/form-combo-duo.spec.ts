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
import {FormComboDuoComponent} from './form-combo-duo.component';

describe('FormComboDuoComponent', () => {
  // provide our implementations or mocks to the dependency injector
  beforeEachProviders(() => [
    FormComboDuoComponent
  ]);

  it('should log ngOnInit', inject([ FormComboDuoComponent ], (formComboDuoComponent) => {
    spyOn(console, 'log');
    expect(console.log).not.toHaveBeenCalled();

    formComboDuoComponent.ngOnInit();
    expect(console.log).toHaveBeenCalled();
  }));

});
