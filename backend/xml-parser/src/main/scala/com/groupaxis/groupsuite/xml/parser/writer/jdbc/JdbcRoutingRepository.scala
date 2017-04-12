package com.groupaxis.groupsuite.xml.parser.writer.jdbc

import com.groupaxis.groupsuite.commons.persistence.jdbc.DatabaseService
import com.groupaxis.groupsuite.routing.write.domain.model.routing.point.Point
import scala.concurrent.Await
import com.groupaxis.groupsuite.routing.write.domain.model.routing.rule.RuleEntity
import java.util.concurrent.TimeoutException

object JdbcRoutingRepository {

  def apply(databaseService: DatabaseService) { new JdbcRoutingRepository(databaseService) }
}
case class RuleCreated(rule: RuleEntity)
case class RulesCreated(rules: Seq[RuleEntity])

class JdbcRoutingRepository(databaseService: DatabaseService) {

  import scala.concurrent.duration._
  import scala.concurrent.ExecutionContext.Implicits.global
  import databaseService._
  import databaseService.driver.api._

//  def createPoints(newPoints: Seq[Point]): Either[String, RulesCreated] = {
//    val rules: Seq[RuleEntity] = newPoints.flatMap(point => point.rules)
//    createRules(rules)
//  }
//  def findAllRules: Option[Seq[RuleEntity]] = {
//    val result = Await.result(db.run(rules.result), 10.seconds)
//    Some(result)
//  }
//
//  def findRulesByRoutingPoint(pointName: String): Option[Seq[RuleEntity]] = {
//    val result = Await.result(db.run(rules.filter(_.routingPointName === pointName).result), 10.seconds)
//    Some(result)
//  }
//
//  private def createRules(newRules: Seq[RuleEntity]): Either[String, RulesCreated] = {
//    try {
//      val resp = Await.result(db.run(rules ++= newRules).map { _ => Right(RulesCreated(newRules)) }, 15.seconds)
//      logger.debug(" finish with " + resp.b)
//      resp
//    } catch {
//      case timeEx: TimeoutException => Left(s" Error while creating rule DB not responding")
//      case e: Exception             => Left(s" Error while creating rule msg[$e.getMessage]")
//    }
//  }
//
//  private def createRule(rule: RuleEntity): Either[String, RuleCreated] = {
//    try {
//      Await.result(db.run(rules returning rules += rule).map { rule => Right(RuleCreated(rule)) }, 5.seconds)
//    } catch {
//      case timeEx: TimeoutException => Left(s" Error while creating rule DB not responding")
//      case e: Exception             => Left(s" Error while creating rule msg[$e.getMessage]")
//    }
//  }

}