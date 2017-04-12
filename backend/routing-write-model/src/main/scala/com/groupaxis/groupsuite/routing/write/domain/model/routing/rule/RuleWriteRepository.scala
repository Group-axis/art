package com.groupaxis.groupsuite.routing.write.domain.model.routing.rule

import com.groupaxis.groupsuite.routing.write.domain.model.routing.rule.RuleMessages.{RuleCreated, RuleFound, RuleUpdated, RulesFound}

trait RuleWriteRepository {

  def getRules(pointName: String): Either[String, RulesFound]

  def getRuleByKey(pointName: String, sequence: Long): Either[String, RuleFound]

  def getRuleByRoutingPoint(routingPointName: String): Either[String, RuleEntity]

  def createRule(rule: RuleEntity): Either[String, RuleCreated]

  def updateRule(pointName: String, sequence: Long, ruleUpdate: RuleEntityUpdate): Either[String, RuleUpdated]

  def deleteRule(pointName: String, sequence: Long): Either[String, Int]

//  @throws(classOf[RuntimeException])
//  def save(rule: Rule): Rule
//
//  @throws(classOf[RuntimeException])
//  def findById(ruleId: Long): Rule
//
//  @throws(classOf[RuntimeException])
//  def findAll(): Seq[Rule]
//
//  @throws(classOf[RuntimeException])
//  def findByPointId(pointId: Long): Rule
  
}