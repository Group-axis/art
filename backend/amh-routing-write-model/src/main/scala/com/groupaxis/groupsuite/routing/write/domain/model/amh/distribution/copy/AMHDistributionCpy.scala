package com.groupaxis.groupsuite.routing.write.domain.model.amh.distribution.copy

import org.joda.time.DateTime

trait AMHDistributionCpyRequest
trait AMHDistributionCpyResponse

object AMHDistributionCpyMessages {
 
  //commands
  case class CreateAMHDistributionCpy(code: String, distributionCpy: AMHDistributionCpyEntityUpdate, userId : Option[String], date: Option[DateTime]) extends AMHDistributionCpyRequest
  case class UpdateAMHDistributionCpy(code: String, distributionCpy: AMHDistributionCpyEntityUpdate, userId : Option[String], date: Option[DateTime]) extends AMHDistributionCpyRequest
  case class FindAMHDistributionCpyByCode(code: String) extends AMHDistributionCpyRequest
  case class FindAllAMHDistributionCpy() extends AMHDistributionCpyRequest
  case class CreateCSVFile(username : Option[String], userId : Option[String], date: Option[DateTime]) extends AMHDistributionCpyRequest

  //events
  case class AMHDistributionCpyFound(distributionCpy: Option[AMHDistributionCpyEntity]) extends AMHDistributionCpyResponse
  case class AMHDistributionCpyCreated(response : AMHDistributionCpyEntity) extends AMHDistributionCpyResponse
  case class AMHDistributionCpyUpdated(response : AMHDistributionCpyEntity) extends AMHDistributionCpyResponse
  case class AMHDistributionCpsFound(distributionCps: Option[Seq[AMHDistributionCpyEntity]]) extends AMHDistributionCpyResponse
  case class AMHDistributionCpsCreated(response: Seq[AMHDistributionCpyEntity])
  case class AMHDistributionCpyRuleCreated(response : AMHDistributionCpyRuleEntity) extends AMHDistributionCpyResponse
  case class AMHDistributionCpyRulesCreated(response : Seq[AMHDistributionCpyRuleEntity]) extends AMHDistributionCpyResponse
  case class AMHDistributionCpyBackendCreated(response : AMHDistributionCpyBackendEntity) extends AMHDistributionCpyResponse
  case class AMHDistributionCpyBackendsCreated(response : Seq[AMHDistributionCpyBackendEntity]) extends AMHDistributionCpyResponse
  case class CSVFileCreated(fileName : String) extends AMHDistributionCpyResponse
}
