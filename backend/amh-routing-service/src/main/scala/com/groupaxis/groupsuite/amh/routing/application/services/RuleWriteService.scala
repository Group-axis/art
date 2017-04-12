package com.groupaxis.groupsuite.amh.routing.application.services

import akka.actor.SupervisorStrategy.{Restart, Resume, Stop}
import akka.actor.{Actor, OneForOneStrategy, Props}
import akka.pattern.ask
import akka.stream.Supervision
import akka.util.Timeout
import com.groupaxis.groupsuite.amh.routing.infrastructor.es.ESRuleWriteRepository
import com.groupaxis.groupsuite.amh.routing.infrastructor.file.{AssignmentWriteRepository, FileDistributionCpyWriteRepository, FileFeedbackDtnCpyWriteRepository}
import com.groupaxis.groupsuite.amh.routing.interfaces.http.client.HttpAuditRoutingClient
import com.groupaxis.groupsuite.routing.write.domain.model.amh.rule.AMHRuleMessages.{CreateRuleOverviewCSVFile, RuleUnAssigned, UnAssignRule}
import com.groupaxis.groupsuite.xml.parser.amh.writer.jdbc.JdbcAMHRuleRepository
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import com.typesafe.config.Config
import org.apache.logging.log4j.scala.Logging
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}


object RuleWriteService {

  final val Name = "rule-write-service"

  def props(ruleRepo: JdbcAMHRuleRepository, assignRepo: AssignmentWriteRepository, distributionRepo: FileDistributionCpyWriteRepository, feedbackRepo: FileFeedbackDtnCpyWriteRepository, restClient : HttpAuditRoutingClient): Props = Props(classOf[RuleWriteService], ruleRepo, assignRepo, distributionRepo, feedbackRepo, restClient)

}

class RuleWriteService(ruleRepo: JdbcAMHRuleRepository, assignRepo: AssignmentWriteRepository, distributionRepo: FileDistributionCpyWriteRepository, feedbackRepo: FileFeedbackDtnCpyWriteRepository, val restClient : HttpAuditRoutingClient) extends Actor with Logging {

  import scala.concurrent.duration._

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1.minute) {
      case _: ArithmeticException => Resume
      case _: NullPointerException => Restart
      case _: IllegalArgumentException => Stop
      case e: Exception =>
        logger.warn("[HttpRoutingService] Exception has been received, so restarting the actor " + e.getMessage)
        e.printStackTrace()
        Restart
    }

  val decider: Supervision.Decider = (ex) => Supervision.Resume

  implicit val ec: ExecutionContext = context.dispatcher

  //TODO: Move this to another actor to make the de-normalization asynchronous
  val config: Config = context.system.settings.config
  implicit val timeout: Timeout = 50.seconds

  import org.elasticsearch.common.settings.Settings

  val settings = Settings.settingsBuilder().put("cluster.name", config.getString("elastic.cluster.name")).build()
  val client: ElasticClient = ElasticClient.transport(settings, ElasticsearchClientUri(s"elasticsearch://" + config.getString("elastic.url")))

  val esRuleWriteRepo = context.actorOf(ESRuleWriteRepository.props(client), ESRuleWriteRepository.Name)

  private def unAssignRule(ruleCode: String, userId : Option[String], date : Option[DateTime]): Future[RuleUnAssigned] = {

    val rowDeleted = for {
      unAssignAssignCount <- assignRepo.unAssignRule(ruleCode)
      unAssignDtnCount <- distributionRepo.unAssignRule(ruleCode)
      unAssignFeedCount <- feedbackRepo.unAssignRule(ruleCode)
      ruleDeleted <- ruleRepo.deleteRuleByCode(ruleCode)
    } yield {
      if (unAssignAssignCount > -1 && unAssignDtnCount > -1
        && unAssignFeedCount > -1 && ruleDeleted > -1)
        unAssignAssignCount + unAssignDtnCount + unAssignFeedCount + ruleDeleted
      else -1
    }

    rowDeleted onComplete {
      case Success(count) if count > 0 =>
        logger.info(s"$count row(s) were successfully deleted!")
        esRuleWriteRepo ! UnAssignRule(ruleCode, userId, date)
        logger.info(s" E-S synchronization for rule $ruleCode sent!")
      case Success(count) if count == 0 =>
        logger.info(s"No rule(s) were found with code $ruleCode, therefore not E-S synchronization needed!")
      case Success(count) if count == -1 =>
        logger.error(s"No rule(s) were found with code $ruleCode")
      case Failure(t) =>
        logger.error(s"There was an DB error while removing the rule $ruleCode")
    }

    rowDeleted.map(count => RuleUnAssigned(count))
      .recover { case ex: Throwable => RuleUnAssigned(-1) }
  }

  def receive: Receive = {

    case UnAssignRule(code, userId, date) =>
      logger.info(s"UnAssignRule('$code') received")
      val origSender = sender()
      unAssignRule(code, userId, date) onSuccess {
        case ruleUnAssigned => origSender ! ruleUnAssigned
      }

    case CreateRuleOverviewCSVFile(assignedValue, username, date) =>
      logger.info(s"CreateRuleOverviewCSVFile('$assignedValue', '$username') received")
      val origSender = sender()
      (esRuleWriteRepo ? CreateRuleOverviewCSVFile(assignedValue, username, date))
        .onComplete {
          case Success(created) => origSender ! created
        }
  }

}