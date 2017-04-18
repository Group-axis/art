import {Condition} from './condition.model';
import {Action} from './action.model';
import {updateFields} from '../referential';

export class Rule {
  public sequence: number;
  public routingPoint: string;
  public description: string;
  public schemas: string[];
  public lastModification: Date;
  public creationDate: Date;
  public createdBy: string;
  public condition: Condition;
  public action: Action;

  constructor(sequence: number, routingPoint: string, description?: string, schemas?: string[], lastModification?: Date, creationDate?: Date, createdBy?: string, condition?: Condition, action?: Action) {
    this.sequence = sequence; 
    this.routingPoint = routingPoint;
    this.description = description;
    this.schemas = schemas;
    this.lastModification = lastModification;
    this.creationDate = creationDate;
    this.createdBy = createdBy;
    this.condition = condition;
    this.action = action; 
  }

  public static empty() : Rule {
    return new Rule(undefined, "","",[],undefined, undefined, undefined, Condition.empty(), Action.empty());
  }

  public update( that : Rule) {
    updateFields(this, that, ["sequence","routingPoint","description"
    ,"schemas","lastModification","creationDate","createdBy"]);
    this.condition.update(that.condition);
    this.action.update(that.action);
  }

  public toWriteModel(environment : string, version : string) : any {
    return {
      "full" : "true",
      "ruleDescription" : this.description,
      "environment" : environment,
      "version" : version,
      "action" : this.action.toWriteModel(),
      "condition" : this.condition.toWriteModel()
    };
  }
}