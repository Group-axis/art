import { Injectable, Component } from 'angular2/core';
import { Observable } from 'rxjs/Observable';
import 'rxjs/add/operator/share';
import {Config} from './';
import { Logger } from './log.component';
import {User} from '../../../models/users';


@Injectable()
export class FileUploadService {
    /**
     * @param Observable<number>
     */
    private progress$: Observable<number>;

    /**
     * @type {number}
     */
    private progress: number = 0;

    private progressObserver: any;

    // constructor() {
    //     this.logger.log("creating FileUpdloadService")
    //     this.progress$ = new Observable<number>(observer => {
    //         this.progressObserver = observer
    //     });
    // }

    constructor (private logger : Logger) {
             this.logger.log("creating FileUpdloadService")
    this.progress$ = Observable.create(observer => {
        this.progressObserver = observer
    }).share();
  }

    /**
     * @returns {Observable<number>}
     */
    public getObserver(): Observable<number> {
        return this.progress$;
    }

    /**
     * Upload files through XMLHttpRequest
     *
     * @param url
     * @param files
     * @returns {Promise<T>}
     */
    public upload(url: string, files: File[], extraParams? : Map<string, string>, interval? : number): Promise<any> {
        return new Promise((resolve, reject) => {
            let formData: FormData = new FormData();
            let xhr: XMLHttpRequest = new XMLHttpRequest();

            if (files.length == 0) {
                this.logger.log("No file chosen");
                reject("No file chosen");
            } else {

                //  this.logger.log("adding file "+this.config.get('clientImportDir') + files[files.length - 1].name + " to formData");
                //  formData.append("file", files[files.length - 1], this.config.get('clientImportDir') + files[files.length - 1].name);
                 this.logger.log("adding file "+"/amhImports/" + files[files.length - 1].name + " to formData");
                 formData.append("file", files[files.length - 1], "/amhImports/" + files[files.length - 1].name);
                 formData.append("env", "UNKNOWN");
                 formData.append("ver", "DEFAULT");
                 if (extraParams) {
                     extraParams.forEach((v,k) => {
                         this.logger.debug("Adding parameter ("+k+"->"+v+") ");
                         formData.append(k, v);
                     })
                 }

                 xhr.ontimeout = function () {
                    console.error("The request for " + url + " timed out after : "+this.timeout+" ms");
                 };

                xhr.onreadystatechange = () => {
                    if (xhr.readyState === 4) {
                        if (xhr.status >= 200 && xhr.status < 300) {
                            resolve(JSON.parse(xhr.response));
                        } else {
                            reject(xhr.response);
                        }
                    }
                };

    //            FileUploadService.setUploadUpdateInterval(interval || 500);

                xhr.upload.onprogress = (event) => {
                    this.progress = Math.round(event.loaded / event.total * 100);

                    this.progressObserver.next(this.progress);
                };

                xhr.open('POST', url, true);
                this.logger.log("upload service about to sending to " + url);
                xhr.send(formData);
            }
        });
    }

    public upload2(url: string, files: File[], extraParams? : Map<string, string>, interval? : number): Observable<any> {
         return Observable.create(observer => {
        let formData: FormData = new FormData();
        let xhr: XMLHttpRequest = new XMLHttpRequest();

        // for(let elt in xhr) {
        //   this.logger.log('-elt = '+elt);
        // }
        this.logger.log(xhr);

if (files.length == 0) {
                this.logger.log("No file chosen");
                observer.error("No file chosen");
            } else {

                //  this.logger.log("adding file "+this.config.get('clientImportDir') + files[files.length - 1].name + " to formData");
                //  formData.append("file", files[files.length - 1], this.config.get('clientImportDir') + files[files.length - 1].name);
                 this.logger.log("adding file "+"/amhImports/" + files[files.length - 1].name + " to formData");
                 formData.append("file", files[files.length - 1], "/amhImports/" + files[files.length - 1].name);
                 formData.append("env", "UNKNOWN");
                 formData.append("ver", "DEFAULT");
                 if (extraParams) {
                     extraParams.forEach((v,k) => {
                         this.logger.debug("Adding parameter ("+k+"->"+v+") ");
                         formData.append(k, v);
                     })
                 }


        xhr.onreadystatechange = () => {
            if (xhr.readyState === 4) {
                if (xhr.status === 201) {
                    //observer.next(JSON.parse(xhr.response));
                    observer.complete();
                } else {
                    observer.error(xhr.response);
                }
            }
        };

        xhr.onprogress = (event) => {
            this.logger.log('onupload');
            this.logger.log(event);
            this.progress = Math.round(event.loaded / event.total * 100);

            this.progressObserver.next(this.progress);
        };

        xhr.open('POST', url, true);
        this.logger.log("upload service about to sending to " + url);
        xhr.send(formData);

        // xhr.onprogress = (event) => {
        //     this.progress = event.loaded;

        //     this.progressObserver.next(this.progress);
        // };

        // xhr.open('GET', url, true);
        // xhr.send()
            };
    });
    }

    /**
     * Set interval for frequency with which Observable inside Promise will share data with subscribers.
     *
     * @param interval
     */
    private static setUploadUpdateInterval(interval: number): void {
        setInterval(() => { }, interval);
    }
}