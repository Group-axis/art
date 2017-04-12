package com.groupaxis.groupsuite.amh.routing.application.services

import java.util.UUID

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{Actor, ActorInitializationException, DeathPactException, OneForOneStrategy, Props, actorRef2Scala}
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}
import akka.cluster.singleton.{ClusterSingletonProxy, ClusterSingletonProxySettings}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import com.groupaxis.groupsuite.commons.protocol.{Master, Work, WorkResult}
import com.groupaxis.groupsuite.routing.write.domain.model.amh.assignment.AMHAssignmentMessages.{CreateAMHAssignment, FindAMHAssignmentByCode, FindAllAMHAssignments, UpdateAMHAssignment}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.concurrent.duration.FiniteDuration

object AssignmentService {

  final val Name = "assignment-service"

  def props(workTimeout: FiniteDuration): Props = Props(classOf[AssignmentService], workTimeout)

  case object Ok
  case object NotOk
}

class AssignmentService(workTimeout: FiniteDuration) extends Actor with Logging {
//  import AssignmentService._

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
  val assignmentMasterProxy = context.actorOf(
    ClusterSingletonProxy.props(
      settings = ClusterSingletonProxySettings(context.system).withRole("backend"),
      singletonManagerPath = "/user/master"),
    name = "assignmentMasterProxy")

  //Consumer
  val mediator = DistributedPubSub(context.system).mediator
  mediator ! DistributedPubSubMediator.Subscribe(Master.ResultsTopic, self)

  implicit val ec: ExecutionContext = context.dispatcher
  implicit val timeout = Timeout(workTimeout)

  private var promises = Map[String, Promise[Any]]()

  def receive: Receive = {
    case FindAMHAssignmentByCode(code) =>
      val findAssignment = FindAMHAssignmentByCode(code)
      logger.debug(s"sending to AssignmentServiceMaster $findAssignment")
      (assignmentMasterProxy ? Work(nextWorkId(), findAssignment)) map {
        case Master.Ack(workId) =>
          logger.debug(s"receiving Ack from AssignmentServiceMaster FindAmhAssignmentByCode")
          Await.result(addWorkPromise(workId), workTimeout)
      } recover {
        case _ =>
          logger.error(s"receiving KO from AssignmentServiceMaster FindAmhAssignmentByCode")
          Left("An error has occurred while finding an assignment" )
      } pipeTo sender()

    case faaa : FindAllAMHAssignments =>
      logger.debug(s"sending to AssignmentServiceMaster FindAllAmhAssignments")
      (assignmentMasterProxy ? Work(nextWorkId(), faaa)) map {
        case Master.Ack(workId) =>
          logger.debug(s"receiving Ack from AssignmentServiceMaster FindAllAmhAssignments")
          Await.result(addWorkPromise(workId), workTimeout)
      } recover {
        case _ =>
          logger.error(s"recover from AssignmentService FindAllAMHAssignments")
          Left("An error has occurred while finding all  Assignments" )
      } pipeTo sender()

    case caa : CreateAMHAssignment =>
      logger.debug(s"sending to AssignmentServiceMaster CreateAMHAssignment")
      (assignmentMasterProxy ? Work(nextWorkId(), caa)) map {
        case Master.Ack(workId) =>
          logger.debug(s"receiving Ack from Master (AMHAssignmentService) => CreateAMHAssignment")
          Await.result(addWorkPromise(workId), workTimeout)
      } recover {
        case _ =>
          logger.error(s"recover from AssignmentService CreateAMHAssignment")
          Left("An error has occurred while creating Assignment" )
      } pipeTo sender()

    case uaa : UpdateAMHAssignment =>
      logger.debug(s"sending to AMHAssignmentServiceMaster UpdateAMHAssignment")
      (assignmentMasterProxy ? Work(nextWorkId(), uaa)) map {
        case Master.Ack(workId) =>
          logger.debug(s"receiving Ack from Master (AMHAssignmentService) => UpdateAMHAssignment")
          val updateResult = Await.result(addWorkPromise(workId), workTimeout)
          logger.info(s"piping to HttpRouting with $updateResult")
          updateResult
      } recover {
        case _ =>
          logger.error(s"recover from AssignmentService UpdateAMHAssignment")
          Left("An error has occurred while updating Assignment" )
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