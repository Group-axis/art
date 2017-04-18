import {
  it,
  inject,
  injectAsync,
  describe,
  beforeEachProviders
} from 'angular2/testing';

import {Component, provide} from 'angular2/core';

// Load the implementations that should be tested
import {AMHAssignment} from './amh-assignment.component';

describe('AMHAssignment', () => {
  // provide our implementations or mocks to the dependency injector
  beforeEachProviders(() => [
    AMHAssignment
  ]);

  it('should log ngOnInit', inject([ AMHAssignment ], (amhAssignment) => {
    spyOn(console, 'log');
    expect(console.log).not.toHaveBeenCalled();

    amhAssignment.ngOnInit();
    expect(console.log).toHaveBeenCalled();
  }));

});
