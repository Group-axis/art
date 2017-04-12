package com.groupaxis.groupsuite.audit.interfaces.http

import akka.actor.SupervisorStrategy.{Restart, Resume, Stop}
import akka.actor.{Actor, OneForOneStrategy, Props, Status}
import akka.http.scaladsl._
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.settings.RoutingSettings
import akka.pattern.{ask, pipe}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Materializer, Supervision}
import akka.util.Timeout
import ch.megard.akka.http.cors.CorsDirectives._
import ch.megard.akka.http.cors.CorsSettings
import com.groupaxis.groupsuite.audit.application.services.{AuditAssignmentWriteService, AuditSimulationWriteService, AuditUserWriteService}
import com.groupaxis.groupsuite.audit.infrastructor.jdbc.JdbcDifferenceWriteRepository
import com.groupaxis.groupsuite.audit.write.domain.model.difference.DifferenceDAO
import com.groupaxis.groupsuite.authentication.write.domain.audit.messages.AuthenticationAuditMessages._
import com.groupaxis.groupsuite.persistence.datastore.jdbc.Database
import com.groupaxis.groupsuite.routing.write.domain.audit.messages.AMHRoutingAuditMessages.{CreateBackendAssignment, _}
import com.groupaxis.groupsuite.simulator.write.domain.audit.messages.SimulationAuditMessages.CreateInstanceSimulation
import com.typesafe.config.Config
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.ExecutionContext


object HttpAuditService {

  final val Name = "http-audit-service"

  def props(config: Config, database: Database): Props = Props(new HttpAuditService(config, database))

}

class HttpAuditService(config: Config, database: Database) extends Actor with Logging with HttpResource {
  implicit val ec: ExecutionContext = context.dispatcher

  import scala.concurrent.duration._

  implicit val timeout: Timeout = 50.seconds

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1.minute) {
      case _: ArithmeticException => Resume
      case _: NullPointerException => Restart
      case _: IllegalArgumentException => Stop
      case e: Exception =>
        logger.warn(s"[HttpRoutingService] Exception has been received, so restarting the actor ${e.getMessage}" )
        e.printStackTrace()
        Restart
    }

  val decider: Supervision.Decider = (ex) => Supervision.Resume

  import io.circe.generic.auto._

  implicit val mat: Materializer = ActorMaterializer(ActorMaterializerSettings(context.system).withSupervisionStrategy(decider))

  val diffDao: DifferenceDAO = new DifferenceDAO(slick.driver.PostgresDriver)
  val diffRepo: JdbcDifferenceWriteRepository = new JdbcDifferenceWriteRepository(diffDao, database)
  val auditUserService = context.actorOf(AuditUserWriteService.props(diffRepo), AuditUserWriteService.Name)
  val auditSimulationService = context.actorOf(AuditSimulationWriteService.props(diffRepo), AuditSimulationWriteService.Name)
  val auditRoutingService = context.actorOf(AuditAssignmentWriteService.props(diffRepo), AuditAssignmentWriteService.Name)

  val settings = CorsSettings.defaultSettings.copy(allowedMethods = scala.collection.immutable.Seq(GET, POST, PUT, DELETE, HEAD, OPTIONS))
  val special = RoutingSettings(context.system).withFileIODispatcher("special-io-dispatcher")

  def authenticationRoutes: Route = logRequestResult("auditAuthenticationController") {
    pathPrefix("audit" / "authentication" / "user") {
      cors(settings) {
        pathEnd {
          post {
            entity(as[CreateUser]) { createUser =>
              complete(
                (auditUserService ? createUser).map(r => r.asInstanceOf[AuthenticationCreationDone])
              )
            }
          } ~
            put {
              entity(as[UpdateUser]) { updateUser =>
                complete {
                  (auditUserService ? updateUser).mapTo[AuthenticationUpdateDone]//(r => r.asInstanceOf[AuthenticationUpdateDone])
                }
              }
            } ~
            delete {
              entity(as[DeleteUser]) { deleteUser =>
                complete {
                  (auditUserService ? deleteUser).map(r => r.asInstanceOf[AuthenticationDeletionDone])
                }
              }
            }
        }
      }
    } ~
      pathPrefix("audit" / "authentication" / "user" / "block") {
        cors(settings) {
          pathEnd {
            post {
              entity(as[CreateBlockUser]) { createBlockUser =>
                complete(
                  (auditUserService ? createBlockUser).map(r => r.asInstanceOf[AuthenticationCreationDone])
                )
              }
            } ~
              delete {
                entity(as[CreateUnblockUser]) { createUnblockUser =>
                  complete(
                    (auditUserService ? createUnblockUser).map(r => r.asInstanceOf[AuthenticationCreationDone])
                  )
                }
              }
          }
        }
      } ~
      pathPrefix("audit" / "authentication" / "user" / "login") {
        cors(settings) {
          pathEnd {
            post {
              entity(as[CreateLoginUser]) { createLoginUser =>
                complete(
                  (auditUserService ? createLoginUser).map(r => r.asInstanceOf[AuthenticationCreationDone])
                )
              }
            } ~
              delete {
                entity(as[CreateLogoutUser]) { createLogoutUser =>
                  complete(
                    (auditUserService ? createLogoutUser).map(r => r.asInstanceOf[AuthenticationCreationDone])
                  )
                }
              }
          }
        }
      } ~
      pathPrefix("audit" / "authentication" / "user" / "password") {
        cors(settings) {
          pathEnd {
            put {
              entity(as[CreateResetPassword]) { createResetPassword =>
                complete(
                  (auditUserService ? createResetPassword).map(r => r.asInstanceOf[AuthenticationCreationDone])
                )
              }
            }
          }
        }
      } ~
      pathPrefix("audit" / "authentication" / "user" / "connection") {
        cors(settings) {
          pathEnd {
            post {
              entity(as[CreateFailedConnUser]) { createFailedConnUser =>
                complete(
                  (auditUserService ? createFailedConnUser).map(r => r.asInstanceOf[AuthenticationCreationDone])
                )
              }
            }
          }
        }
      }
  }

  def simulationRoutes: Route = logRequestResult("auditSimulatorController") {

    pathPrefix("audit" / "simulation" / "instance") {
      cors(settings) {

        pathEnd {
          post {
            entity(as[CreateInstanceSimulation]) { createInstanceSimulation =>
              complete(
                (auditSimulationService ? createInstanceSimulation).map(r => r.asInstanceOf[AuthenticationCreationDone])
              )
            }
          }
        }
      }
    }
  }

  /*
  def sendImportCreation(createImport : CreateImport): Future[Either[String, RoutingCreationDone]] = {
    send(Post("/audit/routing/import", createImport))
  def sendImportBackupCreation(createImport : CreateImportBackup): Future[Either[String, RoutingCreationDone]] = {
    send(Post("/audit/routing/import/backup", createImport))
  def sendExportCreation(createExport : CreateExport): Future[Either[String, RoutingCreationDone]] = {
    send(Post("/audit/routing/export", createExport))
  def sendRuleOverviewCreation(createRuleOverviewCSV : CreateRuleOverviewCSV): Future[Either[String, RoutingCreationDone]] = {
    send(Post("/audit/routing/rule/overview", createRuleOverviewCSV))
  def sendRoutingOverviewCreation(createAssignmentOverviewCSV : CreateAssignmentOverviewCSV): Future[Either[String, RoutingCreationDone]] = {
    send(Post("/audit/routing/routing/overview", createAssignmentOverviewCSV))
  def sendRuleCreation(createRule : CreateRule): Future[Either[String, RoutingCreationDone]] = {
    send(Post("/audit/routing/rule", createRule))
  def sendRuleUpdate(updateRule : UpdateRule): Future[Either[String, RoutingCreationDone]] = {
    send(Put("/audit/routing/rule", updateRule))
  def sendRuleDelete(deleteRule : DeleteRule): Future[Either[String, RoutingCreationDone]] = {
    send(Delete("/audit/routing/rule", deleteRule))
  * */


  def amhRoutingRoutes: Route = logRequestResult("auditAMHRoutingController") {

    pathPrefix("audit" / "routing" / "backend") {
      cors(settings) {
        pathEnd {
          post {
            entity(as[CreateBackendAssignment]) { createBackendAssignment =>
              complete(
                (auditRoutingService ? createBackendAssignment).map(r => r.asInstanceOf[RoutingCreationDone])
              )
            }
          } ~
            put {
              entity(as[UpdateBackendAssignment]) { updateBackendAssignment =>
                complete(
                  (auditRoutingService ? updateBackendAssignment).map(r => r.asInstanceOf[RoutingCreationDone])
                )
              }
            }
        }
      }
    } ~
      pathPrefix("audit" / "routing" / "distribution") {
        cors(settings) {
          pathEnd {
            post {
              entity(as[CreateDistributionAssignment]) { createDistributionAssignment =>
                complete(
                  (auditRoutingService ? createDistributionAssignment).map(r => r.asInstanceOf[RoutingCreationDone])
                )
              }
            } ~
              put {
                entity(as[UpdateDistributionAssignment]) { updateDistributionAssignment =>
                  complete(
                    (auditRoutingService ? updateDistributionAssignment).map(r => r.asInstanceOf[RoutingCreationDone])
                  )
                }
              }
          }
        }
      } ~
      pathPrefix("audit" / "routing" / "feedback") {
        cors(settings) {
          pathEnd {
            post {
              entity(as[CreateFeedbackAssignment]) { createFeedbackAssignment =>
                complete(
                  (auditRoutingService ? createFeedbackAssignment).map(r => r.asInstanceOf[RoutingCreationDone])
                )
              }
            } ~
              put {
                entity(as[UpdateFeedbackAssignment]) { updateFeedbackAssignment =>
                  complete(
                    (auditRoutingService ? updateFeedbackAssignment).map(r => r.asInstanceOf[RoutingCreationDone])
                  )
                }
              }
          }
        }
      } ~
      pathPrefix("audit" / "routing" / "import") {
        cors(settings) {
          pathEnd {
            post {
              entity(as[CreateImport]) { createImport =>
                complete(
                  (auditRoutingService ? createImport).map(r => r.asInstanceOf[RoutingCreationDone])
                )
              }
            }
          }
        }
      } ~
      pathPrefix("audit" / "routing" / "import" / "backup") {
        cors(settings) {
          pathEnd {
            post {
              entity(as[CreateImportBackup]) { createImportBackup =>
                complete(
                  (auditRoutingService ? createImportBackup).map(r => r.asInstanceOf[RoutingCreationDone])
                )
              }
            }
          }
        }
      }~
      pathPrefix("audit" / "routing" / "rule") {
        cors(settings) {
          pathEnd {
            post {
              entity(as[CreateRule]) { createRule =>
                complete(
                  (auditRoutingService ? createRule).map(r => r.asInstanceOf[RoutingCreationDone])
                )
              }
            } ~
            put {
              entity(as[UpdateRule]) { updateRule =>
                complete(
                  (auditRoutingService ? updateRule).map(r => r.asInstanceOf[RoutingCreationDone])
                )
              }
            } ~
            delete {
              entity(as[DeleteRule]) { deleteRule =>
                complete(
                  (auditRoutingService ? deleteRule).map(r => r.asInstanceOf[RoutingCreationDone])
                )
              }
            }
          }
        }
      }
  }

  Http(context.system).bindAndHandle(amhRoutingRoutes ~ authenticationRoutes ~ simulationRoutes, config.getString("http.interface"), config.getInt("http.port"))
    .pipeTo(self)

  override def receive = binding

  private def binding: Receive = {
    case serverBinding@Http.ServerBinding(address) =>
      logger.info("Listening on $address")
    case Status.Failure(cause) =>
      logger.error(s"Can't bind to address:port ${cause.getMessage}")
      context.stop(self)
  }

}
