import { ElementRef, Component, Input, Output, ViewChild, EventEmitter} from 'angular2/core';
import { CORE_DIRECTIVES} from '@angular/common';
import {Option} from '../../../../../models/referential/option';

@Component({
    selector: 'gp-multi-selection',
    template: require('./multi-selection.html'),
    directives: [CORE_DIRECTIVES],
    host: {
      '(document:click)': 'handleClick($event)',
    }
})
export class MultiSelectionComponent {
    @ViewChild('select') selectElRef;
  
  @Input("options") inputOptions:Option[]=[];
  @Output("optionSelected") seletionEmitter : EventEmitter<Option[]> = new EventEmitter<Option[]>();
  selected:any[]=[];
  @Input("noSelectedMessage")
  private defaultTitle : string = "Select Items";

  private showMenu : boolean = false;
  private selectionTitle : string = "";

  constructor( private elementRef: ElementRef) {  
  }

  ngOnInit() {
    this.selectionTitle = this.defaultTitle;

  }

  handleClick(event) {
    var clickedComponent = event.target;
    var inside = false;
    do {
      if (clickedComponent === this.elementRef.nativeElement) {
        inside = true;
      }
      clickedComponent = clickedComponent.parentNode;
    } while (clickedComponent);

    if (!inside) {
      this.showMenu = false;
    }
  }

actionToggleMultiSelect(event, val){
    event.preventDefault();
    if(this.selected.indexOf(val) == -1){
      this.selected = [...this.selected, val];
      val.selected=true;
    }else{
    val.selected=false;
      this.selected = this.selected.filter(function(elem){
        return elem != val;
      })
    }
    this.selectionTitle = this.selected.length == 0 ? this.defaultTitle : this.selected.map(option => option.code).join(', ');
    this.seletionEmitter.emit(this.selected);
  }
}