// import {CONST_EXPR} from 'angular2/src/facade/lang';
import {forwardRef} from '@angular/core/src/di';
import {  Component, Input, Output, ViewChild, Provider, Directive, EventEmitter, OnInit, SimpleChange,  OnChanges} from 'angular2/core';
import { CORE_DIRECTIVES, NG_VALUE_ACCESSOR, ControlValueAccessor} from '@angular/common';
import {IOption} from '../../../../../models/referential';
import {Logger} from "../../../services";

@Component({
  selector: 'gp-combo-duo',
  template: require('./form-combo-duo.html'),
  directives:[CORE_DIRECTIVES],
  styles: [
    `seleceet {
      margin: 5px 5px 5px 5px;
      border: 1px solid #111;
   background: transparent;
    width: 250px;
   padding: 5px 0px 5px 0px;
   font-size: 16px;
   border: 1px solid #ccc;
   height: 34px;
   /*background: #FAFEFF;*/
    }`
    ,
    // `.combo-select {
    //    display: block;
    //     width: 65%; 
    //     height: 34px;
    //     padding: 6px 15px;
    //     margin: 5px 5px;
    //     font-size: 14px;
    //     line-height: 1.428571429;
    //     color: #8e8e93;
    //     vertical-align: middle;
    //     background-color: #ffffff;
    //     border: 1px solid #c7c7cc;
    //     border-radius: 4px;
    //     -webkit-transition: border-color ease-in-out .15s, box-shadow ease-in-out .15s;
    //     transition: border-color ease-in-out .15s, box-shadow ease-in-out .15s;
    // }`
    // ,
    // `.combo-select:disabled {
    //    cursor: not-allowed;
    // background-color: #f7f7f7;
    // }`
    // ,
    `@media screen and (min-width:0\0) { 
        select {
            background:none;
            padding: 5px;
        }
    }`,
    `label {margin-right: 1px;}`
  ]
})
export class FormComboDuoComponent implements OnInit, OnChanges {
  
  @Input()
  private labelParent: string;
  
  @Input()
  private active: boolean;
  
  @Input()
  private defaultParent: string;
  
  @Input()
  private defaultChild: string;
  
  @Input()
  private labelChild: string;
  
  @Output()
  private parentChange : EventEmitter<string>;
  
  @Output()
  private childChange : EventEmitter<string>;
  
  @Output()
  private duoChange : EventEmitter<Object>;
  
  private selectedParent: IOption;
  private selectedChild: IOption;
  
  @Input()
  private options: Array<IOption> = [];
  
  private parentList: IOption[] = [];
  private childList: IOption[] = undefined;
  private parentChildrenMap : { [code: string] : IOption[] } = {};
        
  constructor(private logger : Logger) {
      this.parentChange = new EventEmitter<string>();
      this.childChange = new EventEmitter<string>();
      this.duoChange = new EventEmitter<string>();
      this.active = true;
  }
  
  ngOnChanges(changes: { [propertyName: string]: SimpleChange }) {
    if (changes["defaultParent"] && changes["defaultParent"].currentValue) {
      this.logger.debug("defaultParent from "+changes["defaultParent"].previousValue+"  to "+changes["defaultParent"].currentValue);
      this.ngOnInit()    
    }
  }

  ngOnInit() {
      
      let firstParent : string = undefined;
      let selectedParentCode = ""; 
      this.parentList = [];
      this.parentChildrenMap = {};
      if (this.options) {
      this.options.forEach((parent) => {
          if (!firstParent) {
              firstParent = parent.code;
              selectedParentCode = this.defaultParent && this.defaultParent.length > 0 ? this.defaultParent : firstParent;
          }
          this.logger.debug("parent.code "+parent.code+" selectedParentCode "+selectedParentCode);
          this.selectedParent =  parent.code == selectedParentCode ? parent : this.selectedParent;     
          this.parentList.push(parent);
          this.parentChildrenMap[parent.code] = parent.children;
      });
      
      this.logger.debug("this.defaultParent "+this.defaultParent+" this.defaultChild "+this.defaultChild);
    //   this.selectedParent={"code":selectedParentCode,"text":"","id":0, "children":[]};
      this.logger.log(" this.selectedParent id" + this.selectedParent.id + " code "+ this.selectedParent.code);
      this.childList = this.parentChildrenMap [this.selectedParent.code];
      
      this.selectedChild = this.childList.find(child => child.code == this.defaultChild);
  }
      this.logger.log("this.selectedChild "+this.selectedChild+" this.defaultChild " + this.defaultChild);
  }
  
  public changeParent(newParent : any) { //selectedIndex:number
    //   this.selectedParent = this.parentList[selectedIndex];
      this.selectedParent = newParent;
    
      this.updateChildOptions();
    //    this.logger.log("emmiting parentSelected " + this.selectedParent.value);
      this.parentChange.emit( this.selectedParent.code );
      this.emmitDuoChange();
  }
  
  public changeChild(newChild : any) {
      this.selectedChild = newChild;
    //    this.logger.log("emmiting childSelected " + this.selectedChild);
      this.childChange.emit( this.selectedChild.code );
      this.emmitDuoChange();
  }
  
  private emmitDuoChange() {
      this.logger.debug("emiting douChange with parent " + this.selectedParent.code + " child " + this.selectedChild)
      this.duoChange.emit({parentValue : this.selectedParent.code, childValue : this.selectedChild?this.selectedChild.code:""});
  }

  public writeDuoValues(duoValues:{parentValue:string, childValue:string}) {
      if (!duoValues) {
          this.logger.log("duoValues is null");
          return;
      }
      this.selectedParent = this.parentList.find(parent => parent.code == duoValues.parentValue);
      if (this.selectedParent) {
        this.childList = this.parentChildrenMap [this.selectedParent.code];
        this.selectedChild = this.childList.find(child => child.code == duoValues.childValue);
      }
      
      this.logger.log(" writing seleceted values "+ duoValues.parentValue + " childValue " + duoValues.childValue);
      // this.logger.debug(" this.selectedParent "+ this.selectedParent.code + " childValue " + this.selectedChild);
  }
  
  private updateChildOptions() {
      this.childList = this.parentChildrenMap [this.selectedParent.code];
      this.selectedChild = this.childList && this.childList.length > 0 ? this.childList[0] : undefined;
  }
  
}

const CUSTOM_VALUE_ACCESSOR = new Provider(NG_VALUE_ACCESSOR, {useExisting: forwardRef(() => FormComboDuoValueAccessor), multi: true});

@Directive({
  selector: 'gp-combo-duo',
  host: { '(duoChange)': 'onChange($event)'},
  providers: [CUSTOM_VALUE_ACCESSOR]
})
export class FormComboDuoValueAccessor implements ControlValueAccessor {
  onChange = (_) => {};
  onTouched = () => {};
      
  constructor(private host: FormComboDuoComponent, private logger : Logger) {
      this.logger.log( " constructor valueAccessor "+FormComboDuoValueAccessor);
  }

  writeValue(value: any): void {
     this.logger.log("writing into comboDuo..."+value);
     this.host.writeDuoValues(value);
  }

  registerOnChange(fn: (_: any) => void): void { this.logger.log("  on registerOnChange "+fn); this.onChange = fn; }
  registerOnTouched(fn: () => void): void { this.onTouched = fn; }
}

export function comboDuoRequired(control) {
    this.logger.debug("comboDuoRequired");
  if( !control.value || control.value.length === 0) {
    return {
      comboDuoRequired: true
    }
  }

  return null;
}