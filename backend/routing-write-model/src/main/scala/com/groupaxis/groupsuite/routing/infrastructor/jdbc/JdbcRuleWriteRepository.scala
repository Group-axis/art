package com.groupaxis.groupsuite.routing.infrastructor.jdbc

import java.util.concurrent.TimeoutException

import com.groupaxis.groupsuite.datastore.jdbc.Database
import com.groupaxis.groupsuite.routing.write.domain.model.routing.exit.point.ExitPoint
import com.groupaxis.groupsuite.routing.write.domain.model.routing.exit.point.ExitPointDAO.{apply => _, driver => _}
import com.groupaxis.groupsuite.routing.write.domain.model.routing.message.partner.MessagePartner
import com.groupaxis.groupsuite.routing.write.domain.model.routing.rule.RuleMessages._
import com.groupaxis.groupsuite.routing.write.domain.model.routing.schema.SchemaDAO._
import com.groupaxis.groupsuite.routing.write.domain.model.routing.rule._
import com.groupaxis.groupsuite.routing.write.domain.model.routing.schema.Schema
import com.groupaxis.groupsuite.routing.write.domain.{ImportSAARouting, SAARoutingImported}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, Future}
import scalaz.Kleisli.{apply => _}

object JdbcRuleWriteRepository {
  //  def apply(dao: RuleDAO, databaseService: DatabaseService) { new JdbcRuleWriteRepository(dao, databaseService) }
}

class JdbcRuleWriteRepository(database: Database, timeout: FiniteDuration) extends RuleWriteRepository with Logging {
  //  import scala.concurrent.duration._
  import com.groupaxis.groupsuite.routing.write.domain.model.routing.exit.point.ExitPointDAO.exitPoints
  import com.groupaxis.groupsuite.routing.write.domain.model.routing.rule.RuleDAO.rules

  import com.groupaxis.groupsuite.routing.write.domain.model.routing.message.partner.MessagePartnerDAO._
  import database.db
  import driver.api._

  import scala.concurrent.ExecutionContext.Implicits.global


  def getAllRules: Either[String, RulesFound] = {
    try {
      val result = Await.result(db.run(rules.result), timeout)
      Right(RulesFound(result))
    } catch {
      case timeEx: TimeoutException => Left(s" Error while looking for all rules DB not responding")
      case e: Exception => Left(s" Error while looking for all rules msg[$e.getMessage]")
    }
  }

  def getRules(pointName: String): Either[String, RulesFound] = {
    try {
      val result = Await.result(db.run(rules.filter(_.routingPointName === pointName).result), timeout)
      Right(RulesFound(result))
    } catch {
      case timeEx: TimeoutException => Left(s" Error while looking for rule's point $pointName DB not responding")
      case e: Exception => Left(s" Error while looking for rule's $pointName msg[$e.getMessage]")
    }
  }

  def getRuleByKey(pointName: String, sequence: Long): Either[String, RuleFound] = {
    try {
      val result: Option[RuleEntity] = Await.result(db.run(rules.filter(_.sequence === sequence).filter(_.routingPointName === pointName).result), timeout).headOption
      Right(RuleFound(result))
    } catch {
      case timeEx: TimeoutException => Left(s" Error while looking for a rule $sequence DB not responding")
      case e: Exception => Left(s" Error while looking for a rule $sequence msg[$e.getMessage]")
    }
  }

  def getRuleByRoutingPoint(routingPointName: String): Either[String, RuleEntity] = {
    try {
      val result = Await.result(db.run(rules.filter(_.routingPointName === routingPointName).result), timeout).headOption
      if (result.isEmpty) Left("Not rule found") else Right(result.get)
    } catch {
      case te: TimeoutException => Left(s" Error while searching rule by Routing point DB not responding")
      case e: Exception =>
        e.printStackTrace()
        Left(s" Error while searching rule by Routing point msg[$e.getMessage] - ${e.getCause} - ${e.getStackTrace}")
    }
  }

  def createRule(rule: RuleEntity): Either[String, RuleCreated] = {
    try {
      Await.result(db.run(rules returning rules += rule).map { rule => Right(RuleCreated(rule)) }, timeout)
    } catch {
      case timeEx: TimeoutException => Left(s" Error while creating rule DB not responding")
      case e: Exception => Left(s" Error while creating rule msg[$e.getMessage]")
    }
  }

  def createRules(rulesToCreate: Seq[RuleEntity]): Either[String, RulesCreated] = {
    try {
      Await.result(db.run(rules returning rules ++= rulesToCreate).map { rule => Right(RulesCreated(rulesToCreate)) }, timeout)
    } catch {
      case timeEx: TimeoutException => Left(s" Error while creating rules DB not responding")
      case e: Exception => Left(s" Error while creating rules msg[$e.getMessage]")
    }
  }

  def updateRule(pointName: String, sequence: Long, ruleUpdate: RuleEntityUpdate): Either[String, RuleUpdated] = {
    val eitherResponse = getRuleByKey(pointName, sequence)

    if (eitherResponse.isLeft) {
      Left(eitherResponse.left.get)
    } else {
      val ruleFound = eitherResponse.right.get
      ruleFound.rule match {
        case Some(rule) =>
          try {
            val updatedRule = ruleUpdate.merge(rule)
            Await.result(db.run(rules.filter(_.sequence === sequence).filter(_.routingPointName === pointName).update(updatedRule)), timeout)
            Right(RuleUpdated(updatedRule))
          } catch {
            case timeEx: TimeoutException => Left(s" Error while updating rule DB not responding")
            case e: Exception => Left(s" Error while updating rule msg[$e.getMessage]")
          }
        case None => Left("")
      }
    }
  }

  def deleteRule(pointName: String, sequence: Long): Either[String, Int] = {
    try {
      val result = Await.result(db.run(rules.filter(_.sequence === sequence).filter(_.routingPointName === pointName).delete), timeout)
      Right(result)
    } catch {
      case timeEx: TimeoutException => Left(s" Error while deleting rule sequence $sequence on point $pointName DB not responding")
      case e: Exception => Left(s" Error while deleting rule sequence $sequence on point $pointName  msg[$e.getMessage]")
    }
  }

  def deleteAllRules(): Either[String, Int] = {
    try {
      val result = Await.result(db.run(rules.delete), timeout)
      Right(result)
    } catch {
      case timeEx: TimeoutException => Left(s" Error while deleting all rules DB not responding")
      case e: Exception => Left(s" Error while deleting all rules msg[$e.getMessage]")
    }
  }

  def doImport(importSaaRouting : ImportSAARouting): Future[Either[String, SAARoutingImported]] = {
    //rules: Seq[RuleEntity], messagePartnersToInsert: Seq[MessagePartner], exitPointsToInsert: Seq[ExitPoint]

//    def updateCondition(condition : RuleCondition) : RuleCondition = if (condition.criteria.isDefined) condition else condition.copy(criteria=condition.conditionOn)

    def updateRuleCriteria(rule : RuleEntity) = if (rule.condition.criteria.isDefined) rule else rule.copy(condition = rule.condition.copy(criteria=rule.condition.conditionOn))

    val init =
      for {
        rulesDeleted <- rules.delete
        messagePartnersDeleted <- messagePartners.delete
        exitPointsDeleted <- exitPoints.delete
        schemasDeleted <- schemas.delete
        rules <- rules returning rules ++= importSaaRouting.points.flatMap(_.rules).map(updateRuleCriteria)
        msg <- messagePartners returning messagePartners ++= importSaaRouting.messagePartners
        exitPoint <- exitPoints returning exitPoints ++= importSaaRouting.exitPoints
        schemas <- schemas returning schemas ++= importSaaRouting.schemas
//        ee <- DBIOAction.successful(SAARoutingImported(rules, msg, exitPoint))
      } yield (rules, exitPoint, msg, schemas)

    logger.debug("DBIOAction done")
//    val rr: DBIOAction[Either[String, Int], NoStream, Effect.All] =
    val rr =  init.map(r => Some(r).toRight("Message Partner error while saving."))
    logger.debug("DBIOAction toRight done")

    val resp = db.run(rr.transactionally).map(v => v.productElement(0).asInstanceOf[(Seq[RuleEntity], Seq[ExitPoint], Seq[MessagePartner], Seq[Schema])])

    logger.debug("run done")
    resp.map(v => Right(SAARoutingImported(importSaaRouting.points,v._2,v._3, v._4)))

  }

}