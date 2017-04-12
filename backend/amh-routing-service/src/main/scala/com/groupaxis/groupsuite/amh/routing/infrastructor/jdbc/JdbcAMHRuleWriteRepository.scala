package com.groupaxis.groupsuite.amh.routing.infrastructor.jdbc

import java.util.concurrent.TimeoutException

import akka.actor.{Actor, Props}
import akka.stream.ActorMaterializer
import com.groupaxis.groupsuite.xml.parser.routing.infrastructure.util.XmlHelper
import com.groupaxis.groupsuite.amh.routing.interfaces.http.client.HttpAuditRoutingClient
import com.groupaxis.groupsuite.commons.persistence.jdbc.DatabaseService
import com.groupaxis.groupsuite.routing.write.domain.audit.messages.AMHRoutingAuditMessages.{CreateRule, UpdateRule}
import com.groupaxis.groupsuite.xml.parser.amh.writer.es.{ESAMHDistributionCpyRepository, ESAMHFeedbackDtnCpyRepository}
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import com.typesafe.config.Config
import org.apache.logging.log4j.scala.Logging
import org.joda.time.DateTime

import scala.util.Success
//import com.groupaxis.groupsuite.commons.protocol.worker.Worker.WorkComplete
import com.groupaxis.groupsuite.routing.write.domain.model.amh.rule.AMHRuleMessages._
import com.groupaxis.groupsuite.routing.write.domain.model.amh.rule.{AMHRuleDAO, AMHRuleEntity, AMHRuleEntityES, AMHRuleEntityUpdate}
import com.groupaxis.groupsuite.xml.parser.amh.writer.es.{ESAMHAssignmentRepository, ESAMHRuleRepository}

import scala.concurrent.Await

object JdbcAMHRuleWriteRepository {

  final val Name = "jdbc-amh-rule-write-repository"

  def props(dao: AMHRuleDAO, databaseService: DatabaseService, restClient : HttpAuditRoutingClient): Props = Props(classOf[JdbcAMHRuleWriteRepository], dao, databaseService, restClient )
}

class JdbcAMHRuleWriteRepository(val dao: AMHRuleDAO, val databaseService: DatabaseService, val restClient : HttpAuditRoutingClient) extends Actor with Logging {
  //  import JdbcRuleWriteRepository._
  import databaseService._
  import databaseService.driver.api._

  import scala.concurrent.duration._

  implicit val ec = context.dispatcher

  /*  def insert(user: User): DBIO[User] = for {
    pic <-
      if(user.picture.id.isEmpty) insert(user.picture)
      else DBIO.successful(user.picture)
    id <- usersAutoInc += (user.name, pic.id.get)
  } yield user.copy(picture = pic, id = id)
  */
  /** **************************************/

  //TODO: Move this to another actor to make the denormalization asynchronous
  val config : Config = context.system.settings.config
  import org.elasticsearch.common.settings.Settings
  val settings = Settings.settingsBuilder().put("cluster.name", config.getString("elastic.cluster.name")).build()
  val client : ElasticClient = ElasticClient.transport(settings, ElasticsearchClientUri(s"elasticsearch://"+config.getString("elastic.url")))

  val esAssignmentRepo: ESAMHAssignmentRepository = new ESAMHAssignmentRepository(client)
  val esDistributionRepo: ESAMHDistributionCpyRepository = new ESAMHDistributionCpyRepository(client)
  val esFeedbackRepo: ESAMHFeedbackDtnCpyRepository = new ESAMHFeedbackDtnCpyRepository(client)
  val esRuleRepo: ESAMHRuleRepository = new ESAMHRuleRepository(client)

  private def getRules: Either[String,Seq[AMHRuleEntity]] = {
    try {
      val result = Await.result(db.run(dao.amhRules.take(200).result), 10.seconds)
      Right(result)
    } catch { case e: Throwable => Left(s"An error occurred while reading rules: ${e.getMessage}")}
  }

//  private def deleteRuleByCode(code : String) : Future[Int] = {
//    val query = dao.amhRules.filter(_.code === code)
//    val action = query.delete
//    val affectedRows : Future[Int] = db.run(action)
//    affectedRows
//  }

  private def getRuleByCode(code: String): Either[String, AMHRuleFound] = {
    try {
      logger.debug("getRuleByCode " )
      val result: Option[AMHRuleEntity] = Await.result(db.run(dao.amhRules.filter(_.code === code).result), 5.seconds).headOption
      logger.debug("getRulesByCode => " + result)
      Right(AMHRuleFound(result))
    } catch {
      case timeEx: TimeoutException =>
        logger.debug(s"Error while looking for a rule $code DB not responding" )
        Left(s" Error while looking for a rule $code DB not responding")
      case e: Exception =>
        e.printStackTrace()
        logger.debug(s"Error while looking for a rule $code msg[${e.getMessage}]" )
        Left(s" Error while looking for a rule $code msg[${e.getMessage}]")

    }
  }

  //db.run(rules.filter(_.sequence=== sequence).result.headOption)

  //  def getRuleByRoutingPoint(routingPointName: String): Option[RuleEntity] = Await.result(db.run(rules.filter(_.routingPointName === routingPointName).result), 10.seconds).headOption
  //  def getUserByRoutingPoint(routingPointName: String): Future[Option[RuleEntity]] = db.run(rules.filter(_.routingPointName === routingPointName).result.headOption)

  private def createRule(rule: AMHRuleEntity): Either[String, AMHRuleCreated] = {
    try {
      val result = Await.result(db.run(dao.amhRules returning dao.amhRules += rule).map { rule => Right(AMHRuleCreated(rule)) }, 5.seconds)
      logger.debug("createRule => " + result)
      result
    } catch {
      case timeEx: TimeoutException => Left(s" Error while creating amh rule DB not responding")
      case e: Exception => Left(s" Error while creating amh rule msg[$e.getMessage]")
    }
  }

  //  def createRule(rule: RuleEntity): Future[RuleEntity] = db.run(rules returning rules += rule)

  private def updateRule(code: String, ruleUpdate: AMHRuleEntityUpdate): Either[String, AMHRuleUpdated] = {
    val eitherResponse = getRuleByCode(code)

    if (eitherResponse.isLeft) {
      Left(eitherResponse.left.get)
    } else {
      val ruleFound = eitherResponse.right.get
      ruleFound.rule match {
        case Some(rule) =>
          try {
            val updatedRule = removeReturnCharInExpression(ruleUpdate.merge(rule))
            Await.result(db.run(dao.amhRules.filter(_.code === code).update(updatedRule)), 10.seconds)
            Right(AMHRuleUpdated(updatedRule))
          } catch {
            case timeEx: TimeoutException => Left(s" Error while updating amh rule DB not responding")
            case e: Exception => Left(s" Error while updating amh rule msg[$e.getMessage]")
          }
        case None => Left("")
      }
    }
  }

//  def deleteRule(code: String): Int = Await.result(db.run(dao.amhRules.filter(_.code === code).delete), 10.seconds)

  implicit val system = context.system
  implicit val materializer = ActorMaterializer()

  private def removeReturnCharInExpression(rule : AMHRuleEntity) =
    rule.expression match {
      case Some(express) => rule.copy(expression = Some(XmlHelper.removeCharacters(express,Seq("\n","\r"))))
      case _ => rule
    }


  def receive: Receive = {
    case CreateAMHRule(code, ruleEntityUpdate, userId, datetime) =>
      logger.info(s" receiving create($ruleEntityUpdate) on JdbcAMHRuleWriteRepository")
      val username = userId.getOrElse("no_user_defined")
      val dateTime = datetime.getOrElse(DateTime.now)

      val result = createRule(removeReturnCharInExpression(ruleEntityUpdate.merge(code)))
      result.fold(
          errorMsg => {
            logger.info("Rule creation failed with " + errorMsg)
          },
          ruleCreated => {
            logger.info(s"rule created!")
            try {
              val auditCreateRule = CreateRule(username, dateTime, ruleCreated.response)
              restClient.sendRuleCreation(auditCreateRule)
                .onComplete {
                  case Success(resp) =>
                    resp.fold(
                      error => logger.error(s"While auditing rule creation: $error"),
                      done => logger.info(s" Audit rule creation success")
                    )
                  case error => logger.error(s"While auditing rule creation: $error")
                }
              esRuleRepo.insert(ruleCreated.response.toES(Some(false)))
              logger.info("rule was created into ES ")
            } catch {
              case e: Exception =>
                logger.error("rule was not saved into ES : " + e.getMessage)
            }
        }
      )
//      sender() ! WorkComplete(result)
      sender() ! result
    case UpdateAMHRule(code, ruleEntityUpdate, userId, datetime) =>
      logger.info(s" receiving $ruleEntityUpdate on JdbcAMHRuleWriteRepository")
      // auditing code
      val username = userId.getOrElse("no_user_defined")
      val dateTime = datetime.getOrElse(DateTime.now)
      val oldRuleResp : Option[AMHRuleEntity] = getRuleByCode(code).fold(err => None, ruleFound => ruleFound.rule)

      val result = updateRule(code, ruleEntityUpdate)
      logger.info(s"amh rule $result updated")
      result.fold(
        errorMsg => {
          logger.info("Rule update failed with " + errorMsg)
        },
        ruleUpdated => {
          try {
            logger.info(s"rule updated, now updating audit with old: $oldRuleResp new: ${ruleUpdated.response}")
            val auditUpdateRule = UpdateRule(username, dateTime, oldRuleResp.getOrElse(AMHRuleEntity(code)),ruleUpdated.response)
            restClient.sendRuleUpdate(auditUpdateRule)
              .onComplete {
                case Success(resp) =>
                  resp.fold(
                    error => logger.error(s"While auditing rule update: $error"),
                    done => logger.info(s" Audit rule update success")
                  )
                case error => logger.error(s"While auditing rule update: $error")
              }

            logger.info(s"rule updated, now trying to update ES model")
            val ruleES: AMHRuleEntityES = ruleUpdated.response.toES(None)
            esRuleRepo.updateRule(ruleES)
            logger.info("rule was updated into ES, now trying to update all assignments affected")
            val rowsAffected : Int = esAssignmentRepo.updateAssignments(ruleES)
            logger.info(rowsAffected + " assignment(s) affected")
            val distRowsAffected : Int = esDistributionRepo.updateDistributionCps(ruleES)
            logger.info(distRowsAffected + " distribution(s) affected")
            val feedRowsAffected : Int = esFeedbackRepo.updateFeedbackDtnCps(ruleES)
            logger.info(feedRowsAffected + " feedback(s) affected")
          } catch {
            case e: Exception =>
              logger.error("rule was not saved into ES : " + e.getMessage)
          }
        }
      )
//      sender() ! WorkComplete(result)
      sender() ! result

    case FindAMHRuleByCode(code) =>
      logger.debug(" receiving FindRuleBySequence on JdbcRuleWriteRepository")
      val result = getRuleByCode(code)
      logger.debug("retrieving amh rule $result")
//      sender() ! WorkComplete(result)
      sender() ! result

    case findAll : FindAllAMHRules =>
      logger.debug(" * receiving FindAllAMHRules *  on JdbcAMHRuleWriteRepository")
      val result = getRules match {
        case Left(ex) => Left(ex)
        case Right(list) => Right(AMHRulesFound(Some(list)))
      }
      sender() ! result
    case a : Any =>
      logger.debug("That's why")
      logger.debug(s" received $a")
      logger.debug("MSG not supported!!! ")
      logger.debug(s"$a")
  }

}