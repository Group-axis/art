package com.groupaxis.groupsuite.audit

import akka.actor.{Actor, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.Future
import com.groupaxis.groupsuite.persistence.datastore.jdbc.Database
import com.typesafe.config.{Config, ConfigFactory}
import spray.json.DefaultJsonProtocol

case class Person(id:Int, name:String)
object PersonDb{
  case class CreatePerson(person:Person)
  case object FindAllPeople
}
class PersonDb extends Actor{
  import PersonDb._
  var people:Map[Int, Person] = Map.empty

  def receive = {
    case FindAllPeople =>
      sender ! people.values.toList

    case CreatePerson(person) =>
      people = people ++ Map(person.id -> person)
      sender ! person
  }
}

trait MyJsonProtocol extends DefaultJsonProtocol {
  implicit val personFormat = jsonFormat2(Person)
}

object ExperimentHttpLowLevel extends MyJsonProtocol with SprayJsonSupport {
//  import scala.concurrent.ExecutionContext.Implicits.global
  private val auditClusterName = "groupsuite-audit"


  def main(args: Array[String]): Unit = {
    val conf : Config = ConfigFactory.parseString("akka.remote.netty.tcp.port=0").
    withFallback(ConfigFactory.load("worker"))
//    val database = new Database(slick.driver.PostgresDriver, conf.getString("database.url"), conf.getString("database.user"), conf.getString("database.password"))
//    startHttpService(database, 0)
    example1()
  }

  def example1() = {


//import scala.concurrent.ExecutionContext.Implicits.global
    implicit val system = ActorSystem()
    implicit val materializer = ActorMaterializer()
    import system.dispatcher
    import akka.pattern.ask
    import spray.json._
    import akka.http.scaladsl.model._
    implicit val timeout = Timeout(5.seconds)

    val personDb = system.actorOf(Props[PersonDb])
    val requestHandler:HttpRequest => Future[HttpResponse] = {
      case HttpRequest(HttpMethods.GET, Uri.Path("/api/person"), _, _, _) =>
        val peopleFut =
          (personDb ? PersonDb.FindAllPeople).mapTo[List[Person]]
        peopleFut.map{ people =>
          val respJson = people.toJson
          val ent = HttpEntity(ContentTypes.`application/json`,
            respJson.prettyPrint )
          HttpResponse(StatusCodes.OK, entity = ent)
        }
      case HttpRequest(HttpMethods.POST, Uri.Path("/api/person"), _, ent, _) =>
        val strictEntFut = ent.toStrict(timeout.duration)
        for{
          strictEnt <- strictEntFut
          person = strictEnt.data.utf8String.parseJson.convertTo[Person]
          result <- (personDb ? PersonDb.CreatePerson(person)).
            mapTo[Person]
        } yield {
          val respJson = result.toJson
          val ent = HttpEntity(ContentTypes.`application/json`,
            respJson.prettyPrint )
          HttpResponse(StatusCodes.OK, entity = ent)
        }
      case req:HttpRequest =>
//        req.discardEntityBytes()
        Future.successful(HttpResponse(StatusCodes.NotFound ))
    }


  }


  def startHttpService(database: Database, port: Int): Unit = {

      val conf = ConfigFactory.load()
      implicit val system = ActorSystem(auditClusterName, conf)
      implicit val materializer = ActorMaterializer()
      val serverSource: Source[Http.IncomingConnection,  Future[Http.ServerBinding]] = Http().bind(interface = "localhost",  port = 8080)

      val sink = Sink.foreach[Http.IncomingConnection]{ conn =>
        conn.handleWithAsyncHandler(r => asynchEx1)
      }

      val bindingFut = serverSource.to(sink).run
    }

  case class UserEntity(id : String)


  private def talkToDatabase() = {
    import scala.concurrent.ExecutionContext.Implicits.global
    Future { UserEntity("id_toto") }
  }

  private def asynchEx1 ={
    import scala.concurrent.ExecutionContext.Implicits.global
    val fut = talkToDatabase()
    fut.map{dbentity =>
      val respEntity =
        HttpEntity(ContentTypes.`application/json`,
          s"""{"id": ${dbentity.id}}""")
      HttpResponse(StatusCodes.OK, entity = respEntity)
    }
  }

}












