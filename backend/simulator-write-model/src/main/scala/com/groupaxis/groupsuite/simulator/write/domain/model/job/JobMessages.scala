package com.groupaxis.groupsuite.simulator.write.domain.model.job

import org.joda.time.DateTime

trait JobRequest
trait JobResponse

object JobMessages {

  //commands
  case class CreateJob(job: JobEntityUpdate, username : Option[String], date : Option[DateTime]) extends JobRequest
  case class UpdateJob(id : Int, job: JobEntityUpdate, username : Option[String], date : Option[DateTime]) extends JobRequest
  case class CancelJob(id: Int, username : Option[String], date : Option[DateTime])extends JobRequest
  case class ReExecuteJob(id: Int, username : Option[String], date : Option[DateTime])extends JobRequest
  case class CreateCSVFile(id: Int, username : Option[String], date : Option[DateTime])extends JobRequest
  case class CreateCSVFileWithHits(hits: Seq[Hit], username : Option[String], date : Option[DateTime])extends JobRequest
  case class FindAllJobs() extends JobRequest
  case class FindAllJobsByUsername(username : String, status: Option[Int], threshold : Option[Int] = None, launcherId: Int) extends JobRequest
  case class FindJobById(id : Int) extends JobRequest

  //events
  case class JobFound(job: Option[JobEntity]) extends JobResponse
  case class JobCreated(job : JobEntity) extends JobResponse
  case class JobUpdated(job : JobEntity) extends JobResponse
  case class OperationDone(job : JobEntity) extends JobResponse
  case class JobsFound(jobs: Seq[JobEntity]) extends JobResponse
  case class CSVFileCreated(fileName : String) extends JobResponse

}
