package com.groupaxis.groupsuite.routing.write.domain.model.amh.feedback.distribution.copy

import org.joda.time.DateTime

trait AMHFeedbackDistributionCpyRequest
trait AMHFeedbackDistributionCpyResponse

object AMHFeedbackDistributionCpyMessages {
 
  //commands
  case class CreateAMHFeedbackDistributionCpy(code: String, feedbackDistCpy: AMHFeedbackDistributionCpyEntityUpdate, userId : Option[String], date : Option[DateTime]) extends AMHFeedbackDistributionCpyRequest
  case class UpdateAMHFeedbackDistributionCpy(code: String, feedbackDistCpy: AMHFeedbackDistributionCpyEntityUpdate, userId : Option[String], date : Option[DateTime]) extends AMHFeedbackDistributionCpyRequest
  case class FindAMHFeedbackDistributionCpyByCode(code: String) extends AMHFeedbackDistributionCpyRequest
  case class FindAllAMHFeedbackDistributionCpy() extends AMHFeedbackDistributionCpyRequest
  case class CreateCSVFile(username : Option[String]) extends AMHFeedbackDistributionCpyRequest

  //events
  case class AMHFeedbackDistributionCpyFound(feedbackDistCpy: Option[AMHFeedbackDistributionCpyEntity]) extends AMHFeedbackDistributionCpyResponse
  case class AMHFeedbackDistributionCpyCreated(response : AMHFeedbackDistributionCpyEntity) extends AMHFeedbackDistributionCpyResponse
  case class AMHFeedbackDistributionCpyUpdated(response : AMHFeedbackDistributionCpyEntity) extends AMHFeedbackDistributionCpyResponse
  case class AMHFeedbackDistributionCpsFound(feedbackDistCps: Option[Seq[AMHFeedbackDistributionCpyEntity]]) extends AMHFeedbackDistributionCpyResponse
  case class AMHFeedbackDistributionCpsCreated(rules: Seq[AMHFeedbackDistributionCpyEntity])
  case class AMHFeedbackDistributionCpyRuleCreated(response : AMHFeedbackDistributionCpyRuleEntity) extends AMHFeedbackDistributionCpyResponse
  case class AMHFeedbackDistributionCpyRulesCreated(response : Seq[AMHFeedbackDistributionCpyRuleEntity]) extends AMHFeedbackDistributionCpyResponse
  case class AMHFeedbackDistributionCpyBackendCreated(response : AMHFeedbackDistributionCpyBackendEntity) extends AMHFeedbackDistributionCpyResponse
  case class AMHFeedbackDistributionCpyBackendsCreated(response : Seq[AMHFeedbackDistributionCpyBackendEntity]) extends AMHFeedbackDistributionCpyResponse
  case class CSVFileCreated(fileName : String) extends AMHFeedbackDistributionCpyResponse

}
