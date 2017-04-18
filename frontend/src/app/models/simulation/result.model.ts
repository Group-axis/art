export class SimulationResult {
    public selectionSequence : number;
	public selectionCode : string;
	public ruleSequence : number;
	public ruleName : string;
	public ruleExpression : string;
	public backendSequence : number;
	public backendName : string;
    public selectionType : string;
    public messageReference : string;
    public hit : boolean;
    public withError : boolean;
    public expressionToEvaluate : string;
    public errorMsg : string;
    public paramsSize : number;
    public messageName : string;
    public messageNameHit : string;
    public status : number;
    public selected : boolean;
    
    constructor(selectionSequence : number, selectionCode : string, ruleSequence : number, ruleName : string, ruleExpression : string,
                backendSequence : number, backendName, selectionType : string, messageReference : string) {
        this.selectionSequence = selectionSequence;
        this.selectionCode = selectionCode;
        this.ruleSequence = ruleSequence;
        this.ruleName = ruleName;
        this.ruleExpression = ruleExpression;
        this.backendSequence = backendSequence;
        this.backendName = backendName;
        this.selectionType = selectionType;
        this.messageReference = messageReference;
    }


    public toWriteModel() {
       return {"fileName":this.messageNameHit,
            "selectionSequence":this.selectionSequence,
            "selectionCode":this.selectionCode,
            "ruleSequence":this.ruleSequence,
            "ruleName":this.ruleName,
            "ruleExpression":this.ruleExpression,
            "backendSequences":"",
            "backendNames":this.backendName,
            "selectionType": this.selectionType,
            "messageReference" : this.messageReference
    }
    }

    public clone() : SimulationResult {
        let newSimuResult =  new SimulationResult(this.selectionSequence, this.selectionCode, this.ruleSequence, this.ruleName, this.ruleExpression, 
                    this.backendSequence, this.backendName, this.selectionType, this.messageReference);
        newSimuResult.messageNameHit = this.messageNameHit;
        newSimuResult.messageName = this.messageName;
        newSimuResult.hit = this.hit;
        newSimuResult.withError = this.withError;
        newSimuResult.errorMsg = this.errorMsg;
        return newSimuResult;
    }

}



