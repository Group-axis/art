package com.groupaxis.groupsuite.amh.routing.interfaces.http.client

import java.io.IOException

import akka.actor.ActorSystem
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.groupaxis.groupsuite.amh.routing.interfaces.http.HttpResource
import com.groupaxis.groupsuite.routing.write.domain.audit.messages.AMHRoutingAuditMessages._
import com.groupaxis.groupsuite.synchronizator.http.RestClient

import scala.concurrent.{ExecutionContext, Future}

object HttpAuditRoutingClient {
  def apply(restClient: RestClient) = new HttpAuditRoutingClient(restClient)
}

class HttpAuditRoutingClient (val restClient: RestClient) extends HttpResource {

  import io.circe.generic.auto._
  import RequestBuilding._

  private def send(request: HttpRequest)
                   (implicit system: ActorSystem, materializer: ActorMaterializer, ec: ExecutionContext)
  : Future[Either[String, RoutingCreationDone]] = {
    restClient.exec(request)
      .flatMap { response =>
        response.status match {
          case StatusCodes.OK => Unmarshal(response.entity).to[RoutingCreationDone].map(Right(_))
          case StatusCodes.BadRequest => Future.successful(Left(s"bad request"))
          case _ => Unmarshal(response.entity).to[String].flatMap { entity =>
            val error = s"FAIL - ${response.status}"
            Future.failed(new IOException(error))
          }
        }
      }
  }

  def sendBackendCreation(createBackend : CreateBackendAssignment)
                         (implicit system: ActorSystem, materializer: ActorMaterializer, ec: ExecutionContext): Future[Either[String, RoutingCreationDone]] = {
    send(Post("/audit/routing/backend", createBackend))
  }

  def sendDistributionCreation(createDistribution : CreateDistributionAssignment)
                              (implicit system: ActorSystem, materializer: ActorMaterializer, ec: ExecutionContext): Future[Either[String, RoutingCreationDone]] = {
    send(Post("/audit/routing/distribution", createDistribution))
  }

  def sendFeedbackCreation(createFeedback : CreateFeedbackAssignment)
                          (implicit system: ActorSystem, materializer: ActorMaterializer, ec: ExecutionContext): Future[Either[String, RoutingCreationDone]] = {
    send(Post("/audit/routing/feedback", createFeedback))
  }

  def sendBackendUpdate(updateBackend : UpdateBackendAssignment)
                       (implicit system: ActorSystem, materializer: ActorMaterializer, ec: ExecutionContext): Future[Either[String, RoutingCreationDone]] = {
    send(Put("/audit/routing/backend", updateBackend))
  }

  def sendDistributionUpdate(updateDistribution : UpdateDistributionAssignment)
                            (implicit system: ActorSystem, materializer: ActorMaterializer, ec: ExecutionContext): Future[Either[String, RoutingCreationDone]] = {
    send(Put("/audit/routing/distribution", updateDistribution))
  }

  def sendFeedbackUpdate(updateFeedback : UpdateFeedbackAssignment)
                        (implicit system: ActorSystem, materializer: ActorMaterializer, ec: ExecutionContext): Future[Either[String, RoutingCreationDone]] = {
    send(Put("/audit/routing/feedback", updateFeedback))
  }

  def sendImportCreation(createImport : CreateImport)
                        (implicit system: ActorSystem, materializer: ActorMaterializer, ec: ExecutionContext): Future[Either[String, RoutingCreationDone]] = {
    send(Post("/audit/routing/import", createImport))
  }

  def sendImportBackupCreation(createImport : CreateImportBackup)
                              (implicit system: ActorSystem, materializer: ActorMaterializer, ec: ExecutionContext): Future[Either[String, RoutingCreationDone]] = {
    send(Post("/audit/routing/import/backup", createImport))
  }

  def sendExportCreation(createExport : CreateExport)(implicit system: ActorSystem, materializer: ActorMaterializer, ec: ExecutionContext): Future[Either[String, RoutingCreationDone]] = {
    send(Post("/audit/routing/export", createExport))
  }

  def sendRuleOverviewCreation(createRuleOverviewCSV : CreateRuleOverviewCSV)(implicit system: ActorSystem, materializer: ActorMaterializer, ec: ExecutionContext): Future[Either[String, RoutingCreationDone]] = {
    send(Post("/audit/routing/rule/overview", createRuleOverviewCSV))
  }

  def sendRoutingOverviewCreation(createAssignmentOverviewCSV : CreateAssignmentOverviewCSV)(implicit system: ActorSystem, materializer: ActorMaterializer, ec: ExecutionContext): Future[Either[String, RoutingCreationDone]] = {
    send(Post("/audit/routing/routing/overview", createAssignmentOverviewCSV))
  }

  def sendRuleCreation(createRule : CreateRule)(implicit system: ActorSystem, materializer: ActorMaterializer, ec: ExecutionContext): Future[Either[String, RoutingCreationDone]] = {
    send(Post("/audit/routing/rule", createRule))
  }

  def sendRuleUpdate(updateRule : UpdateRule)(implicit system: ActorSystem, materializer: ActorMaterializer, ec: ExecutionContext): Future[Either[String, RoutingCreationDone]] = {
    send(Put("/audit/routing/rule", updateRule))
  }

  def sendRuleDelete(deleteRule : DeleteRule)(implicit system: ActorSystem, materializer: ActorMaterializer, ec: ExecutionContext): Future[Either[String, RoutingCreationDone]] = {
    send(Delete("/audit/routing/rule", deleteRule))
  }

}
