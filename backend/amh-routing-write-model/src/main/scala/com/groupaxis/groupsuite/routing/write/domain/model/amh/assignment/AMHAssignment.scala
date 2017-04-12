package com.groupaxis.groupsuite.routing.write.domain.model.amh.assignment

import org.joda.time.DateTime

trait AMHAssignmentRequest
trait AMHAssignmentResponse

object AMHAssignmentMessages {
 
  //commands
  case class CreateAMHAssignment(code: String, assignment: AMHAssignmentEntityUpdate, userId : Option[String], date : Option[DateTime]) extends AMHAssignmentRequest
  case class UpdateAMHAssignment(code: String, assignment: AMHAssignmentEntityUpdate, userId : Option[String], date : Option[DateTime]) extends AMHAssignmentRequest
  case class FindAMHAssignmentByCode(code: String) extends AMHAssignmentRequest
  case class FindAllAMHAssignments() extends AMHAssignmentRequest
  case class UnassignRuleByCode(ruleCode : String, userId : Option[String], date : Option[DateTime]) extends AMHAssignmentRequest
  case class CreateAssignmentCSVFile(userId : Option[String], date : Option[DateTime]) extends AMHAssignmentRequest

  //events
  case class AMHAssignmentFound(assignment: Option[AMHAssignmentEntity]) extends AMHAssignmentResponse
  case class AMHAssignmentCreated(response : AMHAssignmentEntity) extends AMHAssignmentResponse
  case class AMHAssignmentUpdated(response : AMHAssignmentEntity) extends AMHAssignmentResponse
  case class AMHAssignmentsFound(assignments: Option[Seq[AMHAssignmentEntity]]) extends AMHAssignmentResponse
  case class AMHAssignmentRuleCreated(response : AMHAssignmentRuleEntity) extends AMHAssignmentResponse
  case class AMHAssignmentRulesCreated(response : Seq[AMHAssignmentRuleEntity]) extends AMHAssignmentResponse
  case class RuleUnassigned(count :Int) extends AMHAssignmentResponse


}
