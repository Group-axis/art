package com.groupaxis.groupsuite.routing.interfaces.http

import akka.http.scaladsl.marshalling.{ToResponseMarshallable, ToResponseMarshaller}

import scala.concurrent.Future
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.{Directives, Route}
import org.apache.logging.log4j.scala.Logging
//import de.heikoseeberger.akkahttpcirce.CirceSupport

trait HttpResource extends Directives with JsonSupport with Logging
{

//    implicit def executionContext: ExecutionContext
  //import CirceSupport._
  import io.circe.generic.auto._

  def completeWithLocationHeader[A, B : ToResponseMarshaller, T <: Either[A, B]](resourceId: Future[T], ifDefinedStatus: Int, ifEmptyStatus: Int): Route =
    onSuccess(resourceId) {
      t => {
        t.fold(
          //ex => complete(ifEmptyStatus, Some(ex)),
            ex => complete(ifEmptyStatus, Some(ex.asInstanceOf[String])),
          rule => {
            logger.debug("response onSuccess "+ rule)
            completeWithLocationHeader(ifDefinedStatus, rule)
          })
      }
      //case None => complete(ifEmptyStatus, Some("No content"))
    }
    
//  def completeWithLocationHeader[A, B, T <: Either[A, B]](resourceId: Future[Option[T]], ifDefinedStatus: Int, ifEmptyStatus: Int): Route =
//    onSuccess(resourceId) {
//      case Some(t) => {
//        t.fold(
//          //ex => complete(ifEmptyStatus, Some(ex)),
//            ex => complete(ifEmptyStatus, Some(ex.asInstanceOf[String])),
//          rule => completeWithLocationHeader(ifDefinedStatus, rule))
//      }
//      case None => complete(ifEmptyStatus, Some("No content"))
//    }

  def completeWithLocationHeader[T](status: Int, resourceId: T): Route =
    extractRequestContext { requestContext =>
      val request = requestContext.request
      val location = request.uri.copy(path = request.uri.path)
      respondWithHeader(Location(location)) {
        complete(status, None)
//        complete(status, Some(resourceId))
      }   
    }

//  def complete[T: ToResponseMarshaller](resource: Future[Option[T]]): Route =
//    onSuccess(resource) {
//      case Some(t) => complete(ToResponseMarshallable(t))
//      case None    => complete(404, "") //None
//    }

  def complete[A, B : ToResponseMarshaller, T <: Either[A, B]](resource: Future[T]): Route =
    onSuccess(resource) {
      case t =>
        t.fold(
          //ex => complete(ifEmptyStatus, Some(ex)),
          ex => complete(500, Some(ex.asInstanceOf[String])),
          rule => {
            logger.debug("response onSuccess "+ rule)
            complete(ToResponseMarshallable(rule))
          })
      case _ => complete(404, "") //None
    }

  def complete(resource: Future[Unit]): Route = onSuccess(resource) { complete(204, "") /*None*/ }

}