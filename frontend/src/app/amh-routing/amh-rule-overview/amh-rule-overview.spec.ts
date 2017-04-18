import {
  it,
  inject,
  injectAsync,
  describe,
  beforeEachProviders
} from 'angular2/testing';

import {Component, provide} from 'angular2/core';

// Load the implementations that should be tested
import {AMHRuleOverviewComponent} from './amh-rule-overview.component';

describe('AMHRuleOverviewComponent', () => {
  // provide our implementations or mocks to the dependency injector
  beforeEachProviders(() => [
    AMHRuleOverviewComponent
  ]);

  it('should log ngOnInit', inject([ AMHRuleOverviewComponent ], (amhRuleOverviewComponent) => {
    spyOn(console, 'log');
    expect(console.log).not.toHaveBeenCalled();

    amhRuleOverviewComponent.ngOnInit();
    expect(console.log).toHaveBeenCalled();
  }));

});
