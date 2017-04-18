import {
  it,
  inject,
  injectAsync,
  describe,
  beforeEachProviders
} from 'angular2/testing';

import {Component, provide} from 'angular2/core';

// Load the implementations that should be tested
import {MultiSelectionComponent} from './multi-selection.component';

describe('FormPickListComponent', () => {
  // provide our implementations or mocks to the dependency injector
  beforeEachProviders(() => [
    MultiSelectionComponent
  ]);

  it('should log ngOnInit', inject([ MultiSelectionComponent ], (multiSelectionComponent) => {
    spyOn(console, 'log');
    expect(console.log).not.toHaveBeenCalled();

    multiSelectionComponent.ngOnInit();
    expect(console.log).toHaveBeenCalled();
  }));

});
