package com.groupaxis.groupsuite.simulator.infrastructor.jdbc

import java.util.concurrent.TimeoutException

import com.groupaxis.groupsuite.persistence.datastore.jdbc.Database
import com.groupaxis.groupsuite.simulator.write.domain.model.job.JobMessages.{JobCreated, JobFound, JobUpdated, JobsFound}
import com.groupaxis.groupsuite.simulator.write.domain.model.job.{JobEntity, JobEntityUpdate, JobWriteRepository}

import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration


object JdbcJobWriteRepository {
}

class JdbcJobWriteRepository(database: Database, timeout: FiniteDuration) extends JobWriteRepository {

  import database._
  import database.driver.api._
//  import slick.driver.PostgresDriver.api._
  import com.groupaxis.groupsuite.simulator.write.domain.model.job.JobDAO.jobs
  import scala.concurrent.ExecutionContext.Implicits.global

  def getJobs : Either[String, JobsFound] = {
    try {
      val result = Await.result(db.run(jobs.result), timeout)
      Right(JobsFound(result))
    } catch {
      case timeEx: TimeoutException => Left(s" Error while looking for all jobs DB not responding")
      case e: Exception             => Left(s" Error while looking for all jobs msg[$e.getMessage]")
    }
  }

  def getJobsByUsername(username : String, status : Option[Int] = None) : Either[String, JobsFound] = {
    try {
      val query  = jobs.filter(_.user === username)
      val finalQuery = status.map(s => query.filter(_.status === s)).getOrElse(query)
//      val finalQuery = if (status.isDefined) query.filter(_.status === status) else query

      val result = Await.result(db.run(finalQuery.sortBy(_.status.desc.nullsFirst).result), timeout)
      Right(JobsFound(result))
    } catch {
      case timeEx: TimeoutException => Left(s" Error while looking for all jobs of user $username DB not responding")
      case e: Exception             => Left(s" Error while looking for all jobs of user $username msg[$e.getMessage]")
    }
  }

  def getJobsByUsernameAndLauncher(username : String, launcherId : Int) : Either[String, JobsFound] = {
    try {
      val query  = jobs.filter(_.user === username).filter(_.jobLauncher === launcherId)
      val result = Await.result(db.run(query.sortBy(_.status.desc.nullsFirst).result), timeout)
      Right(JobsFound(result))
    } catch {
      case timeEx: TimeoutException => Left(s" Error while looking for all jobs of user $username and launcher $launcherId DB not responding")
      case e: Exception             => Left(s" Error while looking for all jobs of user $username and launcher $launcherId msg[$e.getMessage]")
    }
  }

  def getJobById(id: Int): Either[String, JobFound] = {
    try {
      val result: Option[JobEntity] = Await.result(db.run(jobs.filter(_.id === id).result), timeout).headOption
      Right(JobFound(result))
    } catch {
      case timeEx: TimeoutException => Left(s" Error while looking for a job $id DB not responding")
      case e: Exception             => Left(s" Error while looking for a job $id msg[$e.getMessage]")
    }
  }

  def createJob(job: JobEntity): Either[String, JobCreated] = {
    try {
      Await.result(db.run(jobs returning jobs += job).map { job => Right(JobCreated(job)) }, timeout)
    } catch {
      case timeEx: TimeoutException => Left(s" Error while creating job DB not responding")
      case e: Exception             => Left(s" Error while creating job msg[$e.getMessage]")
    }
  }

  def updateStatus(id: Int, status : Option[Int]): Either[String, JobUpdated] = {
    val eitherResponse = getJobById(id)
    if (eitherResponse.isLeft) {
      Left(eitherResponse.left.get)
    } else {
      val jobFound = eitherResponse.right.get
      jobFound.job match {
        case Some(job) =>
          try {
            val updatedMessage = job.copy(status = status)
            Await.result(db.run(jobs.filter(_.id === id).update(updatedMessage)), timeout)
            Right(JobUpdated(updatedMessage))
          } catch {
            case timeEx: TimeoutException => Left(s" Error while updating job DB not responding")
            case e: Exception             => Left(s" Error while updating job msg[$e.getMessage]")
          }
        case None => Left("")
      }
    }
  }

  def updateJob(id: Int, jobUpdate: JobEntityUpdate): Either[String, JobUpdated] = {
    val eitherResponse = getJobById(id)

    if (eitherResponse.isLeft) {
      Left(eitherResponse.left.get)
    } else {
      val jobFound = eitherResponse.right.get
      jobFound.job match {
        case Some(job) =>
          try {
            val updatedMessage = jobUpdate.merge(job)
            Await.result(db.run(jobs.filter(_.id === id).update(updatedMessage)), timeout)
            Right(JobUpdated(updatedMessage))
          } catch {
            case timeEx: TimeoutException => Left(s" Error while updating job DB not responding")
            case e: Exception             => Left(s" Error while updating job msg[$e.getMessage]")
          }
        case None => Left("")
      }
    }
  }
  def deleteJob(id: Int): Either[String, Int] = {
   try {
    val result = Await.result(db.run(jobs.filter(_.id === id).delete), timeout)
     Right(result)
   } catch {
     case timeEx: TimeoutException => Left(s" Error while deleting job id $id DB not responding")
     case e: Exception             => Left(s" Error while deleting job id $id msg[$e.getMessage]")
   }
  }

  def findJobByStatus(statuses : Seq[Int]) : Either[String, Option[JobEntity]] =
    try {
      val result = Await.result(db.run(jobs.filter(_.status inSet statuses).result), timeout)
        Right(result.headOption)
    } catch {
      case timeEx: TimeoutException => Left(s" Error while findJobByStatus($statuses) DB not responding")
      case e: Exception             => Left(s" Error while findJobByStatus($statuses) msg[${e.getMessage}]")
    }

  def updateStatus(jobId: Int, status : Int, comment : String) =
    getJobById(jobId) match {
      case Right(JobFound(Some(jobEntity))) =>
        val resp = Await.result(db.run(jobs.filter(_.id === jobId).update(jobEntity.copy(status = Some(status), comment = Some(comment)))), timeout)
        Right(resp)
      case Right(JobFound(None)) =>
        Left(s"No job found with id = $jobId")
      case Left(error) =>
        Left(error)
    }
}