import {
  it,
  inject,
  injectAsync,
  describe,
  beforeEachProviders
} from 'angular2/testing';

import {Component, provide} from 'angular2/core';

// Load the implementations that should be tested
import {AMHAssignmentSearchComponent} from './amh-assignment-search.component';

describe('AMHAssignmentSearchComponent', () => {
  // provide our implementations or mocks to the dependency injector
  beforeEachProviders(() => [
    AMHAssignmentSearchComponent
  ]);

  it('should log ngOnInit', inject([ AMHAssignmentSearchComponent ], (amhAssignmentSearchComponent) => {
    spyOn(console, 'log');
    expect(console.log).not.toHaveBeenCalled();

    amhAssignmentSearchComponent.ngOnInit();
    expect(console.log).toHaveBeenCalled();
  }));

});
