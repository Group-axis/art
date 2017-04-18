import {Injectable} from 'angular2/core';
import {BrowserXhr} from 'angular2/http';
import { Logger } from './log.component';
let fileSaver = require('../../../../../node_modules/file-saver/FileSaver.js');

@Injectable()
export class FileDownloader {
    public pending: boolean = false;

    constructor() { }

    public download(url: string, fileName: string) {

        // Xhr creates new context so we need to create reference to this
        let self = this;

        // Status flag used in the template.
        this.pending = true;

        // Create the Xhr request object
        let xhr = new XMLHttpRequest();
        // let url =  `/api/pdf/iticket/${fileName}?lang=en`;
        xhr.open('GET', url, true);
        xhr.responseType = 'blob';

        // Xhr callback when we get a result back
        // We are not using arrow function because we need the 'this' context
        xhr.onreadystatechange = function () {

            // We use setTimeout to trigger change detection in Zones
            setTimeout(() => { self.pending = false; }, 0);

            // If we get an HTTP status OK (200), save the file using fileSaver
            if (xhr.readyState === 4 && xhr.status === 200) {
                var blob = new Blob([this.response], { type: 'application/octet-stream' });
                fileSaver.saveAs(blob, fileName);
            }
        };

        // Start the Ajax request
        xhr.send();
    }
}