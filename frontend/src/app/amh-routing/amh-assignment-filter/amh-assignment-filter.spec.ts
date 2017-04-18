import {
  it,
  inject,
  injectAsync,
  describe,
  beforeEachProviders
} from 'angular2/testing';

import {Component, provide} from 'angular2/core';

// Load the implementations that should be tested
import {AMHAssignmentFilterComponent} from './amh-assignment-filter.component';

describe('AMHAssignmentFilterComponent', () => {
  // provide our implementations or mocks to the dependency injector
  beforeEachProviders(() => [
    AMHAssignmentFilterComponent
  ]);

  it('should log ngOnInit', inject([ AMHAssignmentFilterComponent ], (amhAssignmentFilterComponent) => {
    spyOn(console, 'log');
    expect(console.log).not.toHaveBeenCalled();

    amhAssignmentFilterComponent.ngOnInit();
    expect(console.log).toHaveBeenCalled();
  }));

});
