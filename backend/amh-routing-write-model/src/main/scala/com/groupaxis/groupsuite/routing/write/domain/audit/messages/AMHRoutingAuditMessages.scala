package com.groupaxis.groupsuite.routing.write.domain.audit.messages

import com.groupaxis.groupsuite.routing.write.domain.model.amh.assignment.AMHAssignmentEntity
import com.groupaxis.groupsuite.routing.write.domain.model.amh.distribution.copy.AMHDistributionCpyEntity
import com.groupaxis.groupsuite.routing.write.domain.model.amh.feedback.distribution.copy.AMHFeedbackDistributionCpyEntity
import com.groupaxis.groupsuite.routing.write.domain.model.amh.rule.AMHRuleEntity
import org.joda.time.DateTime

object AMHRoutingAuditMessages {
  trait AMHRoutingAuditRequest
  trait AMHRoutingAuditResponse

  //commands

  case class CreateBackendAssignment(username: String, date: DateTime, newEntity: AMHAssignmentEntity) extends AMHRoutingAuditRequest
  case class CreateDistributionAssignment(username: String, date: DateTime, newEntity: AMHDistributionCpyEntity) extends AMHRoutingAuditRequest
  case class CreateFeedbackAssignment(username: String, date: DateTime, newEntity: AMHFeedbackDistributionCpyEntity) extends AMHRoutingAuditRequest

  case class CreateImport(username: String, date: DateTime, newFileName: String, status : String) extends AMHRoutingAuditRequest
  case class CreateImportBackup(username: String, date: DateTime, newFileName: String, status : String) extends AMHRoutingAuditRequest
  case class CreateExport(username: String, date: DateTime, newFileName: String, status : String) extends AMHRoutingAuditRequest

  case class CreateRuleOverviewCSV(username: String, date: DateTime, newFileName: String) extends AMHRoutingAuditRequest
  case class CreateAssignmentOverviewCSV(username: String, date: DateTime, newFileName: String) extends AMHRoutingAuditRequest

  case class UpdateBackendAssignment(username: String, date: DateTime, original: AMHAssignmentEntity, update: AMHAssignmentEntity) extends AMHRoutingAuditRequest
  case class UpdateDistributionAssignment(username: String, date: DateTime, original: AMHDistributionCpyEntity, update: AMHDistributionCpyEntity) extends AMHRoutingAuditRequest
  case class UpdateFeedbackAssignment(username: String, date: DateTime, original: AMHFeedbackDistributionCpyEntity, update: AMHFeedbackDistributionCpyEntity) extends AMHRoutingAuditRequest

  case class CreateRule(username: String, date: DateTime, newEntity: AMHRuleEntity) extends AMHRoutingAuditRequest
  case class UpdateRule(username: String, date: DateTime, original: AMHRuleEntity, update: AMHRuleEntity) extends AMHRoutingAuditRequest
  case class DeleteRule(username: String, date: DateTime, deleteEntity: AMHRuleEntity) extends AMHRoutingAuditRequest

  //events
  case class RoutingUpdateDone() extends AMHRoutingAuditResponse
  case class RoutingUpdateFailed(msg : String) extends AMHRoutingAuditResponse
  case class RoutingCreationDone() extends AMHRoutingAuditResponse
  case class RoutingCreationFailed(msg : String) extends AMHRoutingAuditResponse
  case class RoutingDeletionDone() extends AMHRoutingAuditResponse
  case class RoutingDeletionFailed(msg : String) extends AMHRoutingAuditResponse

}
