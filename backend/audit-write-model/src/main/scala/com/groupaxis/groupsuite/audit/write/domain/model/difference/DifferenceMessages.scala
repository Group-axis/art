package com.groupaxis.groupsuite.audit.write.domain.model.difference

import com.groupaxis.groupsuite.simulator.write.domain.model.job.JobEntity
import com.groupaxis.groupsuite.simulator.write.domain.model.swift.msg.SwiftMessageEntity
import org.joda.time.DateTime

trait DifferenceRequest
trait DifferenceResponse

object DifferenceMessages {
  val USER_ENTITY_TYPE = "USER"

  val NEW_CHANGE_TYPE = "NEW"
  val UPDATE_CHANGE_TYPE = "UPDATE"
  val DELETE_CHANGE_TYPE = "DELETE"

  //commands

  /****  Simulation ****/


  //events


}
