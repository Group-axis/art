package com.groupaxis.groupsuite.amh.routing.interfaces.http

import akka.http.scaladsl.marshalling.{ToResponseMarshallable, ToResponseMarshaller}
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.{Directives, Route}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.Future
//import de.heikoseeberger.akkahttpcirce.CirceSupport

trait HttpResource extends Directives with JsonSupport with Logging {

//  implicit def executionContext: ExecutionContext
//  import CirceSupport._
//  import io.circe.generic.auto._

  def innerF[A, B : ToResponseMarshaller, T <: Either[A, B]](ifDefinedStatus: Int, ifEmptyStatus: Int)(t : T) : Route =
  t match {
          case Left(ex) => logger.debug("error");complete(ifEmptyStatus, ex.asInstanceOf[String])
          case Right(payload) => logger.debug("payload");completeWithLocationHeader(ifDefinedStatus, payload)
        }

  def completeWithLocationHeader[A, B : ToResponseMarshaller, T <: Either[A, B]](resource: Future[T], ifDefinedStatus: Int, ifEmptyStatus: Int): Route =
    onSuccess(resource) (innerF[A,B,T](ifDefinedStatus, ifEmptyStatus))
//    {
//      case Left(ex) => logger.debug("error");complete(ifEmptyStatus, ex.asInstanceOf[String])
//      case Right(payload) => logger.debug("payload");completeWithLocationHeader(ifDefinedStatus, payload)
//    }
    
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

  def completeWithLocationHeader[T : ToResponseMarshaller](status: Int, resourceId: T): Route =
    extractRequestContext { requestContext =>
      val request = requestContext.request
      val location = request.uri.copy(path = request.uri.path)
      respondWithHeader(Location(location)) {
        complete(ToResponseMarshallable(resourceId))
//        complete(Some(resourceId))
      }   
    }

//  def complete[T: ToResponseMarshaller](resource: Future[Option[T]]): Route =
//    onSuccess(resource) {
//      case Some(t) => complete(ToResponseMarshallable(t))
//      case None    => complete(404, "") //None
//    }

  def complete[A, B : ToResponseMarshaller, T <: Either[A, B]](resource: Future[T]): Route =
    onSuccess(resource) {
      case Left(ex)=> complete(500, Some(ex.asInstanceOf[String]))
      case  Right(rule) =>
        logger.debug("response onSuccess "+ rule)
            complete(ToResponseMarshallable(rule))
      case _ => complete(404, "") //None
    }

  def complete(resource: Future[Unit]): Route = onSuccess(resource) { complete(204, "") /*None*/ }

}