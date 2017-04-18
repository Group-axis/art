import {forwardRef} from '@angular/core/src/di';
import {  Component, Input, Output, ViewChild, Provider, Directive, EventEmitter} from 'angular2/core';
import { CORE_DIRECTIVES, NG_VALUE_ACCESSOR, ControlValueAccessor} from '@angular/common';
import {DataTableDirectives} from 'angular2-datatable/datatable';


class toto {
    active:boolean;
    backSequence : string;
    backCode : string;
    ruleSequence: string;
    quinto:string;
    constructor(active:boolean, backSequence : string, backCode : string, ruleSequence: string, quinto:string) {
        this.active =   active;
    this.backSequence = backSequence;
    this.backCode = backCode;
    this.ruleSequence=ruleSequence;
    this.quinto=quinto;
    }
}

@Component({
    selector: 'gp-selection-table',
    template: require('./selection-table.html'),
    directives: [CORE_DIRECTIVES, DataTableDirectives],
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
export class SelectionTableComponent {
    private data : Array<toto> = [
    new toto(true, "BNP_1", "BACK_1", "DISTRIBUTION","LAST"),
    new toto(true, "BNP_1", "BACK_1", "DISTRIBUTION","LAST"),
    new toto(true, "BNP_1", "BACK_1", "DISTRIBUTION","LAST"),
    new toto(true, "BNP_1", "BACK_1", "DISTRIBUTION","LAST"),
    new toto(true, "BNP_2", "BACK_2", "ROUTING","GAIA"),
    new toto(true, "BNP_2", "BACK_2", "ROUTING","GAIA"),
    new toto(true, "BNP_2", "BACK_2", "ROUTING","GAIA"),
    new toto(true, "BNP_2", "BACK_2", "ROUTING","GAIA"),
    new toto(true, "BNP_2", "BACK_2", "ROUTING","GAIA"),
    new toto(true, "BNP_2", "BACK_2", "ROUTING","GAIA"),
    new toto(true, "BNP_3", "BACK_3", "ROUTING","GAIA"),
    new toto(true, "BNP_3", "BACK_3", "ROUTING","GAIA"),
    new toto(true, "BNP_3", "BACK_3", "ROUTING","GAIA"),
    new toto(true, "BNP_3", "BACK_3", "ROUTING","GAIA"),
    new toto(true, "BNP_3", "BACK_3", "ROUTING","GAIA"),
    new toto(true, "BNP_3", "BACK_3", "ROUTING","GAIA")];
    
    private config = {
        "items": [
            { "width": 25, "fieldName": "active", "description": "Active", "pipes":"json" }, 
            { "width": 25, "fieldName": "backSequence", "description": "Back SEquence" , "pipes":"json"}, 
            { "width": 20, "fieldName": "backCode", "description": "BACK CODE" , "pipes":"json"}, 
            { "width": 20, "fieldName": "ruleSequence", "description": "RULE SEQUENCE" },
            { "width": 10, "fieldName": "quinto", "description": "QUINTO" , "pipes":"json"}]
    }
}