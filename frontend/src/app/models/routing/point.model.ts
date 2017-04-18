import {Rule} from './rule.model';

export class Point {
  public pointName: string;
  public full: boolean;
  public rules: Rule[];
  
  constructor(pointName: string, full: boolean, rules: Rule[]) {
    this.pointName = pointName;
    this.full = full;
    this.rules = rules;
  }
}