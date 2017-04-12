package com.groupaxis.groupsuite.routing.write.domain.model.routing.rule

import java.util.Date
import com.groupaxis.groupsuite.routing.write.domain.model.routing.schema.Schema

abstract class Instance {
  def actionId: Long
  def actionOptionId: Option[Long]
  def interventionId: Long
  def interventionText: Option[String]
  def unitId: Long
  def routingCode: Option[String]
  def priorityId: Long
  // methods (ideally only accessors since it is a case class)
}

case class Source(actionId: Long, actionOptionId: Option[Long], interventionId: Long, interventionText: Option[String], unitId: Long, routingCode: Option[String], priorityId: Long) extends Instance
case class NewInstance(actionId: Long, actionOptionId: Option[Long], interventionId: Long, interventionText: Option[String], unitId: Long, routingCode: Option[String], priorityId: Long, typeId: Long, typeOptionId: Option[Long]) extends Instance
case class Action(actionOn: ActionType, source: Option[Source], newInstance: Option[NewInstance])

case class ConditionFunction(id: Long, description: Option[String])

case class Condition(conditionOn: ConditionType, functions: Option[List[ConditionFunction]], message: Option[String])

trait RuleID {
	def ruleId: Long
}
 
case class Rule(ruleId: Long, pointId: Long, description: Option[String], schemas: Option[List[Schema]],lastModification: Date, creationDate: Date, createdBy: String, condition: Condition, action: Action) 
  extends RuleID