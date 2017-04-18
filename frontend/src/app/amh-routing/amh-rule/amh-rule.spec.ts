import {
  it,
  inject,
  injectAsync,
  describe,
  beforeEachProviders
} from 'angular2/testing';

import {Component, provide} from 'angular2/core';

// Load the implementations that should be tested
import {AMHRuleComponent} from './amh-rule.component';

describe('AMHRuleComponent', () => {
  // provide our implementations or mocks to the dependency injector
  beforeEachProviders(() => [
    AMHRuleComponent
  ]);

  it('should log ngOnInit', inject([ AMHRuleComponent ], (aMHRuleComponent) => {
    spyOn(console, 'log');
    expect(console.log).not.toHaveBeenCalled();

    aMHRuleComponent.ngOnInit();
    expect(console.log).toHaveBeenCalled();
  }));

});
