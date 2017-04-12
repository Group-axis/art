package com.groupaxis.groupsuite.routing.write.domain.model.routing.rule

import com.groupaxis.groupsuite.routing.write.domain.model.routing.point.PointES

trait RuleESRequest
trait RuleESResponse
trait RuleRequest
trait RuleResponse

object RuleMessages {

  //commands
  case class CreateRule(pointName: String, sequence: Long, rule: RuleEntityUpdate) extends RuleRequest
  case class UpdateRule(pointName: String, sequence: Long, rule: RuleEntityUpdate) extends RuleRequest
  case class FindAllRules(pointName: String) extends RuleRequest
  case class FindRuleByPK(pointName: String, sequence: Long) extends RuleRequest
  case class InsertPointsES(points: Seq[PointES]) extends RuleESRequest
  case class InsertRuleES(pointName : String, rule: RuleEntityES) extends RuleESRequest
  case class UpdateRuleES(pointName : String, rule: RuleEntityES) extends RuleESRequest
  case class InitializePointsES() extends RuleESRequest

  //events
  case class RuleFound(rule: Option[RuleEntity]) extends RuleResponse
  case class RuleCreated(response : RuleEntity) extends RuleResponse
  case class RulesCreated(response : Seq[RuleEntity]) extends RuleResponse
  case class RuleUpdated(response : RuleEntity) extends RuleResponse
  case class RulesFound(rules: Seq[RuleEntity]) extends RuleResponse
  case class RuleESInserted(rule: RuleEntityES) extends RuleESResponse
  case class PointsESInserted(points: Seq[PointES]) extends RuleESResponse
  case class RuleESUpdated(rule: RuleEntityES) extends RuleESResponse
  case class PointsESInitialized(points : Int) extends RuleResponse

}