package com.groupaxis.groupsuite.synchronizator.http

import akka.http.javadsl.model.headers.RawRequestURI
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.RequestContext
import org.joda.time.DateTime

import scala.util.Try


object GPHttpHelper {
  def headerValue(request : HttpRequest, name : String) : Option[String] = {
    val headerValue = request.getHeader(name).orElse(RawRequestURI.create("")).value
    if (headerValue.isEmpty) None else Some(headerValue)
  }

  def headerIntValue(request : HttpRequest, name : String) : Option[Int] = {
    val headerValue = request.getHeader(name).orElse(RawRequestURI.create("")).value
    val resp = try{ headerValue.toInt} catch { case nfe : NumberFormatException => -1 }
    if (resp < 0) None else Some(headerValue.toInt)
  }

  //val jobStatus = GPHttpHelper.headerValue(request, "job_status").map(v => )

  def loggedUser(requestContext : RequestContext) = headerValue(requestContext.request, "userId")
  def requestDate(requestContext : RequestContext) = headerValue(requestContext.request, "time")
    .map(str => Try(str.toLong).getOrElse(DateTime.now().getMillis))
    .map(lg => new DateTime(lg))

}
