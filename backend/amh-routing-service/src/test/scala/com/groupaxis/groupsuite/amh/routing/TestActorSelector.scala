package com.groupaxis.groupsuite.amh.routing

import java.io.IOException

import akka.actor.{Actor, ActorSelection, ActorSystem, Props, Terminated}
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.Timeout
import com.groupaxis.groupsuite.amh.routing.interfaces.http.HttpResource
import com.groupaxis.groupsuite.routing.write.domain.audit.messages.AMHRoutingAuditMessages.{AMHRoutingAuditResponse, CreateBackendAssignment, RoutingCreationDone}
import com.groupaxis.groupsuite.routing.write.domain.model.amh.assignment.AMHAssignmentEntity
import com.groupaxis.groupsuite.synchronizator.http.RestClient
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object TestActorSelector extends App {

  val conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=0").
    withFallback(ConfigFactory.load())
  val system = ActorSystem("toto", conf)
  system.actorOf(Props[Kenny], "kenny") ! "Start"

}

object MicroHttpClient {
  def apply[T <: AMHRoutingAuditResponse](restClient: RestClient) = new MicroHttpClient[T](restClient)
}

class MicroHttpClient[T <: AMHRoutingAuditResponse](val restClient: RestClient) extends HttpResource {

  import io.circe.generic.auto._

  //  case class OK()
  //  case class BadRequest()

  //  lazy val zombieConnectionFlow: Flow[HttpRequest, HttpResponse, Any] =
  //    Http().outgoingConnection("localhost", 9001)

  //  def zombieRequest(request:HttpRequest): Future[HttpResponse] =
  //    Source.single(request).via(zombieConnectionFlow).runWith(Sink.head)

  //  def fetchZombieInfo(id: String) : Future[Either[String, String]] = {
  //    zombieRequest(RequestBuilding.Get(s"/zombies/$id")).flatMap { response =>
  //
  //      response.status match {
  //        case StatusCodes.OK => Unmarshal(response.entity).to[String].map(Right(_))
  //        case StatusCodes.BadRequest => Future.successful(Left(s"bad request"))
  //        case _ => Unmarshal(response.entity).to[String].flatMap { entity =>
  //          val error = s"FAIL - ${response.status}"
  //          Future.failed(new IOException(error))
  //        }
  //      }
  //
  //    }
  //  }

  def createBackend(request: HttpRequest)
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

}
import org.apache.logging.log4j.scala.Logging
class Kenny extends Actor with Logging with HttpResource {

  import scala.concurrent.duration._

  implicit val ec: ExecutionContext = context.dispatcher
  implicit val system: ActorSystem = context.system
  implicit val mat: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))
  import io.circe.generic.auto._

  implicit val timeout = Timeout(5.seconds)
  val restClient = MicroHttpClient[RoutingCreationDone](RestClient("127.0.0.1", 8084, ConnectionPoolSettings(context.system)))
  logger.debug("....")
  var test1: ActorSelection = _
  //  test1 ! "AreYouThere"
  import RequestBuilding._

  def receive = {
    case "Start" =>

      logger.info("Kenny recevied start!!")
      test1 = context.actorSelection("akka.tcp://groupsuite-audit@127.0.0.1:5150/user/http-audit-service/audit-user-write-service")
      test1.resolveOne().onComplete(
        {
          case Success(actorRef) => context.watch(actorRef)
          case Failure(_) => context.watch(_)
        })
      val newEntity: AMHAssignmentEntity = AMHAssignmentEntity("code_toto", 2, "back_code", "back_direction", Some("Y"), None, None, Some("description"), "", "")
      val createAssignment = CreateBackendAssignment("irach", DateTime.now(), newEntity)
      val request = Post("/audit/routing/backend", createAssignment)
      val f = restClient.createBackend(request)
      f.onComplete({
        case Success(resp) =>
          resp.fold(
            error => logger.error(s"msg: $error"),
            done => logger.info(s" backend done $done")
          )
        case e => logger.error(s" $e")
      })

      test1 ! "AreYouThere"
      logger.info("Kenny AreYouThere sent!")
    case "IamHere" => test1 ! "Ping"
    case "Pong" => logger.info("pong recevied in Kenny")
    case a: String => logger.info(s"Nothing match $a")
    case Terminated(kenny) =>
      logger.debug("OMG, they killed Audit")
      test1 = null
    case _ => logger.debug("Parent received a message")
  }
}
