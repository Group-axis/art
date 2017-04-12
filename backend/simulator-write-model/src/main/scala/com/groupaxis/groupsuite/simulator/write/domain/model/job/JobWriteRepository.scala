package com.groupaxis.groupsuite.simulator.write.domain.model.job

import com.groupaxis.groupsuite.simulator.write.domain.model.job.JobMessages.{JobCreated, JobFound, JobUpdated, JobsFound}

trait JobWriteRepository {

  def getJobs: Either[String, JobsFound]

  def getJobById(id : Int): Either[String, JobFound]

  def createJob(user: JobEntity): Either[String, JobCreated]

  def updateJob(id: Int, userUpdate: JobEntityUpdate): Either[String, JobUpdated]

  def deleteJob(id: Int): Either[String, Int]

}