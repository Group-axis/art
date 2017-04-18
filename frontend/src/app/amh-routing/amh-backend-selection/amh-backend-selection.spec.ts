import {
  it,
  inject,
  injectAsync,
  describe,
  beforeEachProviders
} from 'angular2/testing';

import {Component, provide} from 'angular2/core';

// Load the implementations that should be tested
import {AMHBackendSelectionComponent} from './amh-backend-selection.component';

describe('AMHBackendSelectionComponent', () => {
  // provide our implementations or mocks to the dependency injector
  beforeEachProviders(() => [
    AMHBackendSelectionComponent
  ]);

  it('should log ngOnInit', inject([ AMHBackendSelectionComponent ], (amhBackendSelectionComponent) => {
    spyOn(console, 'log');
    expect(console.log).not.toHaveBeenCalled();

    amhBackendSelectionComponent.ngOnInit();
    expect(console.log).toHaveBeenCalled();
  }));

});
