package com.groupaxis.groupsuite.simulator.write.domain.audit.messages

import com.groupaxis.groupsuite.simulator.write.domain.model.job.JobEntity
import com.groupaxis.groupsuite.simulator.write.domain.model.swift.msg.SwiftMessageEntity
import org.joda.time.DateTime

object SimulationAuditMessages {

  trait SimulationAuditRequest
  trait SimulationAuditResponse
  
  //commands
  case class CreateSimpleMessage(username: String, date: DateTime, newEntity: SwiftMessageEntity) extends SimulationAuditRequest
  case class DeleteSimpleMessage(username: String, date: DateTime, deletedEntity: SwiftMessageEntity) extends SimulationAuditRequest
  case class UpdateSimpleMessage(username: String, date: DateTime, oldEntity: SwiftMessageEntity, newEntity: SwiftMessageEntity) extends SimulationAuditRequest

  case class CreateGroupMessage(username: String, date: DateTime, fileName: String) extends SimulationAuditRequest
  case class DeleteGroupMessage(username: String, date: DateTime, fileName: String) extends SimulationAuditRequest

  case class CreateBatchSimulation(username: String, date: DateTime, job: JobEntity) extends SimulationAuditRequest
  case class CreateInstanceSimulation(username: String, date: DateTime, job : JobEntity) extends SimulationAuditRequest
  //??? USELESS ???
  case class CreateAskEmailSimulation(username: String, date: DateTime, asked: Boolean) extends SimulationAuditRequest

  case class CreateSimulationResultCSV(username: String, date: DateTime, fileName: String) extends SimulationAuditRequest

  //events
  case class SimulationUpdateDone() extends SimulationAuditResponse
  case class SimulationUpdateFailed(msg : String) extends SimulationAuditResponse
  case class SimulationCreationDone() extends SimulationAuditResponse
  case class SimulationCreationFailed(msg : String) extends SimulationAuditResponse
  case class SimulationDeletionDone() extends SimulationAuditResponse
  case class SimulationDeletionFailed(msg : String) extends SimulationAuditResponse
}
