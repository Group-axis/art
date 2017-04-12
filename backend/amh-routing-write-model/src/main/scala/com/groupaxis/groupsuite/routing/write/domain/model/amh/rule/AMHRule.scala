package com.groupaxis.groupsuite.routing.write.domain.model.amh.rule

import org.joda.time.DateTime

trait AMHRuleRequest
trait AMHRuleResponse

object AMHRuleMessages {
 
  //commands
  case class CreateAMHRule(code: String, rule: AMHRuleEntityUpdate, userId : Option[String], date : Option[DateTime]) extends AMHRuleRequest
  case class UpdateAMHRule(code: String, rule: AMHRuleEntityUpdate, userId : Option[String], date : Option[DateTime]) extends AMHRuleRequest
  case class FindAMHRuleByCode(code: String) extends AMHRuleRequest
  case class FindAllAMHRules() extends AMHRuleRequest
  case class SetRulesAsAssigned(ruleCodes : Seq[AMHRule]) extends AMHRuleRequest
  case class SetRulesAsUnassigned(ruleCodes : Seq[AMHRule])  extends AMHRuleRequest
  case class UnAssignRule(ruleCode : String, username : Option[String], date : Option[DateTime])  extends AMHRuleRequest
  case class CreateRuleOverviewCSVFile(assignType : Option[Boolean], username : Option[String], date : Option[DateTime]) extends AMHRuleRequest

  //events
  case class AMHRuleFound(rule: Option[AMHRuleEntity]) extends AMHRuleResponse
  case class AMHRuleCreated(response : AMHRuleEntity) extends AMHRuleResponse
  case class AMHRuleUpdated(response : AMHRuleEntity) extends AMHRuleResponse
  case class AMHRulesFound(rules: Option[Seq[AMHRuleEntity]]) extends AMHRuleResponse
  case class RuleUnAssigned(deleted : Int) extends AMHRuleResponse
  case class RuleOverviewCSVFileCreated(fileName : Option[String]) extends AMHRuleResponse
}

trait AMHRule {
  def ruleCode : String
}

case class AMHRuleAssigned(code: String, sequence: Long, expression: String)
case class AMHRuleAssignmentResponse(_index : String, _type: String, code : String, sequence : Long, rules : Seq[AMHRuleAssigned])