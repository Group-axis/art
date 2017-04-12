package com.groupaxis.groupsuite.xml.parser.amh.writer.jdbc

import java.util.concurrent.TimeoutException

import com.groupaxis.groupsuite.commons.persistence.jdbc.DatabaseService
import com.groupaxis.groupsuite.routing.write.domain.model.amh.distribution.copy.AMHDistributionCpyMessages.AMHDistributionCpyRulesCreated
import com.groupaxis.groupsuite.routing.write.domain.model.amh.distribution.copy.{AMHDistributionCpyRuleEntity, DistributionCpyRuleDAO}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{Await, Future}
import scala.util.Try

object JdbcAMHDistributionCpyRuleRepository {

  def apply(dao: DistributionCpyRuleDAO, databaseService: DatabaseService) { new JdbcAMHDistributionCpyRuleRepository(dao,databaseService) }
}

class JdbcAMHDistributionCpyRuleRepository(dao : DistributionCpyRuleDAO, databaseService: DatabaseService) extends Logging {

  import databaseService._

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._
//  import databaseService.driver.api._
  import slick.driver.PostgresDriver.api._

  def getDistributionRulesByCode(code: String): Seq[AMHDistributionCpyRuleEntity] = {
    Try(Await.result(db.run(dao.amhDistributionCpyRules.filter(_.code === code).result), 10.seconds))
      .recover{
        case e => logger.debug(s"getDistributionRulesByCode error: ${e.getMessage}");List()
      }
      .get
  }


  def deleteDistributionRulesByRuleCode(ruleCode : String) : Future[Int] = {
    val query = dao.amhDistributionCpyRules.filter(_.ruleCode === ruleCode)
    val action = query.delete
    db.run(action)
      .recover { case ex : java.sql.SQLException =>
        logger.debug(s" An error occurred while deleting rule code $ruleCode from distribution copy : ${ex.getLocalizedMessage}")
        -1
      }
  }

  def createDistributionCpyRules(newDistributionRules: Seq[AMHDistributionCpyRuleEntity]): Either[String, AMHDistributionCpyRulesCreated] = {
    try {
      val resp = Await.result(db.run(dao.amhDistributionCpyRules ++= newDistributionRules).map { _ => Right(AMHDistributionCpyRulesCreated(newDistributionRules)) }, 15.seconds)
      logger.debug(" finish with " + resp.b)
      resp
    } catch {
      case timeEx: TimeoutException => Left(s" Error while creating distribution copy rules DB not responding")
      case e: Exception             => Left(s" Error while creating distribution copy rules msg[$e.getMessage]")
    }
  }

  def findAllDistributionCpyRules: Option[Seq[AMHDistributionCpyRuleEntity]] = {
    val result = Await.result(db.run(dao.amhDistributionCpyRules.result), 10.seconds)
    Some(result)
  }


  def findAllDistributionRulesByEnvAndVersion(env : String, version : String): Option[Seq[AMHDistributionCpyRuleEntity]] = {
    val result = Await.result(db.run(dao.amhDistributionCpyRules.filter( rule => rule.env === env && rule.version === version).result), 10.seconds)
    Some(result)
  }


  def getDistributionRulesByDistributionCode(code: String): Option[Seq[AMHDistributionCpyRuleEntity]] = {
    val result = Await.result(db.run(dao.amhDistributionCpyRules.filter(_.code === code).take(200).result), 10.seconds)
    logger.debug("getDistributionRulesByDistributionCode => " + result)
    Some(result)
  }

  def deleteDistributionRulesByDistributionCode(code: String): Option[Int] = {
    val result = Await.result(db.run(dao.amhDistributionCpyRules.filter(_.code === code).delete), 10.seconds)
    logger.debug("deleteDistributionRulesByDistributionCode => " + result)
    Some(result)
  }

  def insertDistributionRules(assignmentRules: Seq[AMHDistributionCpyRuleEntity]): Either[String, AMHDistributionCpyRulesCreated] = {
    try {
      val result = Await.result(db.run(dao.amhDistributionCpyRules ++= assignmentRules).map { _ => Right(AMHDistributionCpyRulesCreated(assignmentRules)) }, 15.seconds)
      logger.debug("insertDistributionRules => " + result)
      result
    } catch {
      case timeEx: TimeoutException => Left(s" Error while creating amh distribution rules DB not responding")
      case e: Exception => Left(s" Error while creating amh distribution rules msg[$e.getMessage]")
    }
  }



  def deleteAll(): Option[Int] = {
    val result = Await.result(db.run(dao.amhDistributionCpyRules.delete), 10.seconds)
    Some(result)
  }

}