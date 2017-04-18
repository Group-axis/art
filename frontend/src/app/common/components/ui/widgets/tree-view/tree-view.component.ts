import {  Injectable, Provider, ContentChild, Host, Component, Directive, ElementRef, Renderer, Input, Output, EventEmitter, provide} from '@angular/core';
import { NG_VALUE_ACCESSOR, ControlValueAccessor, CORE_DIRECTIVES, NgClass, NgFormControl, NgForm, NG_VALIDATORS} from '@angular/common';
//import {Component, Directive, Input, Output, EventEmitter} from '@angular/core';
import {TreeNode} from './tree-view-node.model';
// import {NgClass, ControlValueAccessor} from '@angular/common';
import {TreeSelectionService} from "./tree.service";
import {forwardRef} from '@angular/core/src/di';
import {Logger} from "../../../services";


@Component({
    selector: 'tree-view',
    template: require('./tree-view.html'),
    directives: [TreeView, NgClass],
    events:['buy']
})
export class TreeView {
    @Input() nodes: Array<TreeNode>;

     buy: EventEmitter <string> = new EventEmitter<string>();

    private selectedNode: TreeNode = new TreeNode("", "", "");

    constructor(private treeSelectionService: TreeSelectionService, private logger : Logger) {}

    private nodeSelected(node: TreeNode) {
        this.buy.emit(node.code);
        this.selectedNode = node;
        this.treeSelectionService.selectionDone(this.selectedNode.code);
        this.logger.log("nodeSelection emitted " + this.selectedNode.code);
    }

    selectedCode() {
        this.logger.log("treeView::selectedCode called, returning : " + this.selectedNode.code);
        return this.selectedNode.code;
    }
    
    writeNodeValue(v : string) {
        this.logger.log("writeNodeValue "+ v);
    }
}

const CUSTOM_VALUE_ACCESSOR = new Provider(NG_VALUE_ACCESSOR, {useExisting: forwardRef(() => TreeViewAccessor), multi: true});

@Directive({
  selector: 'tree-view',
  host: {'(buy)': 'onChange($event)'},
  providers: [CUSTOM_VALUE_ACCESSOR]
})
export class TreeViewAccessor implements ControlValueAccessor {
  onChange = (_) => {};
  onTouched = () => {};
      
  constructor(private host: TreeView, private logger : Logger) {

  }

  writeValue(value: any): void {
      this.logger.log("writing...");
    this.host.writeNodeValue(value);
  }

  registerOnChange(fn: (_: any) => void): void { this.onChange = fn; }
  registerOnTouched(fn: () => void): void { this.onTouched = fn; }
}