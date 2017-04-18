import {
  it,
  inject,
  injectAsync,
  describe,
  beforeEachProviders
} from 'angular2/testing';

import {Component, provide} from 'angular2/core';

// Load the implementations that should be tested
import {SelectionTableComponent} from './selection-table.component';

describe('FormPickListComponent', () => {
  // provide our implementations or mocks to the dependency injector
  beforeEachProviders(() => [
    SelectionTableComponent
  ]);

  it('should log ngOnInit', inject([ SelectionTableComponent ], (selectionTableComponent) => {
    spyOn(console, 'log');
    expect(console.log).not.toHaveBeenCalled();

    selectionTableComponent.ngOnInit();
    expect(console.log).toHaveBeenCalled();
  }));

});
