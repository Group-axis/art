export class SimulationJob {
    public id : number;
	public user : string;
	public creationDate : Date;
	public startDate : Date;
	public endDate : Date;
	public status : number;
	public numOfMessages : number;
    public fileName : string;
    public comment : string;
    public params : string;
    public output : string;
    public outputAsArray : Array<string>;
    public jobLauncherSystem : number;

    public static SAA_SYSTEM_ID = 1;
    public static AMH_SYSTEM_ID = 2;

    constructor(
        id : number, user : string, creationDate : Date, startDate : Date, endDate : Date, status : number,
        numOfMessages : number, fileName : string, comment : string, params : string, output : string, 
        outputAsArray? : Array<string>, jobLauncherSystem : number = SimulationJob.AMH_SYSTEM_ID) {
        this.id = id;
        this.user = user;
        this.creationDate = creationDate;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.numOfMessages = numOfMessages;
        this.fileName = fileName;
        this.comment = comment;
        this.params = params;
        this.output = output;
        this.outputAsArray = outputAsArray;
        this.jobLauncherSystem = jobLauncherSystem;
    }

    public static fromJson(json : any) {
        return new SimulationJob(json.id, json.user, json.creationDate, json.startDate, json.endDate, json.status,
        json.numOfMessages, json.fileName, json.comment, json.params, json.output, json.outputAsArray, json.jobLauncherSystem);
    }

    public toWriteModel(status : number = 1) {
         let isUserArray = Array.isArray(this.user);
         //isUserArray ? this.user[0] : this.user,
        return {
            user : this.user,
            status : this.status || status,
            numOfMessages : this.numOfMessages,
            jobLauncher : this.jobLauncherSystem,
            params : this.params
        }
    }

}



