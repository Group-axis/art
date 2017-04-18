import {
  it,
  inject,
  injectAsync,
  describe,
  beforeEachProviders,
  TestComponentBuilder
} from 'angular2/testing';

import {Component, provide} from 'angular2/core';

// Load the implementations that should be tested
import {UserAdmin} from './user-admin.component';

describe('UserAdmin', () => {
  // provide our implementations or mocks to the dependency injector
  beforeEachProviders(() => [
    About
  ]);

  it('should log ngOnInit', inject([ UserAdmin ], (userAdmin) => {
    spyOn(console, 'log');
    expect(console.log).not.toHaveBeenCalled();

    userAdmin.ngOnInit();
    expect(console.log).toHaveBeenCalled();
  }));

});
