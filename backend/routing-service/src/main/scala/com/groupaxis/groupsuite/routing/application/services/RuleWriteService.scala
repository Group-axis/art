package com.groupaxis.groupsuite.routing.application.services

import akka.actor.SupervisorStrategy.{Restart, Resume, Stop}
import akka.actor.{Actor, OneForOneStrategy, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.groupaxis.groupsuite.routing.infrastructor.es.ESRuleWriteRepository
import com.groupaxis.groupsuite.routing.infrastructor.jdbc.JdbcRuleWriteRepository
import com.groupaxis.groupsuite.routing.write.domain.model.routing.rule.RuleMessages._
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import com.typesafe.config.Config
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}

object RuleWriteService {

  final val Name = "rule-write-service"

  def props(ruleRepo : JdbcRuleWriteRepository): Props = Props(classOf[RuleWriteService], ruleRepo)

}

class RuleWriteService(ruleRepo : JdbcRuleWriteRepository) extends Actor with Logging {
  //import RuleWriteService._
  implicit val ec: ExecutionContext = context.dispatcher
  implicit val timeout = Timeout(5.seconds)

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1.minute) {
      case _: ArithmeticException      => Resume
      case _: NullPointerException     => Restart
      case _: IllegalArgumentException => Stop
      case e: Exception                =>
        logger.warn("[RuleWriteService] Exception has been received, so restarting the actor " + e.getMessage)
        e.printStackTrace()
        Restart
    }

  //TODO: Move this to another actor to make the denormalization asynchronous
  val config : Config = context.system.settings.config
  import org.elasticsearch.common.settings.Settings
  val settings = Settings.settingsBuilder().put("cluster.name", config.getString("elastic.cluster.name")).build()
  val client : ElasticClient = ElasticClient.transport(settings, ElasticsearchClientUri(s"elasticsearch://"+config.getString("elastic.url")))
  val esRuleWriteRepository = context.actorOf(ESRuleWriteRepository.props(client), ESRuleWriteRepository.Name)

    def receive: Receive = {
      case CreateRule(pointName, sequence, ruleEntityUpdate) =>
        logger.info(s" receiving create($ruleEntityUpdate) on RuleWriteService")
        val result = ruleRepo.createRule(ruleEntityUpdate.merge(sequence, pointName))
        result.fold(
          errorMsg => {
            logger.info("Rule creation failed with " + errorMsg)
          },
          ruleCreated => {
            try {
              logger.info(s"rule $result created, now it will be inserted into ES")
              Await.result((esRuleWriteRepository ? InsertRuleES(pointName, ruleCreated.response.toES)).mapTo[RuleESInserted], 5.seconds)
              logger.info("rule was created into ES ")
            } catch {
              case e : Exception =>
                logger.error("rule was not inserted into ES : " + e.getMessage)
            }
          })
        sender() ! result
      case UpdateRule(pointName, sequence, ruleEntityUpdate) =>
        logger.debug(s" receiving update($ruleEntityUpdate) on RuleWriteService")
        val result = ruleRepo.updateRule(pointName, sequence, ruleEntityUpdate)
        result.fold(
          errorMsg => {
            logger.info("Rule update failed with " + errorMsg)
          },
          ruleUpdated => {
            try {
              logger.info(s"rule $result created, now it will be inserted into ES")
              Await.result((esRuleWriteRepository ? UpdateRuleES(pointName, ruleUpdated.response.toES)).mapTo[RuleESUpdated], 5.seconds)
              logger.info("rule was updated into ES ")
            } catch {
              case e : Exception =>
                logger.error("rule was not updated into ES : " + e.getMessage)
            }
          })
        sender() ! result

      case FindRuleByPK(pointName, sequence) =>
        logger.debug(s" receiving FindRuleBySequence($sequence) on RuleWriteService")
        val result = ruleRepo.getRuleByKey(pointName, sequence)
        logger.debug(s"retrieving rule $result")
        sender() ! result

      case FindAllRules(pointName) =>
        logger.debug(s" receiving FindAllRules on RuleWriteService")
        val result = ruleRepo.getRules(pointName)
        logger.debug(s"retrieving rule $result")
        sender() ! result
    }
}