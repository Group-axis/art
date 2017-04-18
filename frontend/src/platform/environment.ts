
// Angular 2
import { enableProdMode } from '@angular/core';
//, REFERENTIAL_PROVIDERS
import {LOGGING_PROVIDERS} from '../app/common/components/services';
// Environment Providers
let PROVIDERS = [];

if ('production' === ENV) {
  // Production
  enableProdMode();

  PROVIDERS = [
    ...PROVIDERS,
    ...LOGGING_PROVIDERS //, ...REFERENTIAL_PROVIDERS
  ];

} else {
  // Development
  PROVIDERS = [
    ...PROVIDERS,
    ...LOGGING_PROVIDERS //, ...REFERENTIAL_PROVIDERS
  ];

}


export const ENV_PROVIDERS = [
  ...PROVIDERS
];
