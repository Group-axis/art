package com.groupaxis.groupsuite.routing.write.domain.model.routing.rule

import java.util.Date

case class RuleAction(
  actionOn: Option[String] = None,
  instanceAction: Option[String] = None,
  instanceInterventionType: Option[String] = None,
  instanceInterventionTypeText: Option[String] = None,
  instanceRoutingCode: Option[String] = None,
  instanceTargetQueue: Option[String] = None,
  instanceUnit: Option[String] = None,
  instancePriority: Option[String] = None,
  newInstanceAction: Option[String] = None,
  newInstanceRoutingCode: Option[String] = None,
  newInstanceInterventionType: Option[String] = None,
  newInstanceInterventionTypeText: Option[String] = None,
  newInstanceTargetQueue: Option[String] = None,
  newInstanceType: Option[String] = None,
  newInstanceUnit: Option[String] = None,
  newInstancePriority: Option[String] = None)

case class RuleCondition(
  conditionOn: Option[String] = None,
  criteria: Option[String] = None,
  functionList: Option[String] = None) {
  
  def functions : Option[Seq[String]] = {
    functionList.map( value => value.split(","))
  }
}

case class RuleEntity(
    sequence: Long,
    routingPointName: String,
    full: Option[String] = Some("true"),
    ruleDescription: Option[String] = None,
    schemaMap: Option[String] = None,
    environment : String =  "UNKNOWN",
    version: String = "DEFAULT",
    action: RuleAction,
    condition: RuleCondition) {

  /*
   sequence: Long, 
    routingPointName: String, 
    actionOn: Option[String] = None, 
    conditionOn: Option[String] = None, 
    criteria: Option[String] = None, 
    full: Option[String] = Some("true"), 
    functionList: Option[String] = None, 
    instanceAction: Option[String] = None, 
    instanceInterventionType: Option[String] = None,
    instanceInterventionTypeText: Option[String] = None, 
    instanceRoutingCode: Option[String] = None, 
    instanceTargetQueue: Option[String] = None, 
    instanceUnit: Option[String] = None, 
    instancePriority: Option[String] = None, 
    newInstanceAction: Option[String] = None, 
    newInstanceRoutingCode: Option[String] = None, 
    newInstanceInterventionType: Option[String] = None, 
    newInstanceInterventionTypeText: Option[String] = None, 
    newInstanceTargetQueue: Option[String] = None, 
    newInstanceType: Option[String] = None, 
    newInstanceUnit: Option[String] = None, 
    newInstancePriority: Option[String] = None, 
    ruleDescription: Option[String] = None, 
    schemaMap: Option[String] = None 
   * */

  //	def toES(ruleEntity : RuleEntity, creationDate: Date, lastMidification: Date, createdBy:String) ue
  //  sequence: number;
  //  public routingPoint: string;
  //  public description: string;
  //  public schemas: Schema[];
  //  public lastModification: Date;
  //  public creationDate: Date;
  //  public createdBy: string;
  //  public condition: Condition;
  //  public action: Action;
  /*
   {
  "ruleId":1,
  "pointId":1,
  "description":"Rule1",
  "condition":{
    	"conditionOn":"MESSAGE", //ALWAYS,    	 
    	"functions":[
          	{
              	"id":1,
              	"description":"Cond Func 1"
            },
          	{
              "id":2,
              "description":"Cond Func 2"
            }
        ],
    	"message":"[RE234]=content!name"
  },
  "audit": {
    "lastModification":"10-05-2016",
    "createdBy":"irach.ramos",
    "creationDate":"10-05-2016",
 	 },
  "action":
  		{
         "actionOn":"NewInstanceType",
          "source":{
            	"actionOptionId":2,
            	"actionId":1,
            	"interventionText":"Intervention3",
            	"routingCode":"RoutingCode1",
            	"interventionId":3,
            	"unitId":4,
            	"priorityId":2
          		}
        }, 	
 "schemas":[
   		{
          "schemaId":1,
          "description":"Schema1"
        },
   		{
          "schemaId":2,
          "description":"Schema2",
          "lastModification":"10-05-2016",
          "createdBy":"irach.ramos",
          "creationDate":"10-05-2016"
        }
 	]
}

"condition":{
    	"conditionOn":"MESSAGE", //ALWAYS,    	 
    	"functions":[
          	{
              	"id":1,
              	"description":"Cond Func 1"
            },
          	{
              "id":2,
              "description":"Cond Func 2"
            }
        ],
    	"message":"[RE234]=content!name"
  },
conditionOn: Option[String] = None, criteria: Option[String] = None, full: Option[String] = Some("true"), functionList: Option[String] = None, 
   * */
  private def toActionDesc(value: String): IdCodeDescriptionES = {
    value.toUpperCase() match {
      case "ACTION_TYPE_NONE"          => IdCodeDescriptionES(1L, value, "None")
      case "ACTION_TYPE_ROUTING_POINT" => IdCodeDescriptionES(2L, value, "Dispose To")
      case "ACTION_TYPE_COMPLETE"      => IdCodeDescriptionES(3L, value, "Complete")
      case "ACTION_TYPE_ADDRESSEE"     => IdCodeDescriptionES(4L, value, "To addressee")
      case _                           => IdCodeDescriptionES(5L, value, "Not defined")
    }
  }

  private def toActionOnDesc(value: String): IdCodeDescriptionES = {
    value.toUpperCase() match {
      case "SOURCE"                  => IdCodeDescriptionES(1L, value, "Source")
      case "NEW_INSTANCE"            => IdCodeDescriptionES(2L, value, "New Instance")
      case "SOURCE_AND_NEW_INSTANCE" => IdCodeDescriptionES(4L, value, "Source and New Instance")
      case _                         => IdCodeDescriptionES(256L, value, "Not defined")
    }
  }
  private def toConditionDesc(value: String): IdDescriptionES = {
    value.toUpperCase() match {
      case "MESSAGE"            => IdDescriptionES(1L, value)
      case "FUNCTION"           => IdDescriptionES(2L, value)
      case "MESSAGEANDFUNCTION" => IdDescriptionES(4L, value)
      case "ALWAYS"             => IdDescriptionES(8L, value)
      case _                    => IdDescriptionES(256L, value)
    }
  }

  /*

  * */
  private def toFunctionDesc(value: String): IdDescriptionES = {
    value.toUpperCase() match {
      case "SUCCESS" => IdDescriptionES(1L, value)
      case "FAILURE" => IdDescriptionES(2L, value)
      case "INACTIVE CORRESPONDENT" => IdDescriptionES(3L, value)
      case "DISPOSITION ERROR" => IdDescriptionES(4L, value)
      case "NOT DELIVERED" => IdDescriptionES(5L, value)
      case "DELAYED DELIVERY" => IdDescriptionES(6L, value)
      case "AUTHORISATION DOES NOT ALLOW MESSAGE" => IdDescriptionES(7L, value)
      case "AUTHORISATION NOT IN VALIDITY PERIOD" => IdDescriptionES(8L, value)
      case "AUTHORISATION NOT ENABLED" => IdDescriptionES(9L, value)
      case "SIGNATURE AUTH. FAILURE" => IdDescriptionES(10L, value)
      case "NOT AUTHORISED BY RMA" => IdDescriptionES(11L, value)
      case "INVALID DIGEST" => IdDescriptionES(12L, value)
      case "INVALID SIGN DN" => IdDescriptionES(13L, value)
      case "INVALID CERTIFICATE POLICY ID" => IdDescriptionES(14L, value)
      case "FIN-COPY SERVICE BYPASSED" => IdDescriptionES(15L, value)
      case "AUTHORISATION NOT PRESENT" => IdDescriptionES(16L, value)
      case "VALIDATION ERROR" => IdDescriptionES(17L, value)
      case "NO AUTHORISATION" => IdDescriptionES(18L, value)
      case "ORIGINAL BROADCAST" => IdDescriptionES(19L, value)
      case "DELIVERED" => IdDescriptionES(20L, value)
      case "NACKED" => IdDescriptionES(21L, value)
      case "SIGNATURE VERIFICATION FAILURE" => IdDescriptionES(22L, value)
      case _ => IdDescriptionES(23L, value)
    }
  }
  private def mapType(value: Option[String]): String = {
    value.map( typeOption => {
        typeOption.toUpperCase() match {
      case "INST_NOTIFICATION_TRANSMISSION" => "INST_NOTIFICATION"
      case "INST_NOTIFICATION_INFO" => "INST_NOTIFICATION"
      case "INST_TYPE_COPY" => "INST_TYPE_COPY"
      case _ => typeOption.toUpperCase()
    }}).getOrElse("")
  }
  
  private def mapTypeOption(value: Option[String]): Option[String] = {
    value.map( typeOption => {
        typeOption.toUpperCase() match {
        case "INST_NOTIFICATION_TRANSMISSION" => "TRANSMISSION"
        case "INST_NOTIFICATION_INFO" => "INFO"
        case "INST_TYPE_COPY" => ""
        case _ => ""
    }})
  }
  
  /*
   { "text": "SARMTR", "id": 11 },
                    { "text": "EMRGZR", "id": 12 },
                    { "text": "SEDED", "id": 13 },
                    { "text": "MRTTRE", "id": 14 }
                    */

  //  private def createCondition: Option[(String, ConditionES)] = {
  //    val functions = functionList.map(value => value.split(",").toList).map(fnNameList => fnNameList.map(toFunctionDesc))
  //    conditionOn.map(value => ("condition", ConditionES(toConditionDesc(value), functions, criteria)))
  //  }
  private def toConditionES: Option[ConditionES] = {
    def updateCriteria(criteria : Option[String]) : Option[String] =
      criteria match {
        case Some(value) if value.equalsIgnoreCase("always") || value.equalsIgnoreCase("function") => None
        case _ => criteria
      }

    val functions = condition.functionList.map(value => value.split(",").toList).map(fnNameList => fnNameList.map(toFunctionDesc))
      val conditionOnValue = condition.conditionOn.getOrElse("NOT_DEFINED_VALUE_FOR_CONDITION_ON")
      Some(ConditionES(toConditionDesc(conditionOnValue), functions, updateCriteria(condition.criteria)))
  }

  private def toActionSourceES(action : RuleAction): Option[ActionSourceES] = {
    action.instanceAction.map(value => 
      ActionSourceES(value, 
          action.instanceTargetQueue, 
          action.instanceInterventionType.getOrElse("NONE"),
          action.instanceInterventionTypeText,
          action.instanceRoutingCode,
        toDefaultES(action.instanceUnit),
        toDefaultES(action.instancePriority)
          ))
  }

  private def toDefaultES(unit : Option[String]): Option[String] = {
    Some(unit.getOrElse("KEEP_CURRENT"))
  }

  private def toActionNewInstanceES(action : RuleAction): Option[ActionNewInstanceES] = {
    action.newInstanceAction.map(value =>
      ActionNewInstanceES(
        mapType(action.newInstanceType),
        mapTypeOption(action.newInstanceType),
        value,
        action.newInstanceTargetQueue,
        action.newInstanceInterventionType.getOrElse("NONE"),
        action.newInstanceInterventionTypeText,
        action.newInstanceRoutingCode,
        toDefaultES(action.newInstanceUnit),
        toDefaultES(action.newInstancePriority)
        )
    )
  }
  
  private def toActionES: Option[ActionES] = {
    val actionOnValue = action.actionOn.getOrElse("NOT_DEFINED_VALUE_FOR_ACTION_ON")

    actionOnValue.toUpperCase() match {
      case "SOURCE" => Some(ActionES(IdCodeDescriptionES(1L, actionOnValue, "Source"), toActionSourceES(action)))
      case "NEW_INSTANCE" => Some(ActionES(IdCodeDescriptionES(2L, actionOnValue, "New Instance"), None, toActionNewInstanceES(action)))
      case "SOURCE_AND_NEW_INSTANCE" => Some(ActionES(IdCodeDescriptionES(4L, actionOnValue, "Source and New Instance"), toActionSourceES(action), toActionNewInstanceES(action)))
      case _ => Some(ActionES(IdCodeDescriptionES(256L, actionOnValue, "Not defined"), toActionSourceES(action), toActionNewInstanceES(action)))
    }

  }
  
  def toES: RuleEntityES = { RuleEntityES(sequence, routingPointName, ruleDescription, schemaMap, toConditionES, toActionES) }

  //  require(!routingPointName.isEmpty, "routingpointname.empty")
}

case class IdCodeDescriptionES(id: Long, code: String, description: String)
case class IdDescriptionES(id: Long, description: String) {
  def fields = Seq(("id", id), ("description", description))
  def map = fields.toMap
}
case class ConditionES(conditionOn: IdDescriptionES, functions: Option[Seq[IdDescriptionES]] = None, message: Option[String] = None) {
  def fields = Seq("conditionOn" -> conditionOn) ++ functions.map(("functions", _)) ++ message.map(("message", _))
  def map = fields.toMap
}
case class ActionSourceES(action: String, actionOption: Option[String] = None, intervention: String, interventionText: Option[String] = None, routingCode: Option[String] = None, unit: Option[String] = Some("KEEP_CURRENT"), priority: Option[String] = Some("KEEP_CURRENT"))
case class ActionNewInstanceES(instanceType: String, instanceTypeOption: Option[String] = None, action: String, actionOption: Option[String] = None, intervention: String, interventionText: Option[String] = None, routingCode: Option[String] = None, unit: Option[String] = Some("KEEP_CURRENT"), priority: Option[String] = Some("KEEP_CURRENT"))
case class ActionES(actionOn: IdCodeDescriptionES, source: Option[ActionSourceES] = None, newInstance: Option[ActionNewInstanceES] = None)
case class AuditES(lastModification: Date, creationDate: Date, createdBy: String)
case class RuleEntityES(sequence: Long, routingPoint: String, description: Option[String], schemas: Option[String] = None, condition: Option[ConditionES] = None, action: Option[ActionES] = None, audit: Option[AuditES] = None)

case class RuleActionUpdate(
    actionOn: Option[String] = None,
    instanceAction: Option[String] = None,
    instanceInterventionType: Option[String] = None,
    instanceInterventionTypeText: Option[String] = None,
    instanceRoutingCode: Option[String] = None,
    instanceTargetQueue: Option[String] = None,
    instanceUnit: Option[String] = None,
    instancePriority: Option[String] = None,
    newInstanceAction: Option[String] = None,
    newInstanceRoutingCode: Option[String] = None,
    newInstanceInterventionType: Option[String] = None,
    newInstanceInterventionTypeText: Option[String] = None,
    newInstanceTargetQueue: Option[String] = None,
    newInstanceType: Option[String] = None,
    newInstanceUnit: Option[String] = None,
    newInstancePriority: Option[String] = None) {

  def merge(ruleAction: RuleAction): RuleAction = {
    
        RuleAction(actionOn.orElse(ruleAction.actionOn),
          instanceAction.orElse(ruleAction.instanceAction),
          instanceInterventionType.orElse(ruleAction.instanceInterventionType),
          instanceInterventionTypeText.orElse(ruleAction.instanceInterventionTypeText),
          instanceRoutingCode.orElse(ruleAction.instanceRoutingCode),
          instanceTargetQueue.orElse(ruleAction.instanceTargetQueue),
          instanceUnit.orElse(ruleAction.instanceUnit),
          instancePriority.orElse(ruleAction.instancePriority),
          newInstanceAction.orElse(ruleAction.newInstanceAction),
          newInstanceRoutingCode.orElse(ruleAction.newInstanceRoutingCode),
          newInstanceInterventionType.orElse(ruleAction.newInstanceInterventionType),
          newInstanceInterventionTypeText.orElse(ruleAction.newInstanceInterventionTypeText),
          newInstanceTargetQueue.orElse(ruleAction.newInstanceTargetQueue),
          newInstanceType.orElse(ruleAction.newInstanceType),
          newInstanceUnit.orElse(ruleAction.newInstanceUnit),
          newInstancePriority.orElse(ruleAction.newInstancePriority))
      
  }
  
  def toRuleAction : RuleAction = {
    RuleAction(actionOn,instanceAction, instanceInterventionType, instanceInterventionTypeText,
      instanceRoutingCode, instanceTargetQueue, instanceUnit, instancePriority,
      newInstanceAction, newInstanceRoutingCode, newInstanceInterventionType, newInstanceInterventionTypeText,
      newInstanceTargetQueue, newInstanceType, newInstanceUnit, newInstancePriority)
  }
}

case class RuleConditionUpdate(conditionOn: Option[String] = None, criteria: Option[String] = None, functionList: Option[String] = None) {

  def merge(ruleCondition: RuleCondition): RuleCondition = {
     RuleCondition(conditionOn.orElse(ruleCondition.conditionOn), criteria.orElse(ruleCondition.criteria), functionList.orElse(ruleCondition.functionList))
  }
  def toRuleCondition: RuleCondition = {
     RuleCondition(conditionOn, criteria, functionList)
  }
}

case class RuleEntityUpdate(
    full: Option[String] = Some("true"),
    ruleDescription: Option[String] = None,
    schemaMap: Option[String] = None,
    environment : String =  "UNKNOWN",
    version: String = "DEFAULT",
    action: RuleActionUpdate,
    condition: RuleConditionUpdate) {

  def merge(rule: RuleEntity): RuleEntity = {
    RuleEntity(rule.sequence, rule.routingPointName, full.orElse(rule.full), ruleDescription.orElse(rule.ruleDescription), schemaMap.orElse(rule.schemaMap), environment, version, action.merge(rule.action), condition.merge(rule.condition))
  }
  
  def merge(sequence: Long, pointName: String): RuleEntity = {
    RuleEntity(sequence, pointName, full, ruleDescription, schemaMap, environment, version, action.toRuleAction, condition.toRuleCondition)
  }
}
