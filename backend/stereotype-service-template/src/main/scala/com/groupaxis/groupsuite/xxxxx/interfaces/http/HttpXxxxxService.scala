package com.groupaxis.groupsuite.xxxxx.interfaces.http


import java.io.File

import akka.actor.SupervisorStrategy.{Restart, Resume, Stop}
import akka.actor.{Actor, ActorLogging, OneForOneStrategy, Props, Status}
import akka.http.scaladsl._
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{ContentDispositionTypes, `Content-Disposition`}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.settings.RoutingSettings
import akka.pattern.{ask, pipe}
import akka.stream.scaladsl.FileIO
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Materializer, Supervision}
import akka.util.Timeout
import ch.megard.akka.http.cors.CorsDirectives._
import ch.megard.akka.http.cors.CorsSettings
import com.groupaxis.groupsuite.persistence.datastore.jdbc.Database
import com.groupaxis.groupsuite.xxxxx.application.services.XxxxxWriteService
import com.groupaxis.groupsuite.simulator.infrastructor.jdbc.{JdbcJobWriteRepository, JdbcMappingWriteRepository, JdbcMessageWriteRepository}
import com.groupaxis.groupsuite.simulator.write.domain.model.job.JobMessages._
import com.groupaxis.groupsuite.simulator.write.domain.model.job.{Hit, JobDAO, JobEntityUpdate}
import com.groupaxis.groupsuite.simulator.write.domain.model.mapping.MappingDAO
import com.groupaxis.groupsuite.simulator.write.domain.model.swift.msg.SwiftMessages._
import com.groupaxis.groupsuite.simulator.write.domain.model.swift.msg.{SwiftMessageDAO, SwiftMessageEntityUpdate}
import com.groupaxis.groupsuite.tools.http.GPHttpHelper
import com.groupaxis.groupsuite.tools.http.GPHttpHelper._
import com.typesafe.config.Config

import scala.concurrent.ExecutionContext


object HttpXxxxxService {

  final val Name = "http-xxxxx-service"

  def props(config: Config, database: Database): Props = Props(new HttpXxxxxService(config, database))

}

class HttpXxxxxService(config: Config, database: Database) extends Actor with ActorLogging with HttpResource {
  implicit val ec: ExecutionContext = context.dispatcher

  import scala.concurrent.duration._

  implicit val timeout: Timeout = 50.seconds

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1.minute) {
      case _: ArithmeticException => Resume
      case _: NullPointerException => Restart
      case _: IllegalArgumentException => Stop
      case e: Exception =>
        log.warning("[HttpRoutingService] Exception has been received, so restarting the actor " + e.getMessage)
        e.printStackTrace()
        Restart
    }

  val decider: Supervision.Decider = (ex) => Supervision.Resume

  import io.circe.generic.auto._

  implicit val mat: Materializer = ActorMaterializer(ActorMaterializerSettings(context.system).withSupervisionStrategy(decider))

  val messageDao: SwiftMessageDAO = new SwiftMessageDAO(slick.driver.PostgresDriver)
  val mappingDao: MappingDAO = new MappingDAO(slick.driver.PostgresDriver)
  val jobDao: JobDAO = new JobDAO(slick.driver.PostgresDriver)
  val messageRepo: JdbcMessageWriteRepository = new JdbcMessageWriteRepository(messageDao, database, 10.seconds)
  val mappingRepo: JdbcMappingWriteRepository = new JdbcMappingWriteRepository(mappingDao, database, 10.seconds)
  val jobRepo: JdbcJobWriteRepository = new JdbcJobWriteRepository(jobDao, database, 10.seconds)
  val simulationService = context.actorOf(SimulatorWriteService.props(messageRepo, mappingRepo, jobRepo), SimulatorWriteService.Name)
  //val impExpRuleService = context.actorOf(FileRuleXmlWriteRepository.props(ruleRepo), FileRuleXmlWriteRepository.Name)

  val settings = CorsSettings.defaultSettings.copy(allowedMethods = scala.collection.immutable.Seq(GET, POST, PUT, DELETE, HEAD, OPTIONS))
  val special = RoutingSettings(context.system).withFileIODispatcher("special-io-dispatcher")

  def impExpRoutes: Route = logRequestResult("simulatorController") {
    pathPrefix("messages" / "amh" / "import") {
      cors(settings) {
        pathEnd {
          post {
            entity(as[Multipart.FormData]) { fileData =>
              log.info(s" messages/amh/import request with path $fileData")
              completeWithLocationHeader[String, String, Either[String, String]](
                resourceId = (simulationService ? fileData).map(r => r.asInstanceOf[Either[String, String]]),
                ifDefinedStatus = 201, ifEmptyStatus = 409)
            }
          }
        }
      }
    } ~
      pathPrefix("jobs" / "amh" / "export") {
        cors(settings) {
          pathEnd {
            post {
              entity(as[Seq[Hit]]) { hits =>
                log.info(s" messages/amh/export request with ${hits.length} hit(s)")
                extractRequestContext { requestContext =>
                  complete(
                    (simulationService ? CreateCSVFileWithHits(hits, loggedUser(requestContext)))
                      .map(r => r.asInstanceOf[Either[String, CSVFileCreated]])
                  )
                }
              }
            }
          } ~
          path(IntNumber) { jobId =>
            post {
              extractRequestContext { requestContext =>
                complete(
                  (simulationService ? CreateCSVFile(jobId, loggedUser(requestContext)))
                    .map(r => r.asInstanceOf[Either[String, CSVFileCreated]])
                )
              }
            }
          } ~
          get {
            withSettings(special) {
              path(Segment) { fileName: String =>
                log.info(" asking for file " + fileName)
                complete {
                  // internally uses the configured fileIODispatcher:
                  val exportDir = config.getString("simulator.export.dir")
                  val source = FileIO.fromFile(new File(s"$exportDir/$fileName"))
                  val headers = `Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" → s"job_$fileName")) :: Nil
                  HttpResponse(headers = headers, entity = HttpEntity(ContentTypes.`application/octet-stream`, source))
                }
              }
            }
          }
        }
      }
  }

  def routes: Route = logRequestResult("simulatorController") {

    path("") {
      redirect("messages/amh/", StatusCodes.PermanentRedirect)
    } ~
      pathPrefix("messages" / "amh") {
        cors(settings) {

          path(IntNumber) { messageId =>
            get {
              complete((simulationService ? FindSwiftMessageById(messageId)).map(r => r.asInstanceOf[Either[String, SwiftMessageFound]]))
            } ~
              put {
                entity(as[SwiftMessageEntityUpdate]) { message =>
                  complete((simulationService ? UpdateSwiftMessage(messageId, message)).map(r => r.asInstanceOf[Either[String, SwiftMessageUpdated]]))
                }

              }
          } ~
//          path(Segment) { messageIds =>
//
//          } ~
            pathEnd {
              get {
                log.info(s"here! messages/amh request")
                complete((simulationService ? FindAllSwiftMessages).mapTo[Either[String, SwiftMessagesFound]])
              } ~
              post {
                entity(as[SwiftMessageEntityUpdate]) { message =>
                  completeWithLocationHeader[String, SwiftMessageCreated, Either[String, SwiftMessageCreated]](
                    resourceId = (simulationService ? CreateSwiftMessage(message)).map(r => r.asInstanceOf[Either[String, SwiftMessageCreated]]),
                    ifDefinedStatus = 201, ifEmptyStatus = 409)
                }
              } ~
              delete {
                extractRequestContext { requestContext =>
                  val request: HttpRequest = requestContext.request
                  val userId = GPHttpHelper.headerValue(request, "userId")
                  val time = GPHttpHelper.headerValue(request, "time")
                  val groupId = GPHttpHelper.headerValue(request, "groupId")
                  val messageIds = GPHttpHelper.headerValue(request, "ids")
                  log.info(s"delete call with username $userId date: $time group $groupId")
                  if (messageIds.isEmpty)
                    complete(200)
                  else {
                    val ids = messageIds.get.split(';').map(_.toInt)
                    complete((simulationService ? DeleteSwiftMessagesByIds(ids, userId, groupId, time)).map(r => r.asInstanceOf[Either[String, SwiftMessagesDeleted]]))
                  }
                }
              }
            }
        }
      }
  }

  def jobRoutes: Route = logRequestResult("simulatorController") {
    import GPHttpHelper._
    path("") {
      redirect("jobs/amh/", StatusCodes.PermanentRedirect)
    } ~
      pathPrefix("jobs" / "amh" / "users") {
        cors(settings) {
          path(Segment) { username =>
            get {
              extractRequestContext { requestContext =>
                val request: HttpRequest = requestContext.request
                val jobStatus = GPHttpHelper.headerIntValue(request, "job_status")
                complete((simulationService ? FindAllJobsByUsername(username, jobStatus)).map(r => r.asInstanceOf[Either[String, JobsFound]]))
              }
            }
          }
        }
      } ~
      pathPrefix("jobs" / "amh") {
        cors(settings) {

          path(IntNumber) { jobId =>
            get {
              complete((simulationService ? FindJobById(jobId)).map(r => r.asInstanceOf[Either[String, JobFound]]))
            } ~
            delete {
              extractRequestContext { requestContext =>
                complete((simulationService ? CancelJob(jobId, loggedUser(requestContext))).map(r => r.asInstanceOf[Either[String, OperationDone]]))
              }
            } ~
            put {
              extractRequestContext { requestContext =>
                complete((simulationService ? ReExecuteJob(jobId, loggedUser(requestContext))).map(r => r.asInstanceOf[Either[String, OperationDone]]))
              }
            }
          } ~
          pathEnd {
            get {
              log.info(s"here! jobs/amh request")
              complete((simulationService ? FindAllJobs).mapTo[Either[String, JobsFound]])
            } ~
            post {
              entity(as[JobEntityUpdate]) { job =>
                completeWithLocationHeader[String, JobCreated, Either[String, JobCreated]](
                  resourceId = (simulationService ? CreateJob(job)).map(r => r.asInstanceOf[Either[String, JobCreated]]),
                  ifDefinedStatus = 201, ifEmptyStatus = 409)
              }
            }
          }
        }
      }
  }

  Http(context.system).bindAndHandle(routes ~ impExpRoutes ~ jobRoutes, config.getString("http.interface"), config.getInt("http.port"))
    .pipeTo(self)

  override def receive = binding

  private def binding: Receive = {
    case serverBinding@Http.ServerBinding(address) =>
      log.info("Listening on {}", address)
    case Status.Failure(cause) =>
      log.error(cause, s"Can't bind to address:port")
      context.stop(self)
  }

}
