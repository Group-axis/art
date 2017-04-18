import {
  it,
  inject,
  injectAsync,
  describe,
  beforeEachProviders
} from 'angular2/testing';

import {Component, provide} from 'angular2/core';

// Load the implementations that should be tested
import {AMHSimulatorComponent} from './amh-simulator.component';

describe('AMHSimulator', () => {
  // provide our implementations or mocks to the dependency injector
  beforeEachProviders(() => [
    AMHSimulatorComponent
  ]);

  it('should log ngOnInit', inject([ AMHSimulatorComponent ], (amhSimulatorComponent) => {
    spyOn(console, 'log');
    expect(console.log).not.toHaveBeenCalled();

    amhSimulatorComponent.ngOnInit();
    expect(console.log).toHaveBeenCalled();
  }));

});
