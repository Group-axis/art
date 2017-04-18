import {
  it,
  inject,
  injectAsync,
  describe,
  beforeEachProviders
} from 'angular2/testing';

import {Component, provide} from 'angular2/core';

// Load the implementations that should be tested
import {UserHome} from './user-home.component';

describe('UserHome', () => {
  // provide our implementations or mocks to the dependency injector
  beforeEachProviders(() => [
    UserHome
  ]);

  it('should log ngOnInit', inject([ UserHome ], (userHome) => {
    spyOn(console, 'log');
    expect(console.log).not.toHaveBeenCalled();

    userHome.ngOnInit();
    expect(console.log).toHaveBeenCalled();
  }));

});
