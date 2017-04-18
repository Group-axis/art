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
import {FormPickListComponent} from './form-pick-list.component';

describe('FormPickListComponent', () => {
  // provide our implementations or mocks to the dependency injector
  beforeEachProviders(() => [
    FormPickListComponent
  ]);

  it('should log ngOnInit', inject([ FormPickListComponent ], (formPickListComponent) => {
    spyOn(console, 'log');
    expect(console.log).not.toHaveBeenCalled();

    formPickListComponent.ngOnInit();
    expect(console.log).toHaveBeenCalled();
  }));

});
