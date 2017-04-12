package com.groupaxis.groupsuite.xml.parser.amh.writer.jdbc

import com.groupaxis.groupsuite.commons.persistence.jdbc.DatabaseService

import scala.concurrent.{Await, Future}
import java.util.concurrent.TimeoutException

import com.groupaxis.groupsuite.routing.write.domain.model.amh.assignment.{AMHAssignmentRuleEntity, AssignmentRuleDAO}
import com.groupaxis.groupsuite.routing.write.domain.model.amh.feedback.distribution.copy.AMHFeedbackDistributionCpyMessages.AMHFeedbackDistributionCpyRulesCreated
import com.groupaxis.groupsuite.routing.write.domain.model.amh.feedback.distribution.copy.{AMHFeedbackDistributionCpyRuleEntity, FeedbackDtnCpyRuleDAO}
import org.apache.logging.log4j.scala.Logging

import scala.util.Try

object JdbcAMHFeedbackDtnCpyRuleRepository {

  def apply(dao: FeedbackDtnCpyRuleDAO, databaseService: DatabaseService) { new JdbcAMHFeedbackDtnCpyRuleRepository(dao,databaseService) }
}

class JdbcAMHFeedbackDtnCpyRuleRepository(dao : FeedbackDtnCpyRuleDAO, databaseService: DatabaseService) extends Logging {

  import scala.concurrent.duration._
  import scala.concurrent.ExecutionContext.Implicits.global
  import databaseService._
//  import databaseService.driver.api._
  import slick.driver.PostgresDriver.api._

  def deleteFeedbackRulesByRuleCode(ruleCode : String) : Future[Int] = {
    val query = dao.amhFeedbackDtnCpyRules.filter(_.ruleCode === ruleCode)
    val action = query.delete
    db.run(action)
      .recover { case ex : java.sql.SQLException =>
        logger.debug(s" An error occurred while deleting rule code $ruleCode from feedback distribution copy : ${ex.getLocalizedMessage}")
        -1
      }
  }

  def createFeedbackDtnCpyRules(newAssignmentRules: Seq[AMHFeedbackDistributionCpyRuleEntity]): Either[String, AMHFeedbackDistributionCpyRulesCreated] = {
    try {
      val resp = Await.result(db.run(dao.amhFeedbackDtnCpyRules ++= newAssignmentRules).map { _ => Right(AMHFeedbackDistributionCpyRulesCreated(newAssignmentRules)) }, 15.seconds)
      logger.debug(" finish with " + resp.b)
      resp
    } catch {
      case timeEx: TimeoutException => Left(s" Error while creating feedback distribution copy rules DB not responding")
      case e: Exception             => Left(s" Error while creating feedback distribution copy rules msg[$e.getMessage]")
    }
  }

  def findAllFeedbackDtnCpyRules: Option[Seq[AMHFeedbackDistributionCpyRuleEntity]] = {
    val result = Await.result(db.run(dao.amhFeedbackDtnCpyRules.result), 10.seconds)
    Some(result)
  }

  def findAllFeedbackRulesByEnvAndVersion(env : String, version : String): Option[Seq[AMHFeedbackDistributionCpyRuleEntity]] = {
    val result = Await.result(db.run(dao.amhFeedbackDtnCpyRules.filter( rule => rule.env === env && rule.version === version).result), 10.seconds)
    Some(result)
  }

  def getFeedbackRulesByDistributionCode(code: String): Seq[AMHFeedbackDistributionCpyRuleEntity] = {
    Try({
      val result = Await.result(db.run(dao.amhFeedbackDtnCpyRules.filter(_.code === code).take(200).result), 10.seconds)
      logger.debug(s"getFeedbackRulesByDistributionCode($code) => Success ")
      result
    })
      .recover{
        case e => logger.debug(s"getFeedbackRulesByDistributionCode error : ${e.getMessage}"); List()
      }
      .get
  }

  def deleteFeedbackRulesByDistributionCode(code: String): Option[Int] = {
    val result = Await.result(db.run(dao.amhFeedbackDtnCpyRules.filter(_.code === code).delete), 10.seconds)
    logger.debug("deleteFeedbackRulesByDistributionCode => " + result)
    Some(result)
  }

  def insertFeedbackRules(feedbackRules: Seq[AMHFeedbackDistributionCpyRuleEntity]): Either[String, AMHFeedbackDistributionCpyRulesCreated] = {
    try {
      val result = Await.result(db.run(dao.amhFeedbackDtnCpyRules ++= feedbackRules).map { _ => Right(AMHFeedbackDistributionCpyRulesCreated(feedbackRules)) }, 15.seconds)
      logger.debug("insertFeedbackRules => " + result)
      result
    } catch {
      case timeEx: TimeoutException => Left(s" Error while creating amh feedback rules DB not responding")
      case e: Exception => Left(s" Error while creating amh feedback rules msg[$e.getMessage]")
    }
  }

  def deleteAll(): Option[Int] = {
    val result = Await.result(db.run(dao.amhFeedbackDtnCpyRules.delete), 10.seconds)
    Some(result)
  }

}