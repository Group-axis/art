package com.groupaxis.groupsuite.amh.routing.application.services

import java.io.File

import akka.actor.ActorSystem
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.io.SynchronousFileSource
import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.Future
import scala.concurrent.duration._

object TestMultipartFileUpload extends App with Logging{
  val testConf: Config = ConfigFactory.parseString("""
    akka.loglevel = INFO
    akka.log-dead-letters = off""")
  implicit val system = ActorSystem("ServerTest", testConf)
  import system.dispatcher
  implicit val materializer = ActorMaterializer()

  val testFile = new File(args(0))

  def startTestServer(): Future[ServerBinding] = {
    import akka.http.scaladsl.server.Directives._

    val route: Route =
      path("upload") {
        entity(as[Multipart.FormData]) { (formdata: Multipart.FormData) ⇒
          val fileNamesFuture = formdata.parts.mapAsync(1) { p ⇒
            logger.debug(s"Got part. name: ${p.name} filename: ${p.filename}")

            logger.debug("Counting size...")
            @volatile var lastReport = System.currentTimeMillis()
            @volatile var lastSize = 0L
            def receiveChunk(counter: (Long, Long), chunk: ByteString): (Long, Long) = {
              val (oldSize, oldChunks) = counter
              val newSize = oldSize + chunk.size
              val newChunks = oldChunks + 1

              val now = System.currentTimeMillis()
              if (now > lastReport + 1000) {
                val lastedTotal = now - lastReport
                val bytesSinceLast = newSize - lastSize
                val speedMBPS = bytesSinceLast.toDouble / 1000000 /* bytes per MB */ / lastedTotal * 1000 /* millis per second */

                logger.debug(f"Already got $newChunks%7d chunks with total size $newSize%11d bytes avg chunksize ${newSize / newChunks}%7d bytes/chunk speed: $speedMBPS%6.2f MB/s")

                lastReport = now
                lastSize = newSize
              }
              (newSize, newChunks)
            }

            p.entity.dataBytes.runFold((0L, 0L))(receiveChunk).map {
              case (size, numChunks) ⇒
                logger.debug(s"Size is $size")
                (p.name, p.filename, size)
            }
          }.runFold(Seq.empty[(String, Option[String], Long)])(_ :+ _).map(_.mkString(", "))

          complete {
            fileNamesFuture
          }
        }
      }
    akka.http.scaladsl.Http().bindAndHandle(route, interface = "localhost", port = 0)
  }

  def createEntity(file: File): Future[RequestEntity] = {
    require(file.exists())
    val formData =
      Multipart.FormData(
        Source.single(
          Multipart.FormData.BodyPart(
            "test",
            HttpEntity(MediaTypes.`application/octet-stream`, file.length(), SynchronousFileSource(file, chunkSize = 100000)), // the chunk size here is currently critical for performance
            Map("filename" -> file.getName))))
    Marshal(formData).to[RequestEntity]
  }

  def createRequest(target: Uri, file: File): Future[HttpRequest] =
    for {
      e ← createEntity(file)
    } yield HttpRequest(HttpMethods.POST, uri = target, entity = e)

  try {
    val result =
      for {
        ServerBinding(address) ← startTestServer()
        _ = logger.debug(s"Server up at $address")
        port = address.getPort
        target = Uri(scheme = "http", authority = Uri.Authority(Uri.Host("localhost"), port = port), path = Uri.Path("/upload"))
        req ← createRequest(target, testFile)
        _ = logger.debug(s"Running request, uploading test file of size ${testFile.length} bytes")
        response ← akka.http.scaladsl.Http().singleRequest(req)
        responseBodyAsString ← Unmarshal(response).to[String]
      } yield responseBodyAsString

    result.onComplete { res ⇒
      logger.debug(s"The result was toto $res")
      system.shutdown()
    }

    system.scheduler.scheduleOnce(60.seconds) {
      logger.debug("Shutting down after timeout...")
      system.shutdown()
    }
  } catch {
    case _: Throwable ⇒ system.shutdown()
  }
}

