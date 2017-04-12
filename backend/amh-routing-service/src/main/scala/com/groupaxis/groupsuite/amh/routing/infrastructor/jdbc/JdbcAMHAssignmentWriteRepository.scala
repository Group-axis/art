package com.groupaxis.groupsuite.amh.routing.infrastructor.jdbc

import java.util.concurrent.TimeoutException

import akka.actor.SupervisorStrategy.{Restart, Resume, Stop}
import akka.actor.{Actor, OneForOneStrategy, Props}
import akka.stream.{ActorMaterializer, Supervision}
import com.groupaxis.groupsuite.amh.routing.infrastructor.es.ESRuleWriteRepository
import com.groupaxis.groupsuite.amh.routing.interfaces.http.client.HttpAuditRoutingClient
import com.groupaxis.groupsuite.commons.persistence.jdbc.DatabaseService
import com.groupaxis.groupsuite.routing.amh.util.RuleUtil
import com.groupaxis.groupsuite.routing.write.domain.audit.messages.AMHRoutingAuditMessages.{CreateBackendAssignment, UpdateBackendAssignment}
import com.groupaxis.groupsuite.routing.write.domain.model.amh.rule.AMHRule
import com.groupaxis.groupsuite.routing.write.domain.model.amh.rule.AMHRuleMessages.{SetRulesAsAssigned, SetRulesAsUnassigned}
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import com.typesafe.config.Config
import org.apache.logging.log4j.scala.Logging
import org.joda.time.DateTime

import scala.concurrent.Future
import scala.util.Success
//import com.groupaxis.groupsuite.commons.protocol.worker.Worker.WorkComplete
import akka.pattern.ask
import akka.util.Timeout
import com.groupaxis.groupsuite.routing.write.domain.model.amh.assignment.AMHAssignmentMessages._
import com.groupaxis.groupsuite.routing.write.domain.model.amh.assignment._
import com.groupaxis.groupsuite.routing.write.domain.model.amh.rule.AMHRuleMessages.{AMHRulesFound, FindAllAMHRules}
import com.groupaxis.groupsuite.routing.write.domain.model.amh.rule.{AMHRuleDAO, AMHRuleEntity}
import com.groupaxis.groupsuite.xml.parser.amh.writer.es.ESAMHAssignmentRepository

import scala.concurrent.Await

object JdbcAMHAssignmentWriteRepository {

  final val Name = "jdbc-amh-assignment-write-repository"

  def props(assignmentDAO: AssignmentDAO, ruleAssignmentDAO: AssignmentRuleDAO, databaseService: DatabaseService, restClient : HttpAuditRoutingClient): Props = Props(classOf[JdbcAMHAssignmentWriteRepository], assignmentDAO, ruleAssignmentDAO, databaseService, restClient)
}

class JdbcAMHAssignmentWriteRepository(val assignmentDAO: AssignmentDAO, val ruleAssignmentDAO: AssignmentRuleDAO, val databaseService: DatabaseService, val restClient : HttpAuditRoutingClient) extends Actor with Logging {

  import databaseService._
  //import databaseService.driver.api._
  import slick.driver.PostgresDriver.api._

  import scala.concurrent.duration._

  //TODO: Move this to another actor to make the de-normalization asynchrinous
  val config : Config = context.system.settings.config
  import org.elasticsearch.common.settings.Settings
  val settings = Settings.settingsBuilder().put("cluster.name", config.getString("elastic.cluster.name")).build()
  val client : ElasticClient = ElasticClient.transport(settings, ElasticsearchClientUri(s"elasticsearch://"+config.getString("elastic.url")))
  val esAssignmentRepo: ESAMHAssignmentRepository = new ESAMHAssignmentRepository(client)

  //TODO:Remove AMHRule creation and move it to the same actor than esAssignmentRepo
  val ruleDao : AMHRuleDAO = AMHRuleDAO
  val ruleRepo = context.actorOf(JdbcAMHRuleWriteRepository.props(ruleDao, databaseService, restClient),JdbcAMHRuleWriteRepository.Name+"_temp")

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1.minute) {
      case _: ArithmeticException => Resume
      case _: NullPointerException => Restart
      case _: IllegalArgumentException => Stop
      case e: Exception =>
        logger.warn(s"[JdbcAMHAssignmentWriteRepository] Exception has been received, so restarting the actor ${e.getMessage}" )
        e.printStackTrace()
        Restart
    }

  val decider: Supervision.Decider = (ex) => Supervision.Resume

  val esRuleWrite = context.actorOf(ESRuleWriteRepository.props(client), ESRuleWriteRepository.Name)

//  private def unassignRuleByCode(ruleCode : String) : Future[RuleUnassigned] = {
//    val query = ruleAssignmentDAO.amhAssignmentRules.filter(_.ruleCode === ruleCode)
//    val action = query.delete
//    db.run(action).map(count => RuleUnassigned(count))
//      .recover { case ex : java.sql.SQLException =>
//          logger.error(s" An error occurred while unassigning rule code $ruleCode from assignments : ${ex.getLocalizedMessage}")
//          RuleUnassigned(-1)
//      }
//  }

  def getAssignments : Option[Seq[AMHAssignmentEntity]] = {
    val result = Await.result(db.run(assignmentDAO.amhAssignments.take(200).result), 10.seconds)
    logger.info(s"getAssignments => $result")
    Some(result)
  }

  implicit val ec = context.dispatcher

  private def getAssignmentRules(code: String): Seq[AMHAssignmentRuleEntity] = {
    import ruleAssignmentDAO.driver.api._
    val result = Await.result(db.run(ruleAssignmentDAO.amhAssignmentRules.filter(_.code === code).take(200).result), 10.seconds)
    logger.info(s"getAssignmentRules => $result")
    result
  }

  private def deleteAssignmentRules(code: String): Option[Int] = {
    val result = Await.result(db.run(ruleAssignmentDAO.amhAssignmentRules.filter(_.code === code).delete), 10.seconds)
    logger.info(s"deleteAssignmentRules => $result")
    Some(result)
  }

  private def createAssignmentRules(assignmentRules: Seq[AMHAssignmentRuleEntity]): Either[String, AMHAssignmentRulesCreated] = {
    try {
      val result = Await.result(db.run(ruleAssignmentDAO.amhAssignmentRules ++= assignmentRules).map { _ => Right(AMHAssignmentRulesCreated(assignmentRules)) }, 15.seconds)
      logger.info(s"createAssignmentRules => $result")
      result
    } catch {
      case timeEx: TimeoutException => Left(s" Error while creating amh assignment rules DB not responding")
      case e: Exception => Left(s" Error while creating amh assignment rules msg[$e.getMessage]")
    }
  }

  def getAssignmentByCode(code: String): Either[String, AMHAssignmentFound] = {
    try {
      val result: Option[AMHAssignmentEntity] = Await.result(db.run(assignmentDAO.amhAssignments.filter(_.code === code).result), 5.seconds).headOption
      logger.info(s"getAssignmentByCode => $result")
      Right(AMHAssignmentFound(result))
    } catch {
      case timeEx: TimeoutException => Left(s" Error while looking for a assignment $code DB not responding")
      case e: Exception => Left(s" Error while looking for a assignment $code msg[$e.getMessage]")
    }
  }

  def createAssignment(assignment: AMHAssignmentEntity): Either[String, AMHAssignmentCreated] = {
    try {
      logger.info("rules to create " + assignment.rules.length)
      val response = Await.result(db.run(assignmentDAO.amhAssignments returning assignmentDAO.amhAssignments += assignment).map { assignment => Right(AMHAssignmentCreated(assignment)) }, 5.seconds)
      createAssignmentRules(assignment.rules)
      response
    } catch {
      case timeEx: TimeoutException => Left(s" Error while creating amh assignment DB not responding")
      case e: Exception => Left(s" Error while creating amh assignment msg[$e.getMessage]")
    }
  }

  def updateAssignment(code: String, assignmentUpdate: AMHAssignmentEntityUpdate, originalRules : Seq[AMHAssignmentRuleEntity]): Either[String, AMHAssignmentUpdated] = {
    val eitherResponse = getAssignmentByCode(code)

    if (eitherResponse.isLeft) {
      Left(eitherResponse.left.get)
    } else {
      val assignmentFound = eitherResponse.right.get
      assignmentFound.assignment match {
        case Some(assignment) =>
          try {
            val fullAssignment = assignment.copy(rules = originalRules)
            val updatedAssignment = assignmentUpdate.merge(fullAssignment)
            Await.result(db.run(assignmentDAO.amhAssignments.filter(_.code === code).update(updatedAssignment)), 10.seconds)
            deleteAssignmentRules(code)
            createAssignmentRules(updatedAssignment.rules)
            Right(AMHAssignmentUpdated(updatedAssignment))
          } catch {
            case timeEx: TimeoutException => Left(s" Error while updating amh assignment DB not responding")
            case e: Exception => Left(s" Error while updating amh assignment msg[$e.getMessage]")
          }
        case None => Left("")
      }
    }
  }

  def getRuleExpression(rules : Seq[AMHRuleEntity])(code : String) : String = {
    //logger.debug("looking for code F "+ code + " in " +rules)
    val foundExpression = rules.filter(_.code.equalsIgnoreCase(code)).map(_.expression.getOrElse("")).headOption
    foundExpression.getOrElse("")
  }

  def ruleFinder: (String) => String = {
    implicit val timeout: Timeout = 50.seconds
    logger.info("calling FindAllAMHRules ")
//    logger.debug("calling FindAllAMHRules ")
//    val workDone : WorkComplete = Await.result((ruleRepo ?  FindAllAMHRules()).mapTo[WorkComplete], 5.seconds)
//    val rulesFound = workDone.result.asInstanceOf[AMHRulesFound]
//    getRuleExpression(rulesFound.rules.getOrElse(Seq()))_

    val rulesFound = Await.result((ruleRepo ?  FindAllAMHRules()).mapTo[Either[String,AMHRulesFound]], 50.seconds)
    val rules = rulesFound match {
      case Left(ex) => logger.error(s"An error while looking for rules: $ex"); Seq()
      case Right(amhRulesFound) => amhRulesFound.rules.getOrElse(Seq())
    }
    getRuleExpression(rules)
  }

  implicit val system = context.system
  implicit val materializer = ActorMaterializer()

  def receive: Receive = {
    case CreateAMHAssignment(code, assignmentEntityUpdate, userId, datetime) =>
      logger.info(s" receiving create($assignmentEntityUpdate) on JdbcAMHAssignmentWriteRepository")
      val assignmentToCreate = assignmentEntityUpdate.merge(code)

      val result = createAssignment(assignmentToCreate)
      logger.info(s"assignment $result created")


      result.fold(
        errorMsg => {
          logger.info("Assignment creation failed with " + errorMsg)
        },
        assignmentCreated => {
          logger.info(s"assignment created!")

          try {

            logger.info(s"Now updating audit with new: ${assignmentCreated.response}")
            val username = assignmentCreated.response.creationUserId.getOrElse("no_user_defined")
            val dateTime = assignmentCreated.response.creationDate
            val auditCreateBack = CreateBackendAssignment(username, dateTime, assignmentCreated.response)
            restClient.sendBackendCreation(auditCreateBack)
              .onComplete {
                case Success(resp) =>
                  resp.fold(
                    error => logger.error(s"While auditing backend assignment create: $error"),
                    done => logger.info(s" Audit backend assignment create success")
                  )
                case error => logger.error(s"While auditing backend assignment create: $error")
              }

            val finder = ruleFinder
            val assignmentRules = assignmentToCreate.rules.map(
              { rule : AMHAssignmentRuleEntity => {
//                logger.info("finding ruleCode "+rule.ruleCode)
                rule.toES(finder(rule.ruleCode))
              }
              })


//            val rulesFound : AMHRulesFound = Await.result((ruleRepo ? FindAllAMHRules()).mapTo[AMHRulesFound], 5.seconds)
//            val findExpression = getRuleExpression(rulesFound.rules.getOrElse(Seq()))_
//            logger.debug("findExpression " + findExpression)
//            val assignmentRules : Seq[AMHAssignmentRuleEntity] = assignmentCreated.response.rules
//            logger.debug("assignmentRules "+ assignmentRules)
//            val dd = assignmentRules.map(
//                  { rule : AMHAssignmentRuleEntity => {
//                    logger.debug(" rule to upda "+rule)
//                    rule.toES(findExpression(rule.ruleCode))}})
//            logger.debug(" dd "+dd)
            esAssignmentRepo.insert(assignmentCreated.response.toES(assignmentRules))
            logger.info("assignment was created into ES ")
            esRuleWrite ! SetRulesAsAssigned(assignmentToCreate.rules)
          } catch {
            case e: Exception =>
              logger.error("assignment was not saved into ES : " + e.getMessage)
          }
        }
      )
      sender() ! result

    case UpdateAMHAssignment(code, assignmentEntityUpdate, userId, datetime) =>
      logger.info(s" receiving create($assignmentEntityUpdate) on JdbcAMHAssignmentWriteRepository")

      val oldAssignment = getAssignmentByCode(code).fold(err => None, resp => resp.assignment)

      val originalRules = getAssignmentRules(code)
      val result = updateAssignment(code, assignmentEntityUpdate, originalRules)
      result.fold(
        errorMsg => {
          logger.error("Assignment creation failed with " + errorMsg)
        },
        assignmentUpdated => {
          logger.info("assignment updated!")

          try {
            logger.info(s"assignment updated, now updating audit with old: $oldAssignment new: ${assignmentUpdated.response}")
            val notFoundAssign = AMHAssignmentEntity(code, -1, "backCode","backDirection",Some("Y"), Some(""), Some(""),Some("edescription"), "","")
            val username = assignmentUpdated.response.modificationUserId.getOrElse("no_user_defined")
            val dateTime = assignmentUpdated.response.modificationDate.getOrElse(DateTime.now)

            val auditUpdateAssign = UpdateBackendAssignment(username, dateTime, oldAssignment.getOrElse(notFoundAssign),assignmentUpdated.response)
            restClient.sendBackendUpdate(auditUpdateAssign)
              .onComplete {
                case Success(resp) =>
                  resp.fold(
                    error => logger.error(s"While auditing assignment update: $error"),
                    done => logger.info(s" Audit assignment update success")
                  )
                case error => logger.error(s"While auditing assignment update: $error")
              }

            implicit val timeout: Timeout = 50.seconds
            logger.info("calling FindAllAMHRules ")
            val findAll : FindAllAMHRules = FindAllAMHRules()
            val rulesFound = Await.result((ruleRepo ?  findAll).mapTo[Either[String, AMHRulesFound]], 50.seconds)
            val rules = rulesFound match {
              case Left(ex) => logger.error(ex) ;  Seq()
              case Right(amhRulesFound) => amhRulesFound.rules.getOrElse(Seq())
            }
            val findExpression = getRuleExpression(rules)_

            val assignmentRules = assignmentUpdated.response.rules.map(
              { rule => rule.toES(findExpression(rule.ruleCode))})

            for {
              esUpdateResp <- esAssignmentRepo.updateAssignment(assignmentUpdated.response.toES(assignmentRules))
              (removed : Option[Seq[AMHRule]], added: Option[Seq[AMHRule]]) <- Future { RuleUtil.getRemovedAddedPair(originalRules, assignmentUpdated.response.rules) }
            } yield {
              logger.info("assignment was created into ES ")
              logger.info(s"Sending update rule unassign $removed")
              removed
                .filter(rules => rules.nonEmpty)
                .foreach(rules =>esRuleWrite ! SetRulesAsUnassigned(rules))
              logger.info(s"Sending update rule assign $added")
              added
                .filter(rules => rules.nonEmpty)
                .foreach(rules => esRuleWrite ! SetRulesAsAssigned(rules))

            }
          } catch {
            case e: Exception =>
              logger.error("assignment was not updated in ES : " + e.getMessage)
          }
        }
      )
//      sender() ! WorkComplete(result)
      sender() ! result

    case FindAMHAssignmentByCode(code) =>
      logger.info(s" receiving FindAssignmentBySequence($code) on JdbcAssignmentWriteRepository")
      val result = getAssignmentByCode(code)
      logger.info(s"retrieving amh assignment $result")
//      sender() ! WorkComplete(result)
      sender() ! result

    case FindAllAMHAssignments() =>
      logger.info(s" receiving FindAllAssignments on JdbcAMHAssignmentWriteRepository")
      val result = getAssignments
      logger.info(s"retrieving amh assignment $result")
//      sender() ! WorkComplete(AMHAssignmentsFound(result))
      sender() ! AMHAssignmentsFound(result)
//    case UnassignRuleByCode(ruleCode) =>
//      sender() ! unassignRuleByCode(ruleCode)
  }

}