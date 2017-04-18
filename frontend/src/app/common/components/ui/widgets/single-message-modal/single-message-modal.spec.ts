import {
  it,
  inject,
  injectAsync,
  describe,
  beforeEachProviders
} from 'angular2/testing';

import {Component, provide} from 'angular2/core';

// Load the implementations that should be tested
import {SingleMessageModalComponent} from './single-message-modal.component';

describe('SingleMessageModalComponent', () => {
  // provide our implementations or mocks to the dependency injector
  beforeEachProviders(() => [
    SingleMessageModalComponent
  ]);

  it('should log ngOnInit', inject([ SingleMessageModalComponent ], (singleMessageModalComponent) => {
    spyOn(console, 'log');
    expect(console.log).not.toHaveBeenCalled();

    singleMessageModalComponent.ngOnInit();
    expect(console.log).toHaveBeenCalled();
  }));

});
