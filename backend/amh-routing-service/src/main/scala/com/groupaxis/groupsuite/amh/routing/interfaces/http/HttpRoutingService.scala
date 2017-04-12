package com.groupaxis.groupsuite.amh.routing.interfaces.http

import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar

import akka.actor.{Actor, ActorRef, Props, Status}
import akka.http.scaladsl._
import akka.http.scaladsl.model.headers.{ContentDispositionTypes, `Content-Disposition`}
import akka.http.scaladsl.model.{HttpEntity, StatusCodes, _}
import akka.http.scaladsl.server.{Directive1, Rejection, Route}
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.pattern.{AskTimeoutException, pipe}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import com.groupaxis.groupsuite.amh.routing.application.services.{AssignmentWriteService, RuleWriteService}
import com.groupaxis.groupsuite.amh.routing.infrastructor.file.AssignmentWriteRepository
import com.groupaxis.groupsuite.amh.routing.infrastructor.jdbc.{JdbcAMHDistributionCopyWriteRepository, JdbcAMHFeedbackDistributionCopyWriteRepository}
import com.groupaxis.groupsuite.amh.routing.interfaces.http.client.HttpAuditRoutingClient
import com.groupaxis.groupsuite.routing.write.domain.global.messages.AMHRoutingGlobalMessages.{CreateOverviewCSVFile, OverviewCSVFileCreated}
import com.groupaxis.groupsuite.routing.write.domain.model.amh.feedback.distribution.copy.AMHFeedbackDistributionCpyEntityUpdate
import com.groupaxis.groupsuite.routing.write.domain.model.amh.feedback.distribution.copy.AMHFeedbackDistributionCpyMessages._
import com.groupaxis.groupsuite.routing.write.domain.model.amh.rule.AMHRuleMessages._
import com.groupaxis.groupsuite.routing.write.domain.model.amh.rule.{AMHRuleDAO, AMHRuleEntityUpdate}
import com.groupaxis.groupsuite.synchronizator.http.GPHttpHelper._
import com.groupaxis.groupsuite.synchronizator.http.RestClient
import com.groupaxis.groupsuite.xml.parser.amh.model.Messages.{ExportAMHBackupXmlFile, ExportAMHXmlFile, ImportAMHXmlFile, XmlFileExported}
import com.typesafe.config.Config
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{Await, ExecutionContext}

//import de.heikoseeberger.akkahttpcirce.CirceSupport
//import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.settings.RoutingSettings
import akka.pattern.ask
import akka.stream.scaladsl.FileIO
import akka.util.Timeout
import ch.megard.akka.http.cors.CorsDirectives._
import ch.megard.akka.http.cors.CorsSettings
import com.groupaxis.groupsuite.amh.routing.infrastructor.file.{FileAMHXmlWriteRepository, FileDistributionCpyWriteRepository, FileFeedbackDtnCpyWriteRepository}
import com.groupaxis.groupsuite.amh.routing.infrastructor.jdbc.{JdbcAMHAssignmentWriteRepository, JdbcAMHRuleWriteRepository}
import com.groupaxis.groupsuite.commons.persistence.jdbc.DatabaseService
import com.groupaxis.groupsuite.routing.write.domain.model.amh.assignment.AMHAssignmentMessages._
import com.groupaxis.groupsuite.routing.write.domain.model.amh.assignment.{AMHAssignmentEntityUpdate, AssignmentDAO, AssignmentRuleDAO}
import com.groupaxis.groupsuite.routing.write.domain.model.amh.backend.BackendDAO
import com.groupaxis.groupsuite.routing.write.domain.model.amh.distribution.copy.AMHDistributionCpyMessages._
import com.groupaxis.groupsuite.routing.write.domain.model.amh.distribution.copy.{AMHDistributionCpyEntityUpdate, DistributionCpyBackendDAO, DistributionCpyDAO, DistributionCpyRuleDAO}
import com.groupaxis.groupsuite.routing.write.domain.model.amh.feedback.distribution.copy.{FeedbackDtnCpyBackDAO, FeedbackDtnCpyDAO, FeedbackDtnCpyRuleDAO}
import com.groupaxis.groupsuite.xml.parser.amh.model.Messages.XmlFileImported
import com.groupaxis.groupsuite.xml.parser.amh.writer.jdbc.{JdbcAMHDistributionCpyBackendRepository, _}

object HttpRoutingService {

  final val Name = "http-amh-routing-service"

  def props(config: Config, dbService: DatabaseService): Props = Props(new HttpRoutingService(config, dbService))

  //  private def routes(ruleService: ActorRef, internalTimeout: Timeout, system: ActorSystem)(implicit ec: ExecutionContext, mat: Materializer) = {
  //    //    new RuleController(ruleService, internalTimeout, system).routes
  //  }
}

/*
* INFO: Maven freeze while compiling is because DateTime or Date or another serializer class is not defined in JsonSupport class
* */
class HttpRoutingService(config: Config, dbService: DatabaseService) extends Actor with Logging with HttpResource {
  implicit val ec: ExecutionContext = context.dispatcher
  implicit val system = context.system

  import akka.actor.OneForOneStrategy
  import akka.actor.SupervisorStrategy._

  import scala.concurrent.duration._

  implicit val timeout: Timeout = 50.seconds

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1.minute) {
      case _: ArithmeticException => Resume
      case nullPointer: NullPointerException =>
        logger.warn("[HttpRoutingService] NullPointerException has been received, so restarting the actor: " + nullPointer.getMessage)
        nullPointer.printStackTrace()
        Restart
      case illegal: IllegalArgumentException =>
        logger.warn("[HttpRoutingService] IllegalArgumentException has been received, so restarting the actor: " + illegal.getMessage)
        illegal.printStackTrace()
        Restart
      case e: Exception =>
        logger.warn("[HttpRoutingService] Exception has been received, so restarting the actor: " + e.getMessage)
        e.printStackTrace()
        Restart
    }

  val decider: Supervision.Decider = (ex) => Supervision.Resume

  implicit val mat: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system).withSupervisionStrategy(decider))

  val restClient = HttpAuditRoutingClient(RestClient(config.getString("audit.host"), config.getInt("audit.port"), ConnectionPoolSettings(context.system)))

  import io.circe.generic.auto._

  val DBDriver = slick.driver.PostgresDriver

  val ruleDao: AMHRuleDAO = AMHRuleDAO
  val backendDao: BackendDAO = BackendDAO(DBDriver)
  val assignmentDao: AssignmentDAO = AssignmentDAO(DBDriver)
  val assignmentRuleDao: AssignmentRuleDAO = AssignmentRuleDAO(DBDriver)
  val distributionDao: DistributionCpyDAO = DistributionCpyDAO(DBDriver)
  val distributionRuleDao: DistributionCpyRuleDAO = DistributionCpyRuleDAO(DBDriver)
  val distributionBackendDao: DistributionCpyBackendDAO = DistributionCpyBackendDAO(DBDriver)
  val feedbackDao: FeedbackDtnCpyDAO = FeedbackDtnCpyDAO(DBDriver)
  val feedbackRuleDao: FeedbackDtnCpyRuleDAO = FeedbackDtnCpyRuleDAO(DBDriver)
  val feedbackBackendDao: FeedbackDtnCpyBackDAO = FeedbackDtnCpyBackDAO(DBDriver)

  val ruleRepo = new com.groupaxis.groupsuite.xml.parser.amh.writer.jdbc.JdbcAMHRuleRepository(ruleDao, dbService)
  val backendRepo: JdbcAMHBackendRepository = new JdbcAMHBackendRepository(backendDao, dbService)
  val assignmentRepo: JdbcAMHAssignmentRepository = new JdbcAMHAssignmentRepository(assignmentDao, dbService)
  val assignmentRuleRepo: JdbcAMHAssignmentRuleRepository = new JdbcAMHAssignmentRuleRepository(assignmentRuleDao, dbService)
  val distributionRepo: JdbcAMHDistributionCpyRepository = new JdbcAMHDistributionCpyRepository(distributionDao, dbService)
  val distributionRuleRepo: JdbcAMHDistributionCpyRuleRepository = new JdbcAMHDistributionCpyRuleRepository(distributionRuleDao, dbService)
  val distributionBackendRepo: JdbcAMHDistributionCpyBackendRepository = new JdbcAMHDistributionCpyBackendRepository(distributionBackendDao, dbService)
  val feedbackRepo: JdbcAMHFeedbackDtnCpyRepository = new JdbcAMHFeedbackDtnCpyRepository(feedbackDao, dbService)
  val feedbackRuleRepo: JdbcAMHFeedbackDtnCpyRuleRepository = new JdbcAMHFeedbackDtnCpyRuleRepository(feedbackRuleDao, dbService)
  val feedbackBackendRepo: JdbcAMHFeedbackDtnCpyBackendRepository = new JdbcAMHFeedbackDtnCpyBackendRepository(feedbackBackendDao, dbService)

  val assignmentWriteRepo = new AssignmentWriteRepository(assignmentRepo, assignmentRuleRepo)
  val distributionCpyWriterRepo: FileDistributionCpyWriteRepository = new FileDistributionCpyWriteRepository(distributionRepo, distributionBackendRepo, distributionRuleRepo)
  val feedbackCpyWriterRepo: FileFeedbackDtnCpyWriteRepository = new FileFeedbackDtnCpyWriteRepository(feedbackRepo, feedbackBackendRepo, feedbackRuleRepo)

  val impExpService = context.actorOf(FileAMHXmlWriteRepository.props(ruleRepo, backendRepo, assignmentRepo, assignmentRuleRepo, distributionCpyWriterRepo, feedbackCpyWriterRepo, dbService, restClient), FileAMHXmlWriteRepository.Name)
  val ruleService = context.actorOf(JdbcAMHRuleWriteRepository.props(ruleDao, dbService, restClient), JdbcAMHRuleWriteRepository.Name)
  val assignmentService = context.actorOf(JdbcAMHAssignmentWriteRepository.props(assignmentDao, assignmentRuleDao, dbService, restClient), JdbcAMHAssignmentWriteRepository.Name)
  val distributionService = context.actorOf(JdbcAMHDistributionCopyWriteRepository.props(distributionRepo, distributionRuleRepo, distributionBackendRepo, dbService, restClient), JdbcAMHDistributionCopyWriteRepository.Name)
  val feedbackService = context.actorOf(JdbcAMHFeedbackDistributionCopyWriteRepository.props(feedbackRepo, feedbackRuleRepo, feedbackBackendRepo, dbService, restClient), JdbcAMHFeedbackDistributionCopyWriteRepository.Name)
  val ruleWriteService = context.actorOf(RuleWriteService.props(ruleRepo, assignmentWriteRepo, distributionCpyWriterRepo, feedbackCpyWriterRepo, restClient), RuleWriteService.Name)
  val assignmentWriteService = context.actorOf(AssignmentWriteService.props(restClient), AssignmentWriteService.Name)
  //  val mediator = DistributedPubSub(context.system).mediator
  //  mediator ! DistributedPubSubMediator.Subscribe(Master.ResultsTopic, self)

  val ruleController = context.system.actorOf(RuleController.props(ruleService, 10.seconds), RuleController.Name)

  import akka.http.scaladsl.model.HttpMethods._

  val settings = CorsSettings.defaultSettings.copy(allowedMethods = scala.collection.immutable.Seq(GET, POST, PUT, DELETE, HEAD, OPTIONS))

  val special = RoutingSettings(context.system).withFileIODispatcher("special-io-dispatcher")

  def currentDate: String = {
    val today = Calendar.getInstance().getTime
    val minuteFormat = new SimpleDateFormat("yyyyMMddHHmmss")
    minuteFormat.format(today)
  }

  case class BackupErrorRejection(message: String) extends Rejection

  def withBackupDirective(fileData: Multipart.FormData): Directive1[(String, String)] = {
    val username = "backup"
    val fileName = username + "_" + currentDate + ".zip"
    val importWithBackup = Await.result((impExpService ? ExportAMHBackupXmlFile(fileData, fileName))
      .mapTo[Either[String, XmlFileExported]], 25.minutes)
    importWithBackup match {
      case Left(exception) => reject(BackupErrorRejection(exception))
      case Right(XmlFileExported(backupFileName, _)) => provide((backupFileName, username))
    }
  }

  def impExpRoutes: Route = logRequestResult("amhImpExpController") {

    pathPrefix("amhrouting" / "import") {
      cors(settings) {
        pathEnd {
          withoutRequestTimeout {
            post {
              entity(as[Multipart.FormData]) { fileData =>
                logger.info(s" amhrouting import request with path $fileData")

                withBackupDirective(fileData) { values =>
                  val (fileName, username) = values
                  implicit val timeout: Timeout = 60.seconds
                  val importResponse = (impExpService ? fileData).mapTo[Either[String, XmlFileImported]]

                  importResponse.onFailure({
                    case timeout: AskTimeoutException =>
                      logger.info(s" Restoring backup for failing: $timeout")
                      impExpService ! ImportAMHXmlFile(fileName, username, "")
                    case e: Throwable =>
                      logger.error(s"JUST AFTER FAILING unknown : $e")
                  })

                  importResponse.onSuccess({
                    case resp =>
                      resp.fold(
                        ex => {
                          logger.error(s"Import with error: $ex")
                          logger.info(s"Rollback to previous state begins...")
                          impExpService ! ImportAMHXmlFile(fileName, username, ex)
                        },
                        ss => logger.info(s"Import with backup done successfully")
                      )
                  })

                  completeWithLocationHeader[String, XmlFileImported, Either[String, XmlFileImported]](
                    resource = importResponse, ifDefinedStatus = 201, ifEmptyStatus = 409)

                }
              }
            }
          }
        }
      }
    } ~
      pathPrefix("amhrouting" / "export") {
        cors(settings) {
          pathEnd {
            post {
              entity(as[ExportAMHXmlFile]) { xmlFile =>
                logger.info(s" amhrouting / export request with path $xmlFile")
                val exportResult = (impExpService ? xmlFile).mapTo[Either[String, XmlFileExported]]
                //.map(r => r.asInstanceOf[Either[String, XmlFileExported]])

                completeWithLocationHeader[String, XmlFileExported,
                  Either[String, XmlFileExported]](
                  resource = exportResult, ifDefinedStatus = 200, ifEmptyStatus = 500)



                //                onSuccess(exportResult) {
                //                   eitherResponse =>
                //                    eitherResponse.fold(
                //                      ex => complete(409, Some(ex)),
                //                      result => {
                //                        logger.debug("response onSuccess " + result)
                //                        complete(200,result)
                //                      })
                //                }
              }
            }
          } ~
            get {
              withSettings(special) {
                path(Segment) { fileName: String =>
                  logger.debug(" asking for file " + fileName)
                  complete {
                    // internally uses the configured fileIODispatcher:
                    val exportDir = config.getString("amh.export.dir")
                    val source = FileIO.fromFile(new File(s"$exportDir/$fileName"))
                    val headers = `Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" → s"AMHExport_$fileName")) :: Nil
                    HttpResponse(headers = headers, entity = HttpEntity(ContentTypes.`application/octet-stream`, source))
                  }
                }
              }
            }
        }
      }
  }

  def csvExportRoutes = logRequestResult("amhCSVExportController") {
    import com.groupaxis.groupsuite.synchronizator.http.GPHttpHelper._
    pathPrefix("amhrouting" / "csv" / "export" / "assignments") {
      cors(settings) {
        path(IntNumber) { assignmentType =>
          post {
            extractRequestContext { requestContext =>
              complete(
                (assignmentWriteService ? CreateOverviewCSVFile(assignmentType, loggedUser(requestContext), requestDate(requestContext)))
                  .map(r => r.asInstanceOf[OverviewCSVFileCreated])
              )
            }
          }
        } ~
          get {
            withSettings(special) {
              path(Segment) { fileName: String =>
                logger.info(" asking for file " + fileName)
                complete {
                  // internally uses the configured fileIODispatcher:
                  val exportDir = config.getString("amh.export.dir")
                  val source = FileIO.fromFile(new File(s"$exportDir/$fileName"))
                  val headers = `Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" → s"$fileName")) :: Nil
                  HttpResponse(headers = headers, entity = HttpEntity(ContentTypes.`application/octet-stream`, source))
                }
              }
            }
          }
      }
    } ~
      pathPrefix("amhrouting" / "csv" / "export" / "rules") {
        cors(settings) {
          path(IntNumber) { assignedType =>
            post {
              extractRequestContext { requestContext =>
                val assignedValue: Option[Boolean] = assignedType match {
                  case 1 => None //all rules
                  case 2 => Some(true) // assigned only
                  case 4 => Some(false) // unassigned only
                  case _ => None //all, when it is an unknown type
                }
                complete(
                  (ruleWriteService ? CreateRuleOverviewCSVFile(assignedValue, loggedUser(requestContext), requestDate(requestContext)))
                    .map(r => r.asInstanceOf[RuleOverviewCSVFileCreated])
                )
              }
            }
          } ~
            get {
              withSettings(special) {
                path(Segment) { fileName: String =>
                  logger.info(" asking for file " + fileName)
                  complete {
                    // internally uses the configured fileIODispatcher:
                    val exportDir = config.getString("amh.export.dir")
                    val source = FileIO.fromFile(new File(s"$exportDir/$fileName"))
                    val headers = `Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" → s"$fileName")) :: Nil
                    HttpResponse(headers = headers, entity = HttpEntity(ContentTypes.`application/octet-stream`, source))
                  }
                }
              }
            }
        }
      }
  }


  def ruleRoutes: Route = logRequestResult("routingController") {
    path("") {
      redirect("amhrouting/rules/", StatusCodes.PermanentRedirect)
    } ~
      pathPrefix("amhrouting" / "rules") {
        cors(settings) {
          path(Segment) { ruleCode: String =>
            get {
              complete((ruleService ? FindAMHRuleByCode(ruleCode)).map(r => r.asInstanceOf[Either[String, AMHRuleFound]]))
            } ~
              post {
                extractRequestContext { requestContext =>
                  entity(as[AMHRuleEntityUpdate]) { rule =>
                    completeWithLocationHeader[String, AMHRuleCreated, Either[String, AMHRuleCreated]](
                      resource = (ruleService ? CreateAMHRule(ruleCode, rule, loggedUser(requestContext), requestDate(requestContext))).map(r => r.asInstanceOf[Either[String, AMHRuleCreated]]),
                      ifDefinedStatus = 201, ifEmptyStatus = 409)
                  }
                }
              } ~
              put {
                extractRequestContext { requestContext =>
                  entity(as[AMHRuleEntityUpdate]) { rule =>
                    complete((ruleService ? UpdateAMHRule(ruleCode, rule, loggedUser(requestContext), requestDate(requestContext))).map(r => r.asInstanceOf[Either[String, AMHRuleUpdated]]))
                  }
                }
              } ~
              delete {
                extractRequestContext { requestContext =>
                  complete((ruleWriteService ? UnAssignRule(ruleCode, loggedUser(requestContext), requestDate(requestContext))).map(r => r.asInstanceOf[RuleUnAssigned]))
                }
              }
          } ~
            pathEnd {
              get {
                logger.info(s"here! amhrouting/rules request")

                logger.info(s"received amhrouting/rules request")
                val result = (ruleService ? FindAllAMHRules()).mapTo[Either[String, AMHRulesFound]]
                logger.info(s"response for amhrouting/rules request => $result")
                completeWithLocationHeader[String, AMHRulesFound, Either[String, AMHRulesFound]](
                  resource = result, ifDefinedStatus = 204, ifEmptyStatus = 500)
                //                complete {
                //                  result
                //                }
              }
            }
        }
      }
  }

  def assignmentRoutes: Route = logRequestResult("assignmentController") {
    path("") {
      redirect("amhrouting/assignments/", StatusCodes.PermanentRedirect)
    }
    pathPrefix("amhrouting" / "assignments") {
      cors(settings) {
        path(Segment) { assignmentCode: String =>
          get {
            complete((assignmentService ? FindAMHAssignmentByCode(assignmentCode)).map(r => r.asInstanceOf[Either[String, AMHAssignmentFound]]))
          } ~
            post {
              extractRequestContext { requestContext =>
                entity(as[AMHAssignmentEntityUpdate]) { assignment =>

                  completeWithLocationHeader[String, AMHAssignmentCreated, Either[String, AMHAssignmentCreated]](
                    resource = (assignmentService ? CreateAMHAssignment(assignmentCode, assignment, loggedUser(requestContext), requestDate(requestContext))).map(r => r.asInstanceOf[Either[String, AMHAssignmentCreated]]),
                    ifDefinedStatus = 201, ifEmptyStatus = 409)
                }
              }
            } ~
            put {
              extractRequestContext { requestContext =>
                entity(as[AMHAssignmentEntityUpdate]) { assignment =>
                  complete((assignmentService ? UpdateAMHAssignment(assignmentCode, assignment, loggedUser(requestContext), requestDate(requestContext))).map(r => r.asInstanceOf[Either[String, AMHAssignmentUpdated]]))
                }

              }
            } ~
            pathEnd {
              get {
                logger.info(s"here! amhrouting/assignments request")
                complete {
                  logger.info(s"received amhrouting/assignments request")
                  val result = (assignmentService ? FindAllAMHAssignments()).mapTo[AMHAssignmentsFound]
                  logger.info(s"response for amhrouting/assignments request => $result")
                  result
                }
              }
            }
        }
      }
    }
  }

  def distributionRoutes: Route = logRequestResult("distributionController") {
    path("") {
      redirect("amhrouting/distributions/", StatusCodes.PermanentRedirect)
    }
    pathPrefix("amhrouting" / "distributions") {
      cors(settings) {
        path(Segment) { distributionCode: String =>
          get {
            complete((distributionService ? FindAMHDistributionCpyByCode(distributionCode)).map(r => r.asInstanceOf[Either[String, AMHDistributionCpyFound]]))
          } ~
            post {
              extractRequestContext { requestContext =>
                entity(as[AMHDistributionCpyEntityUpdate]) { distributionCopy =>

                  completeWithLocationHeader[String, AMHDistributionCpyCreated, Either[String, AMHDistributionCpyCreated]](
                    resource = (distributionService ? CreateAMHDistributionCpy(distributionCode, distributionCopy, loggedUser(requestContext), requestDate(requestContext))).map(r => r.asInstanceOf[Either[String, AMHDistributionCpyCreated]]),
                    ifDefinedStatus = 201, ifEmptyStatus = 409)
                }
              }
            } ~
            put {
              extractRequestContext { requestContext =>
                entity(as[AMHDistributionCpyEntityUpdate]) { distributionCopy =>
                  complete((distributionService ? UpdateAMHDistributionCpy(distributionCode, distributionCopy, loggedUser(requestContext), requestDate(requestContext))).map(r => r.asInstanceOf[Either[String, AMHDistributionCpyUpdated]]))
                }
              }
            }
        } ~
          pathEnd {
            get {
              logger.info(s"here! amhrouting/distributions request")
              complete {
                logger.info(s"received amhrouting/distributions request")
                val result = (distributionService ? FindAllAMHDistributionCpy()).mapTo[AMHDistributionCpsFound]
                logger.info(s"response for amhrouting/distributions request => $result")
                result
              }
            }
          }
      }
    }
  }

  def feedbackRoutes: Route = logRequestResult("feedbackController") {
    path("") {
      redirect("amhrouting/feedbacks/", StatusCodes.PermanentRedirect)
    }
    pathPrefix("amhrouting" / "feedbacks") {
      cors(settings) {
        path(Segment) { feedbackCode: String =>
          get {
            complete((feedbackService ? FindAMHFeedbackDistributionCpyByCode(feedbackCode)).map(r => r.asInstanceOf[Either[String, AMHFeedbackDistributionCpyFound]]))
          } ~
            post {
              extractRequestContext { requestContext =>
                entity(as[AMHFeedbackDistributionCpyEntityUpdate]) { feedbackCopy =>

                  completeWithLocationHeader[String, AMHFeedbackDistributionCpyCreated, Either[String, AMHFeedbackDistributionCpyCreated]](
                    resource = (feedbackService ? CreateAMHFeedbackDistributionCpy(feedbackCode, feedbackCopy, loggedUser(requestContext), requestDate(requestContext))).map(r => r.asInstanceOf[Either[String, AMHFeedbackDistributionCpyCreated]]),
                    ifDefinedStatus = 201, ifEmptyStatus = 409)
                }
              }
            } ~
            put {
              extractRequestContext { requestContext =>
                entity(as[AMHFeedbackDistributionCpyEntityUpdate]) { feedbackCopy =>
                  complete((feedbackService ? UpdateAMHFeedbackDistributionCpy(feedbackCode, feedbackCopy, loggedUser(requestContext), requestDate(requestContext))).map(r => r.asInstanceOf[Either[String, AMHFeedbackDistributionCpyUpdated]]))
                }
              }
            }
        } ~
          pathEnd {
            get {
              logger.info(s"here! amhrouting/feedbacks request")
              complete {
                logger.info(s"received amhrouting/feedbacks request")
                val result = (feedbackService ? FindAllAMHFeedbackDistributionCpy()).mapTo[AMHFeedbackDistributionCpsFound]
                logger.info(s"response for amhrouting/feedbacks request => $result")
                result
              }
            }
          }
      }
    }
  }


  Http(context.system).bindAndHandle(ruleRoutes ~ assignmentRoutes ~ distributionRoutes ~ feedbackRoutes ~ impExpRoutes ~ csvExportRoutes, config.getString("http.interface"), config.getInt("http.port"))
    .pipeTo(self)

  override def receive = binding

  private def binding: Receive = {
    case serverBinding@Http.ServerBinding(address) =>
      logger.info(s"Listening on $address")
    //      context.become(bound(serverBinding))

    case Status.Failure(cause) =>
      logger.error(s"Can't bind to address:port ${cause.getMessage}")
      context.stop(self)
  }

  //  private def bound(serverBinding: Http.ServerBinding): Receive = {
  //    case Stop =>
  //      serverBinding.unbind()
  //      context.stop(self)
  //  }

}

object RuleController {

  final val Name = "rule-controller"

  def props(ruleService: ActorRef, internalTimeout: Timeout): Props = Props(classOf[RuleController], ruleService, internalTimeout)

  //  def routes(ruleService: ActorRef, internalTimeout: Timeout, executionContext: ExecutionContext) = {
  //    implicit val timeout = internalTimeout
  //    implicit val ec: ExecutionContext = executionContext
  //    logRequestResult("routingController") {
  //      path("") {
  //        redirect("routing/rules", StatusCodes.PermanentRedirect)
  //      } ~
  //        pathPrefix("routing" / "rules") {
  //          pathEnd {
  //            get {
  //              complete {
  //                (ruleService ? FindAllRules).mapTo[Set[ConditionFunction]]
  //              }
  //            } ~
  //              post {
  //                entity(as[Rule]) { rule ⇒
  //                  complete(StatusCodes.OK, rule.toString)
  //                }
  //              }
  //          } ~
  //            path(LongNumber) { id =>
  //              complete(
  //                //                  if (id == 1) rule1 else if (id == 2) rule2 else {
  //                //                val error = s"FreeGeoIP request failed with status code 501 and entity NON"
  //                //                logger.error(error)
  //                //                new IOException(error)
  //                //              }
  //                StatusCodes.OK, "salut!")
  //            }
  //        } ~
  //        pathPrefix("counter") {
  //          complete(StatusCodes.OK, "wa wa za za")
  //        }
  //    }
  //  }

}

class RuleController(ruleService: ActorRef, internalTimeout: Timeout) extends Actor with Logging {
  //  implicit val logger: LoggingAdapter = Logging(system, this.getClass)
  //  implicit val timeout = internalTimeout
  //  implicit val ec: ExecutionContext = system.dispatcher

  def receive: Receive = {
    case toto: String =>
  }
}

//object test extends App with Logging {
//  import io.circe.generic.auto._
//  import io.circe.parser._
//
////, "active":"true","dataOwner":"DATA_OWNER","lockCode":"LockCode","description":"Description","environment":"UNKNOWN","version":"DEFAULT","name":"NONAME"
//
//  val ff= decode[AMHDistributionCpyEntityUpdate](
//    """{"sequence":13, "environment":"UNKNOWN","version":"DEFAULT",
//      "rules":[],
//      "backends":[
//      {"code":"BNP_Distribute_Autocancel_TOBEX2",
//      "backCode":"TOBEX_distribute",
//      "backDirection":"DISTRIBUTION",
//      "dataOwner":"B_DataOwner1",
//      "lockCode":"B_LockCode1",
//      "environment":"UNKNOWN",
//      "version":"DEFAULT"}]}""")
//  logger.debug("ff "+ff)
//
//}




