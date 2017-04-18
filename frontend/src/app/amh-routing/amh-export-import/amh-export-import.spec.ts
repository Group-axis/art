import {
  it,
  inject,
  injectAsync,
  describe,
  beforeEachProviders
} from 'angular2/testing';

import {Component, provide} from 'angular2/core';

// Load the implementations that should be tested
import {AMHExportImportComponent} from './amh-export-import.component';

describe('AMHExportImport', () => {
  // provide our implementations or mocks to the dependency injector
  beforeEachProviders(() => [
    AMHExportImportComponent
  ]);

  it('should log ngOnInit', inject([ AMHExportImportComponent ], (amhExportImportComponent) => {
    spyOn(console, 'log');
    expect(console.log).not.toHaveBeenCalled();

    amhExportImportComponent.ngOnInit();
    expect(console.log).toHaveBeenCalled();
  }));

});
