import {Component, ViewChild} from '@angular/core';
import {Control, NgFormControl} from '@angular/common';
import {DataTableDirectives} from 'angular2-datatable/datatable';
import {SAARoutingService} from "../saa-routing.service";
import {Point, PointList} from "../../models/routing";
import {Logger} from '../../common/components/services';
import {Observable} from 'rxjs/Observable';
import {Option} from '../../models/referential/option';
import {MenuConfig} from '../../models/menu';
import {HeaderSecondary} from '../../header-secondary';
import { RouteParams} from 'angular2/router';
import { Permissions, NotPermissions } from '../../common/directives';
import { MultiSelectionComponent } from '../../common/components/ui/widgets/multi-selection';
import 'rxjs/add/operator/debounceTime';
import 'rxjs/add/operator/switchMap';
// import 'rxjs/add/operator/map';

// console.log('`SAA Point Overview` component loaded asynchronously');

@Component({
  selector: 'saa-points-overview',
  providers: [SAARoutingService],
  styles: [`
    h1 {
      font-family: Arial, Helvetica, sans-serif
    }
  `],
  template: require('./saa-points-overview.html'),
  directives: [DataTableDirectives, HeaderSecondary, NgFormControl, Permissions, NotPermissions, MultiSelectionComponent]
})
export class SAAPointsOverviewComponent {
  @ViewChild("filterText") filterValue;
  private searchInput : Control;
  private data: Array<PointList> = [];
  private menuConfig: Array<MenuConfig> = [
    new MenuConfig("fa fa-home", "/home", "Home"),
    new MenuConfig("fa fa-sitemap", "/saa-routing", "SAA Routing"),
    new MenuConfig("fa fa-list", "", "Points Overview")
  ];
  private selectionOptions: Array<Option> = [];
  private defaultOption : Option;
  private selectedOptions: Array<Option> = [];

  constructor(routeParams: RouteParams, private saaRoutingService: SAARoutingService, private logger : Logger) {
    saaRoutingService.findSchemas().subscribe(
          data => {
            this.logger.debug("adding schemas "+JSON.stringify(data));
            this.defaultOption = new Option(1, "Without Schema","", false);
            this.selectionOptions =  [this.defaultOption, ...data];
           // this.loadAssignments(this.defaultOption);
          }
        );

    this.searchInput = new Control('');
    let temp = [];
    let tt = "";
    
    this.searchInput.valueChanges
      .debounceTime(50)
      .switchMap(filterText => { tt = filterText ; return this.saaRoutingService.findPointMatches(this.selectedOptions.map(o => o.code), filterText); })
      .subscribe(
      data => {
        this.data = this.changeFilter(data, { filtering: { filterString: tt, columnName: "pointName" } });
        this.logger.debug("response from findPointMatches with text "+tt + " size "+this.data.length);
      },
      err =>
        console.error("Can't get points. Error code: %s, URL: %s ", err.status, err.url),
      () => {
        // this.data = temp;
        this.logger.debug(temp.length + ' point(s) are retrieved from ES with text '+ tt);
        this.logger.log(this.data.length + ' point(s) are retrieved');
        temp = [];
      }
    );

    this.loadPoints();
  }


  ngOnInit() {
    this.logger.log('hello `SAA points overview` component');
    // Get the data from the server
  }


  private loadPoints() {

    this.saaRoutingService.findPointMatches(this.selectedOptions.map(o => o.code)).subscribe(
      data => {
        this.data = data;
      },
      err =>
        console.error("Can't get points. Error code: %s, URL: %s ", err.status, err.url),
      () => {
         this.logger.log(this.data.length + ' points(s) are retrieved');
      }
    );

  }


  private changeFilter(data: any, config: any): any {
    if (!config.filtering) {
      return data;
    }

    let valueToFind = config.filtering.filterString.toUpperCase();
    let filteredPointName: Array<any> = data.filter((item: any) =>
      item[config.filtering.columnName].toUpperCase().match(valueToFind));

    let filteredRuleDescription: Array<any> = data.filter((item: any) =>
      (item["ruleDescription"] || "").toUpperCase().match(valueToFind));

    let filteredRuleMessage: Array<any> = data.filter((item: any) =>
      (item["ruleMessage"] || "").toUpperCase().match(valueToFind));

    let filteredData = filteredPointName.concat(filteredRuleDescription).concat(filteredRuleMessage);

    let uniqueList: Array<any> = [];
    filteredData.forEach(item => {
      let found = uniqueList.find((value, index, array) => {
        return item["pointName"] == value["pointName"] && item["ruleDescription"] == value["ruleDescription"];
      });
      if (!found) {
        uniqueList.push(item);
      }
    });

    return uniqueList;
  }

  actionUpdateSelection(selectedOptions: Option[]) {
      this.logger.log(JSON.stringify(selectedOptions) + ' received');
      this.selectedOptions = selectedOptions;
      let text = this.searchInput.value;
      this.saaRoutingService.findPointMatches(this.selectedOptions.map(o => o.code), text)
      .subscribe(
      data => {
        this.data = this.changeFilter(data, { filtering: { filterString: text, columnName: "pointName" } });
        this.logger.debug("response from findPointMatches with text "+ text + " size "+this.data.length);
      },
      err =>
        console.error("Can't get points. Error code: %s, URL: %s ", err.status, err.url),
      () => {
        // this.data = temp;
        // this.logger.debug(temp.length + ' point(s) are retrieved from ES with text '+ tt);
        this.logger.log(this.data.length + ' point(s) are retrieved');
        // temp = [];
      }
    );
  }

}
