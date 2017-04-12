package com.groupaxis.groupsuite.routing.application.services

import java.util.UUID

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{Actor, ActorInitializationException, DeathPactException, OneForOneStrategy, Props, actorRef2Scala}
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.cluster.singleton.{ClusterSingletonProxy, ClusterSingletonProxySettings}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.groupaxis.groupsuite.commons.protocol.{Master, Work, WorkResult}
import com.groupaxis.groupsuite.routing.write.domain.model.routing.rule.RuleMessages.{CreateRule, FindAllRules, FindRuleByPK, UpdateRule}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.concurrent.duration.{DurationInt, FiniteDuration}

object RuleService {

  final val Name = "rule-service"

  def props(workTimeout: FiniteDuration): Props = Props(classOf[RuleService], workTimeout)

  case object Ok
  case object NotOk
}

class RuleService(workTimeout: FiniteDuration) extends Actor with Logging {
  import RuleService._

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
  implicit val timeout = Timeout(5.seconds)

  private var promises = Map[String, Promise[Any]]()

  def receive: Receive = {
    case FindRuleByPK(pointName, sequence) =>
      val findRule = FindRuleByPK(pointName, sequence)
      logger.debug(s"sending to RuleServiceMaster $findRule")
      (ruleMasterProxy ? Work(nextWorkId(), findRule)) map {
        case Master.Ack(workId) =>
          logger.debug(s"receiving Ack from RuleServiceMaster FindRuleById")
          Await.result(addWorkPromise(workId), 10.seconds)
      } recover {
        case _ =>
          logger.error(s"receiving KO from RuleServiceMaster FindRuleById")
          NotOk
      } pipeTo sender()

    case far : FindAllRules =>
      logger.debug(s"sending to RuleServiceMaster FindAllRules")
      (ruleMasterProxy ? Work(nextWorkId(), far)) map {
        case Master.Ack(workId) =>
          logger.debug(s"receiving Ack from RuleServiceMaster FindAllRules")
          Await.result(addWorkPromise(workId), 10.seconds)
      } recover {
        case _ =>
          logger.error(s"receiving KO from RuleServiceMaster FindAllRules")
          NotOk
      } pipeTo sender()

    case cr : CreateRule =>
      logger.debug(s"sending to RuleServiceMaster CreateRule")
      (ruleMasterProxy ? Work(nextWorkId(), cr)) map {
        case Master.Ack(workId) =>
          logger.debug(s"receiving Ack from Master (RuleService) => CreateRule")
          Await.result(addWorkPromise(workId), 10.seconds)
      } recover {
        case _ =>
          logger.error(s"receiving KO from RuleServiceMaster CreateRule")
          NotOk
      } pipeTo sender()

    case ur : UpdateRule =>
      logger.debug(s"sending to RuleServiceMaster UpdateRule")
      (ruleMasterProxy ? Work(nextWorkId(), ur)) map {
        case Master.Ack(workId) =>
          logger.debug(s"receiving Ack from Master (RuleService) => UpdateRule")
          val updateResult = Await.result(addWorkPromise(workId), 10.seconds)
          logger.info(s"piping to HttpRouting with $updateResult")
          updateResult
      } recover {
        case _ =>
          logger.error(s"receiving KO from RuleServiceMaster UpdateRule")
          NotOk
      } pipeTo sender()
      
    case _: DistributedPubSubMediator.SubscribeAck =>
    case WorkResult(workId, result) =>
      logger.info(s"---------------      Consumed result: $result")
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