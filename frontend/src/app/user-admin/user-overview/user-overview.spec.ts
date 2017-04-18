import {
  it,
  inject,
  injectAsync,
  describe,
  beforeEachProviders
} from 'angular2/testing';

import {Component, provide} from 'angular2/core';

// Load the implementations that should be tested
import {AMHAssignmentList} from './amh-assignment-list.component';

describe('AMHAssignmentList', () => {
  // provide our implementations or mocks to the dependency injector
  beforeEachProviders(() => [
    AMHAssignmentList
  ]);

  it('should log ngOnInit', inject([ AMHAssignmentList ], (amhAssignmentList) => {
    spyOn(console, 'log');
    expect(console.log).not.toHaveBeenCalled();

    amhAssignmentList.ngOnInit();
    expect(console.log).toHaveBeenCalled();
  }));

});
