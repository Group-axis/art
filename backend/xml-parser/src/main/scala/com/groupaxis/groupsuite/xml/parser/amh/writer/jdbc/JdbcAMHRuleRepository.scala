package com.groupaxis.groupsuite.xml.parser.amh.writer.jdbc

import java.sql.SQLException

import com.groupaxis.groupsuite.commons.persistence.jdbc.DatabaseService

import scala.concurrent.{Await, Future}
import java.util.concurrent.TimeoutException

import com.groupaxis.groupsuite.routing.write.domain.model.amh.rule.{AMHRuleDAO, AMHRuleEntity}
import org.apache.logging.log4j.scala.Logging

object JdbcAMHRuleRepository {

  def apply(dao: AMHRuleDAO, databaseService: DatabaseService) { new JdbcAMHRuleRepository(dao, databaseService) }
}
case class AMHRulesCreated(rules: Seq[AMHRuleEntity])

class JdbcAMHRuleRepository(dao : AMHRuleDAO, databaseService : DatabaseService) extends Logging { //extends AMHBackendEntityTable

  import scala.concurrent.duration._
//  import scala.concurrent.ExecutionContext.Implicits.global
  import databaseService._
  import scala.concurrent.ExecutionContext.Implicits.global
  import slick.driver.PostgresDriver.api._

  def createRules(newRules: Seq[AMHRuleEntity]): Either[String, AMHRulesCreated] = {
    try {
      val f = dao.amhRules ++= newRules
      val resp = Await.result(db.run(f).map { _ => Right(AMHRulesCreated(newRules)) }, 15.seconds)
      logger.debug(" finish with " + resp.b)
      resp
    } catch {
      case timeEx: TimeoutException => Left(s" Error while creating backend DB not responding")
      case e: Exception =>
        e.printStackTrace()
        Left(s" Error while creating backend msg[$e.getMessage] - ${e.getCause} - ${e.getStackTrace}")

    }
  }

  def findAllNonDeletedRules: Option[Seq[AMHRuleEntity]] = {
    findAllRules("N")
  }

  def findAllDeletedRules: Option[Seq[AMHRuleEntity]] = {
    findAllRules("Y")
  }

  private def findAllRules(deleted : String): Option[Seq[AMHRuleEntity]] = {
    val result = Await.result(db.run(dao.amhRules.filter(rule => rule.deleted === deleted).result), 10.seconds)
    Some(result)
  }

  def deleteAll(): Option[Int] = {
    val result = Await.result(db.run(dao.amhRules.delete), 10.seconds)
    Some(result)
  }

  //TODO:PLEASE REMOVE THIS METHOD AND USE THE ONE IS IN TOOL
  private def currentDate : String = {
    import java.text.SimpleDateFormat
    val minuteFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    import java.util.Calendar
    val today = Calendar.getInstance().getTime
    minuteFormat.format(today)
  }

  def predicate(condition: Boolean)(fail: Exception): Future[Unit] =
    if (condition) Future( () ) else Future.failed(fail)

//  def DBIOPredicate(condition: Boolean)(action : DBIOAction[Nothing, NoStream, Effect])(fail: Exception): DBIOAction[Nothing, NoStream, Effect] =
//    if (condition) action else DBIO.failed(fail)
  def DBIOPredicate(condition: Boolean)(action : DBIOAction[Nothing, NoStream, Effect])(fail: Exception): DBIOAction[Nothing, NoStream, Effect] =
    if (condition) action else DBIO.failed(fail)

  def deleteRuleByCode(code : String) : Future[Int] = {

    val action  = for {
       rulesFound <- dao.amhRules.filter(rule => rule.code === code).result
       // test <- dao.amhRules += AMHRuleEntity("irach_testing")
       deletedCount <- {
         logger.debug(s"ruleFound $rulesFound nonEmpty ${rulesFound.nonEmpty} ")
         if (rulesFound.nonEmpty) {
           logger.debug(s"executing the delete")
           dao.amhRules.filter(rule => rule.code === code).delete
         } else {
           logger.debug(s"NO executing the delete")
           DBIO.failed(new SQLException(s"No rules found with code $code"))
         }
       }
       inserted <- {
         logger.debug(s"executing the insert")
         val rule = rulesFound.head
         dao.amhRules +=
           rule.copy(code="_" + currentDate, originalCode=Some(rule.code), deleted = "Y")
       }
    } yield inserted


    db.run(action.transactionally)
      .recover{
        case ex : java.sql.SQLException =>
          logger.debug(s"An error has occurred while deleting a rule code $code : ${ex.getLocalizedMessage }")
          -1
      }
  }
}