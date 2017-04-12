package com.groupaxis.groupsuite.xml.parser.amh.writer.jdbc

import com.groupaxis.groupsuite.commons.persistence.jdbc.DatabaseService

import scala.concurrent.{Await, Future}
import java.util.concurrent.TimeoutException

import com.groupaxis.groupsuite.routing.write.domain.model.amh.assignment.{AMHAssignmentRuleEntity, AssignmentRuleDAO}
import org.apache.logging.log4j.scala.Logging

object JdbcAMHAssignmentRuleRepository {

  def apply(dao: AssignmentRuleDAO, databaseService: DatabaseService) { new JdbcAMHAssignmentRuleRepository(dao,databaseService) }
}
case class AMHAssignmentRulesCreated(rules: Seq[AMHAssignmentRuleEntity])

class JdbcAMHAssignmentRuleRepository(dao : AssignmentRuleDAO, databaseService: DatabaseService) extends Logging {

  import scala.concurrent.duration._
  import scala.concurrent.ExecutionContext.Implicits.global
  import databaseService._
//  import databaseService.driver.api._
  import slick.driver.PostgresDriver.api._

  def deleteAssignmentRulesByRuleCode(ruleCode : String) : Future[Int] = {
    val query = dao.amhAssignmentRules.filter(_.ruleCode === ruleCode)
    val action = query.delete
    db.run(action)
      .recover { case ex : java.sql.SQLException =>
        logger.debug(s" An error occurred while deleting rule code $ruleCode from assignments : ${ex.getLocalizedMessage}")
        -1
      }
  }

  def createAssignmentRules(newAssignmentRules: Seq[AMHAssignmentRuleEntity]): Either[String, AMHAssignmentRulesCreated] = {
    try {
      val resp = Await.result(db.run(dao.amhAssignmentRules ++= newAssignmentRules).map { _ => Right(AMHAssignmentRulesCreated(newAssignmentRules)) }, 15.seconds)
      logger.debug(" finish with " + resp.b)
      resp
    } catch {
      case timeEx: TimeoutException => Left(s" Error while creating assignment rules DB not responding")
      case e: Exception             => Left(s" Error while creating assignment rule msg[$e.getMessage]")
    }
  }

  def findAllAssignmentRules: Option[Seq[AMHAssignmentRuleEntity]] = {
    val result = Await.result(db.run(dao.amhAssignmentRules.result), 10.seconds)
    Some(result)
  }

  def deleteAll(): Option[Int] = {
    val result = Await.result(db.run(dao.amhAssignmentRules.delete), 10.seconds)
    Some(result)
  }

}