import {IdCodeDescription} from '../referential';
import {updateFields} from '../referential';


export class Condition {
  public conditionOn: IdCodeDescription;
  public functions: IdCodeDescription[];
  public message: string;

  constructor(conditionOn?: IdCodeDescription, functions?: IdCodeDescription[], message?: string) {
    this.conditionOn = conditionOn;
    this.functions = functions;
    this.message = message;
  }

  static empty(): Condition {
    return new Condition(IdCodeDescription.empty(), [], "");
  }

  public update(that: Condition) {
    updateFields(this, that, ["conditionOn", "functions", "message"]);
  }

  public toWriteModel(): any {
    let functionsWriteModel = undefined;

    if (this.functions && this.functions.length > 0) {
       functionsWriteModel = this.functions.reduce(
                (acc, cur) => {
                  return acc.length > 0 ? acc + "," + cur.description : cur.description;
                }, "");
    }

    let conditionOn = this.conditionOn.id == 1 ? "MESSAGE" : this.conditionOn.id == 2 ? "FUNCTION" : "MESSAGE_AND_FUNCTION";
    
    return {
      "conditionOn": conditionOn,
      "criteria": this.conditionOn.id != 2 ? this.message : "",
      "functionList": this.conditionOn.id != 1 ? functionsWriteModel : ""
    };
  }
}