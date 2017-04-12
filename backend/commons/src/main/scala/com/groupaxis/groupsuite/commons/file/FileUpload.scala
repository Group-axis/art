package com.groupaxis.groupsuite.commons.file

import java.io.File

import akka.actor.{Actor, ActorSystem, Props, Status}
import akka.http.javadsl.model.Multipart.BodyPart
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{ContentDispositionTypes, `Content-Disposition`}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.settings.RoutingSettings
import akka.pattern.pipe
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.FileIO
import akka.util.Timeout
import ch.megard.akka.http.cors.CorsDirectives._
import ch.megard.akka.http.cors.CorsSettings
import de.heikoseeberger.akkahttpcirce.CirceSupport
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.Future

class HttpServer() extends Actor with Logging with CirceSupport  {
    implicit val system = context.system
//  implicit val ass: ActorContext = context
  implicit val materializer = ActorMaterializer()
  //  // needed for the future flatMap/onComplete in the end
  implicit val executionContext = context.dispatcher

  import scala.concurrent.duration._
  implicit val timeout: Timeout = 10.seconds

  val settings = CorsSettings.defaultSettings.copy(allowedMethods = scala.collection.immutable.Seq(GET, POST, PUT, DELETE, HEAD, OPTIONS))

  val special = RoutingSettings(system).withFileIODispatcher("special-io-dispatcher")

  def sample() =
    path(Segment) { fileName: String =>
      logger.debug(" asking for file "+ fileName)
      complete {
        // internally uses the configured fileIODispatcher:
        val source = FileIO.fromFile(new File(s"C:/dev/DFS/AMH/$fileName"))
        val headers = `Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" → s"$fileName")) :: Nil
        HttpResponse(headers = headers, entity = HttpEntity(ContentTypes.`application/octet-stream`, source))
      }
    }

  def download: Route = {
    pathPrefix("files" / "download") {
      logger.debug("Por aqui!")
      cors(settings) {
        get {
          withSettings(special) {
            sample() // `special` file-io-dispatcher will be used to read the file
          }
        } ~ path("test") {
          complete(200, "EVerything is OK")
        }
      }
    }
  }
  def routes: Route = {
    path("user" / "upload" / "file") {
      cors(settings) {
        (post & entity(as[Multipart.FormData])) { fileData :Multipart.FormData =>
          complete {
//            val fileName = UUID.randomUUID().toString
//            val temp = System.getProperty("java.io.tmpdir")
//            val filePath = temp + "/" + fileName
//            processFile(filePath, fileData).map { fileSize =>
//              HttpResponse(StatusCodes.OK, entity = s"File successfully uploaded. Fil size is $fileSize")
//            }.recover {
//              case ex: Exception => HttpResponse(StatusCodes.InternalServerError, entity = "Error in file uploading")
//            }

            processFile(fileData).map(data => HttpResponse(StatusCodes.OK, entity = s"Data : $data successfully saved."))
              .recover {
                case ex: Exception => HttpResponse(StatusCodes.InternalServerError, entity = "Error in processing the multi part data")
              }
          }
        }
      }
    }
  }
  private def processFile(formData: Multipart.FormData): Future[Map[String, Any]] = {
     formData.parts.mapAsync[(String, Any)](1) {

      case file: BodyPart if file.name == "file" =>
        val tempFile = File.createTempFile("import_", ".zip")
        file.entity.dataBytes.runWith(FileIO.toFile(tempFile)).map(_ => file.name -> file.getFilename().get())

      case data: BodyPart => data.toStrict(2.seconds).map(strict => data.name -> strict.entity.data.utf8String)

    }.runFold(Map.empty[String, Any])((map, tuple) => map + tuple)
  }

  val serverBinding = Http().bindAndHandle(routes ~ download, "127.0.0.1", 7777)
    .pipeTo(self)

  def receive: Receive = {
    case serverBinding@Http.ServerBinding(address) =>
      logger.info("Listening on $address")
    //      context.become(bound(serverBinding))

    case Status.Failure(cause) =>
      logger.error(s"Can't bind to address:port ${cause.getMessage}")
      context.stop(self)
  }
}

object uploadServer extends App {

  implicit val system = ActorSystem()
  //  implicit val executor = system.dispatcher
  val actorSer = system.actorOf(Props(new HttpServer()))
}