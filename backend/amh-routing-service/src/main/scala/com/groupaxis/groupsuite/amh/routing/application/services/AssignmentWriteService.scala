package com.groupaxis.groupsuite.amh.routing.application.services

import akka.actor.SupervisorStrategy.{Restart, Resume, Stop}
import akka.actor.{Actor, OneForOneStrategy, Props}
import akka.pattern.ask
import akka.stream.Supervision
import akka.util.Timeout
import com.groupaxis.groupsuite.amh.routing.infrastructor.es.ESAssignmentWriteRepository
import com.groupaxis.groupsuite.amh.routing.interfaces.http.client.HttpAuditRoutingClient
import com.groupaxis.groupsuite.routing.write.domain.global.messages.AMHRoutingGlobalMessages.CreateOverviewCSVFile
import com.sksamuel.elastic4s.{ElasticClient, ElasticsearchClientUri}
import com.typesafe.config.Config
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.ExecutionContext
import scala.util.Success

object AssignmentWriteService {

  final val Name = "assignment-write-service"

  def props(restClient : HttpAuditRoutingClient): Props = Props(classOf[AssignmentWriteService], restClient)

}

class AssignmentWriteService(val restClient : HttpAuditRoutingClient) extends Actor with Logging {
  import scala.concurrent.duration._
  implicit val timeout: Timeout = 50.seconds

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

  //TODO: Move this to another actor to make the denormalization asynchronous
  val config: Config = context.system.settings.config

  import org.elasticsearch.common.settings.Settings

  val settings = Settings.settingsBuilder().put("cluster.name", config.getString("elastic.cluster.name")).build()
  val client: ElasticClient = ElasticClient.transport(settings, ElasticsearchClientUri(s"elasticsearch://" + config.getString("elastic.url")))

  val esAssignmentWriteRepo = context.actorOf(ESAssignmentWriteRepository.props(client), ESAssignmentWriteRepository.Name)


  def receive: Receive = {
    case CreateOverviewCSVFile(assignmentType, username, date) =>
      logger.info(s"CreateOverviewCSVFile('$assignmentType', '$username') received")
      val origSender = sender()
      (esAssignmentWriteRepo ? CreateOverviewCSVFile(assignmentType, username, date))
        .onComplete {
          case Success(created) => origSender ! created
        }

  }
}

