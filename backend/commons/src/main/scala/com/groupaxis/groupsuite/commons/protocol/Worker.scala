package com.groupaxis.groupsuite.commons.protocol

package worker

import java.util.UUID

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{Actor, ActorInitializationException, ActorRef, DeathPactException, OneForOneStrategy, Props, ReceiveTimeout, Terminated}
import akka.cluster.client.ClusterClient.SendToAll
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.duration._
object Worker {

  def props(clusterClient: ActorRef, workExecutorProps: Props, registerInterval: FiniteDuration = 10.seconds): Props =
    Props(classOf[Worker], clusterClient, workExecutorProps, registerInterval)

  case class WorkComplete(result: Any)
}

class Worker(clusterClient: ActorRef, workExecutorProps: Props, registerInterval: FiniteDuration)
  extends Actor with Logging {
  import MasterWorkerProtocol._
  import Worker._

  val workerId = UUID.randomUUID().toString

  import context.dispatcher
  val registerTask = context.system.scheduler.schedule(0.seconds, registerInterval, clusterClient,
    SendToAll("/user/master/singleton", RegisterWorker(workerId)))

  val workExecutor = context.watch(context.actorOf(workExecutorProps, "exec"))

  var currentWorkId: Option[String] = None
  def workId: String = currentWorkId match {
    case Some(workId) => workId
    case None         => throw new IllegalStateException("Not working")
  }

  override def supervisorStrategy = OneForOneStrategy() {
    case _: ActorInitializationException => Stop
    case _: DeathPactException           => Stop
    case _: Exception =>
      currentWorkId foreach { workId => sendToMaster(WorkFailed(workerId, workId)) }
      context.become(idle)
      logger.warn("Worker supervisor restating executor ")
      Restart
  }

  override def postStop(): Unit = registerTask.cancel()

  def receive = idle

  def idle: Receive = {
    case WorkIsReady =>
      sendToMaster(WorkerRequestsWork(workerId))

    case Work(workId, job) =>
      currentWorkId = Some(workId)
      workExecutor ! job
      context.become(working)
  }

  def working: Receive = {
    case WorkComplete(result) =>
      sendToMaster(WorkIsDone(workerId, workId, result))
      context.setReceiveTimeout(5.seconds)
      context.become(waitForWorkIsDoneAck(result))

    case _: Work =>
      logger.info("Yikes. Master told me to do work, while I'm working.")
  }

  def waitForWorkIsDoneAck(result: Any): Receive = {
    case Ack(id) if id == workId =>
      sendToMaster(WorkerRequestsWork(workerId))
      context.setReceiveTimeout(Duration.Undefined)
      context.become(idle)
    case ReceiveTimeout =>
      logger.info("No ack from master, retrying")
      sendToMaster(WorkIsDone(workerId, workId, result))
  }

  override def unhandled(message: Any): Unit = message match {
    case Terminated(`workExecutor`) => context.stop(self)
    case WorkIsReady                =>
    case _                          => super.unhandled(message)
  }

  def sendToMaster(msg: Any): Unit = {
    clusterClient ! SendToAll("/user/master/singleton", msg)
  }

}