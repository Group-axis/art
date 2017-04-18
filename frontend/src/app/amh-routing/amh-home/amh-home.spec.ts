import {
  it,
  inject,
  injectAsync,
  describe,
  beforeEachProviders
} from 'angular2/testing';

import {Component, provide} from 'angular2/core';

// Load the implementations that should be tested
import {AMHHome} from './amh-home.component';

describe('AMHHome', () => {
  // provide our implementations or mocks to the dependency injector
  beforeEachProviders(() => [
    AMHHome
  ]);

  it('should log ngOnInit', inject([ AMHHome ], (amhHome) => {
    spyOn(console, 'log');
    expect(console.log).not.toHaveBeenCalled();

    amhHome.ngOnInit();
    expect(console.log).toHaveBeenCalled();
  }));

});
