import {
  it,
  inject,
  injectAsync,
  describe,
  beforeEachProviders
} from 'angular2/testing';

import {Component, provide} from 'angular2/core';

// Load the implementations that should be tested
import {SAASimulatorComponent} from './saa-simulator.component';

describe('AMHSimulator', () => {
  // provide our implementations or mocks to the dependency injector
  beforeEachProviders(() => [
    SAASimulatorComponent
  ]);

  it('should log ngOnInit', inject([ SAASimulatorComponent ], (saaSimulatorComponent) => {
    spyOn(console, 'log');
    expect(console.log).not.toHaveBeenCalled();

    saaSimulatorComponent.ngOnInit();
    expect(console.log).toHaveBeenCalled();
  }));

});
