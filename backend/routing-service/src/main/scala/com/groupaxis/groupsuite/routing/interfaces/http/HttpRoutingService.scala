package com.groupaxis.groupsuite.routing.interfaces.http


import java.io.File

import akka.actor.SupervisorStrategy.{Restart, Resume, Stop}
import akka.actor.{Actor, OneForOneStrategy, Props, Status}
import akka.http.scaladsl._
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{ContentDispositionTypes, `Content-Disposition`}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.settings.RoutingSettings
import akka.pattern.{ask, pipe}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Materializer, Supervision}
import akka.stream.scaladsl.FileIO
import akka.util.Timeout
import ch.megard.akka.http.cors.CorsDirectives._
import ch.megard.akka.http.cors.CorsSettings
import com.groupaxis.groupsuite.datastore.jdbc.Database
import com.groupaxis.groupsuite.routing.application.services.RuleWriteService
import com.groupaxis.groupsuite.routing.infrastructor.jdbc.JdbcRuleWriteRepository
import com.groupaxis.groupsuite.routing.write.domain.model.routing.rule.RuleMessages._
import com.groupaxis.groupsuite.routing.write.domain.model.routing.rule.{RuleDAO, RuleEntityUpdate}
import com.groupaxis.groupsuite.xml.parser.amh.model.Messages.{ExportXmlFile, XmlFileExported, XmlFileImported}
import com.groupaxis.groupsuite.xml.parser.routing.infrastructure.jdbc.FileRuleXmlWriteRepository
import com.typesafe.config.Config
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.ExecutionContext


object HttpRoutingService {

  final val Name = "http-routing-service"
  def props(config: Config, database: Database): Props = Props(new HttpRoutingService(config, database))

}
class HttpRoutingService(config: Config, database: Database) extends Actor with Logging with HttpResource {
  implicit val ec: ExecutionContext = context.dispatcher
  import scala.concurrent.duration._
  implicit val timeout: Timeout = 5.minutes

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1.minute) {
      case _: ArithmeticException      => Resume
      case _: NullPointerException     => Restart
      case _: IllegalArgumentException => Stop
      case e: Exception                =>
        logger.warn("[HttpRoutingService] Exception has been received, so restarting the actor " + e.getMessage)
        e.printStackTrace()
        Restart
    }

  val decider: Supervision.Decider = (ex) => Supervision.Resume
  import io.circe.generic.auto._
  implicit val mat: Materializer = ActorMaterializer(ActorMaterializerSettings(context.system).withSupervisionStrategy(decider))

//  val ruleDao : RuleDAO = new RuleDAO(slick.driver.PostgresDriver)
  val ruleRepo : JdbcRuleWriteRepository = new JdbcRuleWriteRepository(database, 10.seconds)
  val ruleService = context.actorOf(RuleWriteService.props(ruleRepo), RuleWriteService.Name)
  val impExpRuleService = context.actorOf(FileRuleXmlWriteRepository.props(ruleRepo), FileRuleXmlWriteRepository.Name)

  val settings = CorsSettings.defaultSettings.copy(allowedMethods = scala.collection.immutable.Seq(GET, POST, PUT, DELETE, HEAD, OPTIONS))
  val special = RoutingSettings(context.system).withFileIODispatcher("special-io-dispatcher")

  def impExpRoutes: Route = logRequestResult("impExpRuleController") {
    pathPrefix("routing" / "import") {
      cors(settings) {
        pathEnd {
          withoutRequestTimeout {
            post {
              entity(as[Multipart.FormData]) { fileData =>
                logger.info(s" routing/import request with path $fileData")
                completeWithLocationHeader[String, XmlFileImported, Either[String, XmlFileImported]](
                  resourceId = (impExpRuleService ? fileData).map(r => r.asInstanceOf[Either[String, XmlFileImported]]),
                  ifDefinedStatus = 201, ifEmptyStatus = 409)
              }
            }
          }
        }
      }
    } ~
      pathPrefix("routing" / "export") {
        cors(settings) {
          pathEnd {
            post {
              entity(as[ExportXmlFile]) { xmlFile =>
                logger.info(s" routing / export request with path $xmlFile")
                val exportResult = (impExpRuleService ? xmlFile).map(r => r.asInstanceOf[Either[String, XmlFileExported]])
                onSuccess(exportResult) {
                  eitherResponse =>
                    eitherResponse.fold(
                      ex => complete(409, Some(ex)),
                      result => {
                        logger.debug("response onSuccess "+ result)
                        complete(result)
                      })
                }
              }
            }
          } ~
            get{
              withSettings(special) {
                path(Segment) { fileName: String =>
                  logger.debug(" asking for file "+ fileName)
                  complete {
                    // internally uses the configured fileIODispatcher:
                    val exportDir = config.getString("sibes.export.dir")
                    val source = FileIO.fromFile(new File(s"$exportDir/$fileName"))
                    val headers = `Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" → s"SibesExport_$fileName")) :: Nil
                    HttpResponse(headers = headers, entity = HttpEntity(ContentTypes.`application/octet-stream`, source))
                  }
                }
              }
            }
        }
      }

  }

  def routes: Route = logRequestResult("routingController") {
    path("") {
      redirect("points/", StatusCodes.PermanentRedirect)
    } ~
      pathPrefix("points" / Segment / "rules") { pointName: String =>
        cors(settings) {

            path(LongNumber) { sequence =>
              get {
                complete((ruleService ? FindRuleByPK(pointName, sequence)).map(r => r.asInstanceOf[Either[String, RuleFound]]))
              } ~
                post {
                  entity(as[RuleEntityUpdate]) { rule =>
                    completeWithLocationHeader[String, RuleCreated, Either[String, RuleCreated]](
                      resourceId = (ruleService ? CreateRule(pointName, sequence, rule)).map(r => r.asInstanceOf[Either[String, RuleCreated]]),
                      ifDefinedStatus = 201, ifEmptyStatus = 409)
                  }
                } ~
                put {
                  entity(as[RuleEntityUpdate]) { rule =>
                    complete((ruleService ? UpdateRule(pointName, sequence, rule)).map(r => r.asInstanceOf[Either[String, RuleUpdated]]))
                  }

                }
            } ~
              pathEnd {
                get {
                  logger.info(s"here! points/$pointName/rules request")
                  complete((ruleService ? FindAllRules(pointName)).mapTo[Either[String, RulesFound]])
                }
              }
        }
      }
  }

  Http(context.system).bindAndHandle(routes ~ impExpRoutes, config.getString("http.interface"), config.getInt("http.port"))
    .pipeTo(self)

  override def receive = binding

  private def binding: Receive = {
    case serverBinding @ Http.ServerBinding(address) =>
      logger.info(s"Listening on $address")
    case Status.Failure(cause) =>
      logger.error(s"Can't bind to address:port ${cause.getMessage}")
      context.stop(self)
  }

}
