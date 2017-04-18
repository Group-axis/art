import {forwardRef} from '@angular/core/src/di';
import {  Component, Input, Output, ViewChild, Provider, Directive, EventEmitter, OnChanges, SimpleChange} from 'angular2/core';
import { CORE_DIRECTIVES, NG_VALUE_ACCESSOR, ControlValueAccessor} from '@angular/common';
import {IText, IdCodeDescription} from '../../../../../models/referential';
import {Logger} from "../../../services";

@Component({
  selector: 'gp-pick-list',
  template: require('./form-pick-list.html'),
  directives:[CORE_DIRECTIVES],
  styles: [
    `.pickListButtons {
       padding: 10px;
       text-align: center;
     }`
    ,
    `.pickListSelect {
        height: 200px !important;
     }`
     ,
    `.pickListButtons button {
        margin-bottom: 5px;
        font-size:20px;
     }`
  ]
})
export class FormPickListComponent implements OnChanges {
  @ViewChild('selectIn') private selectInElRef;
  private selectedInValues: Object[];
  
  @Input()
  private optionsIn: Array<IdCodeDescription>;
  
  @Input()
  private active : boolean;
  
  @ViewChild('selectOut') private selectOutElRef;
  private selectedOutValues: Object[];
  
  @Input()
  private labelIn: string;
  
  @Input()
  private optionsOut: Array<IdCodeDescription>;
  
  @Input()
  private labelOut: string;
  
  @Output()
  private listChange : EventEmitter<Array<IdCodeDescription>>;

  private optionsMap : Map<string, IdCodeDescription> = undefined;
  
  constructor(private logger : Logger) {
      this.selectedInValues = [];
      this.selectedOutValues = [];
      this.listChange = new EventEmitter<Array<IdCodeDescription>>();
      this.active = true;
  }
  
  /* SimpleChange
      previousValue: any;
      currentValue: any;
*/
  ngOnChanges(changes: { [propertyName: string]: SimpleChange }) {
      this.logger.debug("detected " + JSON.stringify(changes));
    if (changes["optionsIn"]) {
      this.logger.debug("this.optionsIn "+ this.optionsIn + " changes "+ JSON.stringify(this.optionsIn));
    this.optionsMap = this.fromListToMap(this.optionsIn);
    this.logger.debug("optionMap filled "+ JSON.stringify(this.optionsMap));
    }
  }

  private fromListToMap(input :Array<IdCodeDescription>) : Map<string, IdCodeDescription> {
      return input.reduce((acc, R) => { acc.set(R.htmlText, R); this.logger.debug("returning acc "+ JSON.stringify(R.htmlText)); return acc; }, new Map<string, IdCodeDescription>());
  }
//    ngAfterViewInit() {
    // this.updateSelectLists(this.selectInElRef.nativeElement.options, this.selectedInValues);
    // this.updateSelectLists(this.selectOutElRef.nativeElement.options, this.selectedOutValues);
    // }
  
//   updateSelectLists(options, selectedValues) {
//     // let options = this.selectInElRef.nativeElement.options;
//     for(let i=0; i < options.length; i++) {
//       options[i].selected = selectedValues.indexOf(options[i].value) > -1;
//     }
//   }
  
  public changeIn(options) {
      this.selectedInValues = this.change(options);
  }
  
  public changeOut(options) {
      this.selectedOutValues = this.change(options);
  }
  
  private change(options) {
    return Array.apply(null,options).filter(option => option.selected);
  }
  
  add() {
      let inOptions = this.selectInElRef.nativeElement.options;
      let outOptions = this.selectOutElRef.nativeElement.options;
      
      this.moveSelectedOptions(inOptions, outOptions, this.selectedInValues);
      
      this.initializeOptions();
      
      this.listChangeEmit();
  }

  remove() {
      let inOptions = this.selectInElRef.nativeElement.options;
      let outOptions = this.selectOutElRef.nativeElement.options;
      
      this.moveSelectedOptions(outOptions, inOptions, this.selectedOutValues);
      
      this.initializeOptions();
      
      this.listChangeEmit();
  }
  
  addAll() {
      this.moveAllOptions(this.selectInElRef.nativeElement.options, this.selectOutElRef.nativeElement.options);
      
      this.initializeOptions();
      
      this.listChangeEmit();
  }
  
  removeAll() {
      this.moveAllOptions(this.selectOutElRef.nativeElement.options, this.selectInElRef.nativeElement.options);
      
      this.initializeOptions();
      
      this.listChangeEmit();
  }
  
  public writeListValue(outOptions:Array<IdCodeDescription>) {
      this.optionsOut = outOptions;
      if (!this.optionsMap) {
           this.optionsMap = new Map<string, IdCodeDescription>(); 
      }
      if (this.optionsOut) {
        this.fromListToMap(Array.apply(null,this.optionsOut).map(item => new IdCodeDescription(item["id"], item["code"], item["description"])))
        .forEach((item, key, mapObj) => this.optionsMap.set(key, item));
        
        this.logger.debug("writing optionsOut "+JSON.stringify(this.optionsOut));
      }

  }
  
  private toFalse = (option: HTMLOptionElement)  => { option.selected = false; };
  
  private initializeOptions() {
      Array.apply(null, this.selectInElRef.nativeElement.options).forEach( this.toFalse );
      Array.apply(null, this.selectOutElRef.nativeElement.options).forEach( this.toFalse );
      
      this.selectedInValues=[];
      this.selectedOutValues=[];
  }
  
  
  private moveSelectedOptions(fromOptions, targetOptions, selectedFrom) {
      
      var addSelectedFromToTargetOptions = [targetOptions.length, 0].concat(selectedFrom);
      Array.prototype.splice.apply(targetOptions, addSelectedFromToTargetOptions);

      fromOptions = Array.apply(null, fromOptions).filter( function(option) {
         return selectedFrom.indexOf(option) === -1;
      });
      
  }
  
  private moveAllOptions(fromOptions, targetOptions) {
      this.moveSelectedOptions(fromOptions, targetOptions, this.selectedOptions(fromOptions));
  }

  private selectedOptions(fromOptions) {
      let selectedOptions = [];
      
      Array.apply(null, fromOptions).forEach(option => {
          selectedOptions.push(option);
      });
      
      return selectedOptions;
  }

  private listChangeEmit() {
      let selection : Array<IdCodeDescription> = this.selectedOptions(this.selectOutElRef.nativeElement.options).map( option => { return this.optionsMap.get(option.text); } );
      this.logger.debug("emitting selection "+JSON.stringify(selection));
      this.listChange.emit(selection);
  }
}

const CUSTOM_VALUE_ACCESSOR = new Provider(NG_VALUE_ACCESSOR, {useExisting: forwardRef(() => FormPickListValueAccessor), multi: true});

@Directive({
  selector: 'gp-pick-list',
  host: {'(listChange)': 'onChange($event)'},
  providers: [CUSTOM_VALUE_ACCESSOR]
})
export class FormPickListValueAccessor implements ControlValueAccessor {
  onChange = (_) => {};
  onTouched = () => {};
      
  constructor(private host: FormPickListComponent, private logger : Logger) {

  }

  writeValue(value: any): void {
      this.logger.log("writing into pickLists..."+value);
    this.host.writeListValue(value);
  }

  registerOnChange(fn: (_: any) => void): void { this.onChange = fn; }
  registerOnTouched(fn: () => void): void { this.onTouched = fn; }
}

export function listRequired(control) {
    
  if( control.value == null || 
      control.value.length === 0) {
    return {
      listRequired: true
    }
  }

  return null;
}