package com.groupaxis.groupsuite.routing.write.domain.model.amh.criteria

import org.joda.time.DateTime

trait AMHCriteriaRequest
trait AMHCriteriaResponse

object AMHRuleMessages {

  //commands
  case class CreateAMHCriteria(code: String, rule: AMHRuleCriteriaEntityUpdate, userId : Option[String], date : Option[DateTime]) extends AMHCriteriaRequest
  case class UpdateAMHCriteria(code: String, rule: AMHRuleCriteriaEntityUpdate, userId : Option[String], date : Option[DateTime]) extends AMHCriteriaRequest
  case class FindAMHCriteriaByCode(code: String) extends AMHCriteriaRequest
  case class FindAllAMHCriteria() extends AMHCriteriaRequest

  //events
  case class AMHCriteriaFound(rule: Option[AMHRuleCriteriaEntity]) extends AMHCriteriaResponse
  case class AMHCriteriaCreated(response : AMHRuleCriteriaEntity) extends AMHCriteriaResponse
  case class AMHCriteriaUpdated(response : AMHRuleCriteriaEntity) extends AMHCriteriaResponse
  case class AMHCriteriaAllFound(rules: Option[Seq[AMHRuleCriteriaEntity]]) extends AMHCriteriaResponse
}
