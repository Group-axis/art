// App
export * from './app.component';
export * from './app.service';

import {AppState} from './app.service';
import { Config, Auth, Store, FileDownloader, FileUploadService } from './common/components/services';
// Application wide providers
export const APP_PROVIDERS = [
   AppState,  Store, Config, Auth, FileDownloader, FileUploadService
];
