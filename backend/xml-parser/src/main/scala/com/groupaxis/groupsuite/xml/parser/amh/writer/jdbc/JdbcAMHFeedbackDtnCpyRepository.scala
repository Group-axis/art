package com.groupaxis.groupsuite.xml.parser.amh.writer.jdbc

import com.groupaxis.groupsuite.commons.persistence.jdbc.DatabaseService

import scala.concurrent.Await
import java.util.concurrent.TimeoutException

import com.groupaxis.groupsuite.routing.write.domain.model.amh.feedback.distribution.copy.AMHFeedbackDistributionCpyMessages.{AMHFeedbackDistributionCpsCreated, AMHFeedbackDistributionCpyCreated, AMHFeedbackDistributionCpyFound, AMHFeedbackDistributionCpyUpdated}
import com.groupaxis.groupsuite.routing.write.domain.model.amh.feedback.distribution.copy._
import org.apache.logging.log4j.scala.Logging

object JdbcAMHFeedbackDtnCpyRepository {

  def apply(dao: FeedbackDtnCpyDAO, databaseService: DatabaseService) { new JdbcAMHFeedbackDtnCpyRepository(dao,databaseService) }
}

class JdbcAMHFeedbackDtnCpyRepository(dao : FeedbackDtnCpyDAO, databaseService: DatabaseService) extends Logging {

  import scala.concurrent.duration._
  import scala.concurrent.ExecutionContext.Implicits.global
  import databaseService._
//  import databaseService.driver.api._
  import slick.driver.PostgresDriver.api._

  def createFeedbackDtnCps(newFeedbackDtnCps: Seq[AMHFeedbackDistributionCpyEntity]): Either[String, AMHFeedbackDistributionCpsCreated] = {
    try {
      val resp = Await.result(db.run(dao.amhFeedbackDistributionCps ++= newFeedbackDtnCps).map { _ => Right(AMHFeedbackDistributionCpsCreated(newFeedbackDtnCps)) }, 15.seconds)
      logger.debug(" finish with " + resp.b)
      resp
    } catch {
      case timeEx: TimeoutException => Left(s" Error while creating feedback distribution copies DB not responding")
      case e: Exception             => Left(s" Error while creating feedback distribution copies msg[$e.getMessage]")
    }
  }


    def createFeedbackCopy(newFeedbackCopy: AMHFeedbackDistributionCpyEntity): Either[String, AMHFeedbackDistributionCpyCreated] = {
    try {
      val resp = Await.result(db.run(dao.amhFeedbackDistributionCps += newFeedbackCopy).map { _ => Right(AMHFeedbackDistributionCpyCreated(newFeedbackCopy)) }, 15.seconds)
      logger.debug(" finish with " + resp.b)
      resp
    } catch {
      case timeEx: TimeoutException => Left(s" Error while creating feedback distribution copy DB not responding")
      case e: Exception             => Left(s" Error while creating feedback distribution copy msg[$e.getMessage]")
    }
  }

  def getFeedbackCopyByCode(code: String): Either[String, AMHFeedbackDistributionCpyFound] = {
    try {
      val result: Option[AMHFeedbackDistributionCpyEntity] = Await.result(db.run(dao.amhFeedbackDistributionCps.filter(_.code === code).result), 5.seconds).headOption
      logger.debug("getFeedbackCopyByCode => " + result)
      Right(AMHFeedbackDistributionCpyFound(result))
    } catch {
      case timeEx: TimeoutException => Left(s" Error while looking for a Feedback distribution copy $code DB not responding")
      case e: Exception => Left(s" Error while looking for a Feedback distribution copy $code msg[$e.getMessage]")
    }
  }

  def updateFeedbackCopy(code: String, feedbackCopyUpdate: AMHFeedbackDistributionCpyEntityUpdate, originalRules : Seq[AMHFeedbackDistributionCpyRuleEntity], originalBackends : Seq[AMHFeedbackDistributionCpyBackendEntity]): Either[String, AMHFeedbackDistributionCpyUpdated] = {
    val eitherResponse = getFeedbackCopyByCode(code)

    if (eitherResponse.isLeft) {
      Left(eitherResponse.left.get)
    } else {
      val feedbackCopyFound = eitherResponse.right.get
      feedbackCopyFound.feedbackDistCpy match {
        case Some(feedbackCpy) =>
          try {
            val updatedWithRulesAndBackendFeedbackDtnCopy = feedbackCpy.copy(rules = originalRules, backends = originalBackends)
            val updatedFeedback = feedbackCopyUpdate.merge(updatedWithRulesAndBackendFeedbackDtnCopy)
            Await.result(db.run(dao.amhFeedbackDistributionCps.filter(_.code === code).update(updatedFeedback)), 10.seconds)
            Right(AMHFeedbackDistributionCpyUpdated(updatedFeedback))
          } catch {
            case timeEx: TimeoutException => Left(s" Error while updating amh feedback distribution copy DB not responding")
            case e: Exception => Left(s" Error while updating amh feedback distribution copy msg[$e.getMessage]")
          }
        case None => Left("")
      }
    }
  }

  def findAllFeedbackDtnCps: Option[Seq[AMHFeedbackDistributionCpyEntity]] = {
    val result = Await.result(db.run(dao.amhFeedbackDistributionCps.result), 10.seconds)
    Some(result)
  }

  def findAllFeedbackCpsByEnvAndVersion(env : String, version : String): Option[Seq[AMHFeedbackDistributionCpyEntity]] = {
    val result = Await.result(db.run(dao.amhFeedbackDistributionCps.filter( feedback => feedback.env === env && feedback.version === version).result), 10.seconds)
    Some(result)
  }

  def deleteAll(): Option[Int] = {
    val result = Await.result(db.run(dao.amhFeedbackDistributionCps.delete), 10.seconds)
    Some(result)
  }
}