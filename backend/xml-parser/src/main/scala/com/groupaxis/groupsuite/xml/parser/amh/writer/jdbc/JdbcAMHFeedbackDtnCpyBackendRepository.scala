package com.groupaxis.groupsuite.xml.parser.amh.writer.jdbc

import java.util.concurrent.TimeoutException

import com.groupaxis.groupsuite.commons.persistence.jdbc.DatabaseService
import com.groupaxis.groupsuite.routing.write.domain.model.amh.feedback.distribution.copy.AMHFeedbackDistributionCpyMessages.AMHFeedbackDistributionCpyBackendsCreated
import com.groupaxis.groupsuite.routing.write.domain.model.amh.feedback.distribution.copy.{AMHFeedbackDistributionCpyBackendEntity, FeedbackDtnCpyBackDAO}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.Await
import scala.util.Try

object JdbcAMHFeedbackDtnCpyBackendRepository {

  def apply(dao: FeedbackDtnCpyBackDAO, databaseService: DatabaseService) { new JdbcAMHFeedbackDtnCpyBackendRepository(dao,databaseService) }
}

class JdbcAMHFeedbackDtnCpyBackendRepository(dao : FeedbackDtnCpyBackDAO, databaseService: DatabaseService) extends Logging {

  import databaseService._

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._
//  import databaseService.driver.api._
  import slick.driver.PostgresDriver.api._

  def createFeedbackDtnCpyBackends(newFeedbackBackends: Seq[AMHFeedbackDistributionCpyBackendEntity]): Either[String, AMHFeedbackDistributionCpyBackendsCreated] = {
    try {
      val resp = Await.result(db.run(dao.amhFeedbackDtnCpyBackends ++= newFeedbackBackends).map { _ => Right(AMHFeedbackDistributionCpyBackendsCreated(newFeedbackBackends)) }, 15.seconds)
      logger.debug(" finish with " + resp.b)
      resp
    } catch {
      case timeEx: TimeoutException => Left(s" Error while creating feedback distribution copy backends DB not responding")
      case e: Exception             => Left(s" Error while creating feedback distribution copy backends msg[$e.getMessage]")
    }
  }

  def findAllFeedbackDtnCpyBackends: Option[Seq[AMHFeedbackDistributionCpyBackendEntity]] = {
    val result = Await.result(db.run(dao.amhFeedbackDtnCpyBackends.result), 10.seconds)
    Some(result)
  }

  def findAllFeedbackBackendsByEnvAndVersion(env : String, version : String): Option[Seq[AMHFeedbackDistributionCpyBackendEntity]] = {
    val result = Await.result(db.run(dao.amhFeedbackDtnCpyBackends.filter( backend => backend.env === env && backend.version === version).result), 10.seconds)
    Some(result)
  }

  def findAllFeedbackBackendsByCode(code : String): Seq[AMHFeedbackDistributionCpyBackendEntity] = {
    Try(
      {
        val result = Await.result(db.run(dao.amhFeedbackDtnCpyBackends.filter( _.code === code).result), 10.seconds)
        logger.debug(s"findAllFeedbackBackendsByCode($code) => Success ")
        result
      }).recover{
      case e => logger.debug(s"findAllFeedbackBackendsByCode error : ${e.getMessage}"); List()
    }.get
  }

  def deleteFeedbackDistributionBackendsByDistributionCode(code: String): Option[Int] = {
    Try(
      {
        val result = Await.result(db.run(dao.amhFeedbackDtnCpyBackends.filter(_.code === code).delete), 10.seconds)
        logger.debug("deleteFeedbackDistributionBackendsByDistributionCode => " + result)
        result
      }).recover{
      case e => logger.debug(s"deleteFeedbackDistributionBackendsByDistributionCode error : ${e.getMessage}"); -1
    }
      .toOption
  }

  def insertFeedbackDistributionBackends(feedbackDisitributionBackends: Seq[AMHFeedbackDistributionCpyBackendEntity]): Either[String, AMHFeedbackDistributionCpyBackendsCreated] = {
    try {
      val result = Await.result(db.run(dao.amhFeedbackDtnCpyBackends ++= feedbackDisitributionBackends).map { _ => Right(AMHFeedbackDistributionCpyBackendsCreated(feedbackDisitributionBackends)) }, 15.seconds)
      logger.debug("insertFeedbackDistributionBackends => " + result)
      result
    } catch {
      case timeEx: TimeoutException => Left(s" Error while creating amh feedback distribution backends DB not responding")
      case e: Exception => Left(s" Error while creating amh feedback distribution backends msg[$e.getMessage]")
    }
  }

  def deleteAll(): Option[Int] = {
    val result = Await.result(db.run(dao.amhFeedbackDtnCpyBackends.delete), 10.seconds)
    Some(result)
  }

}