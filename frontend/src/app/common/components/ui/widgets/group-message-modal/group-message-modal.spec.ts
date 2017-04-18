import {
  it,
  inject,
  injectAsync,
  describe,
  beforeEachProviders
} from 'angular2/testing';

import {Component, provide} from 'angular2/core';

// Load the implementations that should be tested
import {GroupMessageModalComponent} from './group-message-modal.component';

describe('GroupMessageModalComponent', () => {
  // provide our implementations or mocks to the dependency injector
  beforeEachProviders(() => [
    GroupMessageModalComponent
  ]);

  it('should log ngOnInit', inject([ GroupMessageModalComponent ], (groupMessageModalComponent) => {
    spyOn(console, 'log');
    expect(console.log).not.toHaveBeenCalled();

    groupMessageModalComponent.ngOnInit();
    expect(console.log).toHaveBeenCalled();
  }));

});
