package com.groupaxis.groupsuite.amh.routing.application.services

import java.util.UUID

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{Actor, ActorInitializationException, DeathPactException, OneForOneStrategy, Props, actorRef2Scala}
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.cluster.singleton.{ClusterSingletonProxy, ClusterSingletonProxySettings}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.groupaxis.groupsuite.commons.protocol.{Master, Work, WorkResult}
import com.groupaxis.groupsuite.routing.write.domain.model.amh.rule.AMHRuleMessages.{CreateAMHRule, FindAMHRuleByCode, FindAllAMHRules, UpdateAMHRule}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, ExecutionContext, Future, Promise}

object RuleService {

  final val Name = "rule-service"

  def props(workTimeout: FiniteDuration): Props = Props(classOf[RuleService], workTimeout)

  case object Ok
  case object NotOk
}

class RuleService(workTimeout: FiniteDuration) extends Actor with Logging {

  def nextWorkId(): String = UUID.randomUUID().toString

  override val supervisorStrategy = OneForOneStrategy() {
    case _: ActorInitializationException => Stop
    case _: DeathPactException           => Stop
    case _: Exception =>
      logger.error("Restarting ruleMasterProxy actor")
      //      currentWorkId foreach { workId => sendToMaster(WorkFailed(workerId, workId)) }
      //      context.become(idle)
      Restart
  }

  //Producer
  val ruleMasterProxy = context.actorOf(
    ClusterSingletonProxy.props(
      settings = ClusterSingletonProxySettings(context.system).withRole("backend"),
      singletonManagerPath = "/user/master"),
    name = "ruleMasterProxy")

  //Consumer
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! DistributedPubSubMediator.Subscribe(Master.ResultsTopic, self)

  implicit val ec: ExecutionContext = context.dispatcher
  implicit val timeout = Timeout(workTimeout)

  private var promises = Map[String, Promise[Any]]()

  def receive: Receive = {
    case FindAMHRuleByCode(code) =>
      val findRule = FindAMHRuleByCode(code)
      logger.debug(s"sending to RuleServiceMaster $findRule")
      (ruleMasterProxy ? Work(nextWorkId(), findRule)) map {
        case Master.Ack(workId) =>
          logger.debug(s"receiving Ack from RuleServiceMaster FindAmhRuleByCode")
          Await.result(addWorkPromise(workId), workTimeout)
      } recover {
        case _ =>
          logger.error(s"receiving KO from RuleServiceMaster FindAmhRuleByCode")
          new Left("NotOk")
      } pipeTo sender()

    case faar : FindAllAMHRules =>
      logger.debug(s"sending to RuleServiceMaster FindAllAmhRules")
      (ruleMasterProxy ? Work(nextWorkId(), faar)) map {
        case Master.Ack(workId) =>
          logger.debug(s"receiving Ack from RuleServiceMaster FindAllAmhRules")
          Await.result(addWorkPromise(workId), workTimeout)
      } recover {
        case _ =>
          logger.error(s"receiving KO from AMHRuleServiceMaster FindAllAMHRules")
          Left("An error has occurred while finding all Rules" )
      } pipeTo sender()

    case car : CreateAMHRule =>
      logger.debug(s"sending to RuleServiceMaster CreateAMHRule")
      (ruleMasterProxy ? Work(nextWorkId(), car)) map {
        case Master.Ack(workId) =>
          logger.debug(s"receiving Ack from Master (AMHRuleService) => CreateAMHRule")
          Await.result(addWorkPromise(workId), workTimeout)
      } recover {
        case e =>
          logger.error(s"receiving KO from RuleServiceMaster CreateAMHRule")
          Left("An error has occurred while creating a rule, msg: " + e.getMessage)
      } pipeTo sender()

    case ur : UpdateAMHRule =>
      logger.debug(s"sending to AMHRuleServiceMaster UpdateAMHRule")
      (ruleMasterProxy ? Work(nextWorkId(), ur)) map {
        case Master.Ack(workId) =>
          logger.debug(s"receiving Ack from Master (AMHRuleService) => UpdateAMHRule")
          val updateResult = Await.result(addWorkPromise(workId), workTimeout)
          logger.info(s"piping to HttpRouting with $updateResult")
          updateResult
      } recover {
        case e =>
          logger.error(s"receiving KO from AMHRuleServiceMaster UpdateAMHRule")
          Left("An error has occurred while updating a rule, msg: " + e.getMessage)
      } pipeTo sender()
      
    case _: DistributedPubSubMediator.SubscribeAck =>
    case WorkResult(workId, result) =>
      logger.info("---------------      Consumed result: $result")
      promises.get(workId).map { pRest => pRest success result }
      promises -= workId
  }

  def addWorkPromise(workId: String): Future[Any] = {
    val f = Promise[Any]()
    promises += (workId -> f)
    logger.info("wainting........")
    f.future
  }
}