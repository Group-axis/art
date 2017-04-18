import {Component,  EventEmitter, Output, Input} from 'angular2/core';
import {Option} from "../../models/referential/option";
import {Logger} from '../../common/components/services';

//this.logger.log('`AMHSelectionTable` component loaded asynchronously');

@Component({
  selector: 'amh-selection-table',
  template: require('./amh-selection-table.html')
})
export class AMHSelectionTableComponent {
  @Input("default-option") optionSelected : Option;
  @Input() public options : Array<Option>;
  @Input() public label : string;
  @Output() public tableSelected:EventEmitter<any> = new EventEmitter();

  constructor( private logger : Logger) { }

  ngOnInit() {
    this.logger.log('hello `AMH selection table` component');
  }

  private selectOption(index : number) {
    this.logger.log("option selected " + this.options[index].description );
    this.tableSelected.emit(this.options[index]);
  }
  
}
