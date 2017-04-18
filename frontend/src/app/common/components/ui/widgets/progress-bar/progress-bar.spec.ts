import {
  it,
  inject,
  injectAsync,
  describe,
  beforeEachProviders
  //, TestComponentBuilder
} from 'angular2/testing';

import {Component, provide} from '@angular/core';

// Load the implementations that should be tested
import {Progressbar} from './progress-bar.component';

describe('Progressbar', () => {
  // provide our implementations or mocks to the dependency injector
  beforeEachProviders(() => [
    Progressbar
  ]);

  it('should log ngOnInit', inject([ Progressbar ], (progressbar) => {
    spyOn(console, 'log');
    expect(console.log).not.toHaveBeenCalled();

    progressbar.ngOnInit();
    expect(console.log).toHaveBeenCalled();
  }));

});
