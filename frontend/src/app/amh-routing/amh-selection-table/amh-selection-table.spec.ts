import {
  it,
  inject,
  injectAsync,
  describe,
  beforeEachProviders
} from 'angular2/testing';

import {Component, provide} from 'angular2/core';

// Load the implementations that should be tested
import {AMHAssignmentContentComponent} from './amh-assignment-content.component';

describe('AMHAssignmentContentComponent', () => {
  // provide our implementations or mocks to the dependency injector
  beforeEachProviders(() => [
    AMHAssignmentContentComponent
  ]);

  it('should log ngOnInit', inject([ AMHAssignmentContentComponent ], (amhAssignmentContentComponent) => {
    spyOn(console, 'log');
    expect(console.log).not.toHaveBeenCalled();

    amhAssignmentContentComponent.ngOnInit();
    expect(console.log).toHaveBeenCalled();
  }));

});
