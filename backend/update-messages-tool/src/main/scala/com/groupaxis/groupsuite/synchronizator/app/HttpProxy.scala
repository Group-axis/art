package com.groupaxis.groupsuite.synchronizator.app

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.config.ConfigFactory

object HttpProxy extends App {

  val conf = ConfigFactory.parseString("akka.http.server.parsing.max-method-length=" + 80000).
    withFallback(ConfigFactory.load())
  implicit val system = ActorSystem("Proxy", conf)
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  val proxy = Route { context =>
    val request = context.request
    println("Opening connection to " + request.uri.authority.host.address)
    val flow = Http(system).outgoingConnection(request.uri.authority.host.address(), 8090)
    val handler = Source.single(context.request)
      .via(flow)
      .runWith(Sink.head)
      .flatMap(context.complete(_))
    handler
  }

  val binding = Http(system).bindAndHandle(handler = proxy, interface = "localhost", port = 1080)
}