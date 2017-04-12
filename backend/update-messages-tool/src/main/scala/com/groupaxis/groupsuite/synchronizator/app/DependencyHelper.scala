package com.groupaxis.groupsuite.synchronizator.app

import com.groupaxis.groupsuite.synchronizator.domain.model.assignment.backend.{AMHBackendAssignmentESRepository, AMHBackendAssignmentRepository}
import com.groupaxis.groupsuite.synchronizator.domain.model.assignment.distribution.{AMHDistributionAssignmentESRepository, AMHDistributionAssignmentRepository}
import com.groupaxis.groupsuite.synchronizator.domain.model.assignment.feedback.{AMHFeedbackDistributionAssignmentESRepository, AMHFeedbackDistributionAssignmentRepository}
import com.groupaxis.groupsuite.synchronizator.domain.model.backends.{AMHBackendESRepository, AMHBackendRepository}
import com.groupaxis.groupsuite.synchronizator.domain.model.criteria.{AMHRuleCriteriaESRepository, AMHRuleCriteriaFileRepository}
import com.groupaxis.groupsuite.synchronizator.domain.model.exitpoint.{SAAExitPointESRepository, SAAExitPointRepository}
import com.groupaxis.groupsuite.synchronizator.domain.model.messagePartner.{SAAMessagePartnerESRepository, SAAMessagePartnerRepository}
import com.groupaxis.groupsuite.synchronizator.domain.model.points.{SAAPointESRepository, SAARuleRepository}
import com.groupaxis.groupsuite.synchronizator.domain.model.rules.{AMHRuleESRepository, AMHRuleRepository}
import com.groupaxis.groupsuite.synchronizator.domain.model.schemas.{SAASchemaESRepository, SAASchemaRepository}
import com.groupaxis.groupsuite.synchronizator.domain.model.simulation.swift.message.{AMHSwiftMessageESRepository, AMHSwiftMessageRepository}
import com.groupaxis.groupsuite.synchronizator.domain.model.simulation.swift.mapping.AMHMessageMappingRepository

/***
  * Self-type annotations emphasize mixin composition.
  * Inheritance can imply a subtype relationship.
  * Note: Here we are explicitly defining composition of behavior through mixins,
          instead of using inheritance and mixins.
  ***/
//I am getting a null pointer exception while self-typing on DB ot ES Helpers
trait DependencyHelper { //self:  ESHelper =>
  import DBHelper._
  import ESHelper._
  val ruleRepository = AMHRuleRepository(database)
  val ruleESRepository = AMHRuleESRepository(client)

  val backendRepository = AMHBackendRepository(database)
  val backendESRepository = AMHBackendESRepository(client)

  val backendAssignmentRepository = AMHBackendAssignmentRepository(database)
  val backendAssignmentESRepository = AMHBackendAssignmentESRepository(client)

  val distributionAssignmentRepository = AMHDistributionAssignmentRepository(database)
  val distributionAssignmentESRepository = AMHDistributionAssignmentESRepository(client)

  val feedbackAssignmentRepository = AMHFeedbackDistributionAssignmentRepository(database)
  val feedbackAssignmentESRepository = AMHFeedbackDistributionAssignmentESRepository(client)

  val messageRepository = AMHSwiftMessageRepository(database)
  val messageESRepository = AMHSwiftMessageESRepository(client)

  def criteriaRepository(filePath : String) = AMHRuleCriteriaFileRepository(filePath)
  val criteriaESRepository = AMHRuleCriteriaESRepository(client)

  val mappingRepository = AMHMessageMappingRepository(database)

  val saaRuleRepository = SAARuleRepository(database)
  val saaPointESRepository = SAAPointESRepository(client)

  val saaSchemaRepository = SAASchemaRepository(database)
  val saaSchemaESRepository = SAASchemaESRepository(client)

  val saaExitPointRepository = SAAExitPointRepository(database)
  val saaExitPointESRepository = SAAExitPointESRepository(client)

  val saaMessagePartnerRepository = SAAMessagePartnerRepository(database)
  val saaMessagePartnerESRepository = SAAMessagePartnerESRepository(client)
}

//object DependencyHelper extends DependencyHelper