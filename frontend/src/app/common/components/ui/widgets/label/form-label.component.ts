import {  Component, Input } from 'angular2/core';
@Component({
  selector: 'gp-label',
  template: require('./form-label.html'),
  styles: [
    `.glyphicon-refresh-animate {
      -animation: spin .7s infinite linear;
      -webkit-animation: spin2 .7s infinite linear;
    }`,
    `@-webkit-keyframes spin2 {
       from { -webkit-transform: rotate(0deg);}
      to { -webkit-transform: rotate(360deg);}
    }`
  ]
})
export class FormLabelComponent {
  @Input()
  label: string;

  @Input()
  text: string;

  constructor() {
  }

}