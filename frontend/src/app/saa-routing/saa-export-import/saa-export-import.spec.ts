import {
  it,
  inject,
  injectAsync,
  describe,
  beforeEachProviders
} from 'angular2/testing';

import {Component, provide} from 'angular2/core';

// Load the implementations that should be tested
import {AMHExportImportComponent} from './saa-export-import.component';

describe('SAAExportImport', () => {
  // provide our implementations or mocks to the dependency injector
  beforeEachProviders(() => [
    AMHExportImportComponent
  ]);

  it('should log ngOnInit', inject([ SAAExportImportComponent ], (saaExportImportComponent) => {
    spyOn(console, 'log');
    expect(console.log).not.toHaveBeenCalled();

    saaExportImportComponent.ngOnInit();
    expect(console.log).toHaveBeenCalled();
  }));

});
