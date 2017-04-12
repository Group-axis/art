package com.groupaxis.groupsuite.synchronizator.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

import scala.collection.immutable.SortedMap
import scala.concurrent.Future
import scala.util.{Failure, Success}

case class RestClient (address: String, port: Int, poolSettings: ConnectionPoolSettings)
                      (implicit val system: ActorSystem, implicit val materializer: ActorMaterializer){


  import system.dispatcher
  private val pool = Http().cachedHostConnectionPool[Int](address, port, poolSettings)

  def exec(req: HttpRequest): Future[HttpResponse] = {
    Source.single(req → 1)
      .via(pool)
      .runWith(Sink.head).flatMap {
      case (Success(r: HttpResponse), _) ⇒ Future.successful(r)
      case (Failure(f), _) ⇒ Future.failed(f)
    }
  }

  def execFlatten(requests: Iterable[HttpRequest]): Future[Iterable[HttpResponse]] = {
    Source(requests.zipWithIndex.toMap)
      .via(pool)
      .runFold(SortedMap[Int, Future[HttpResponse]]()) {
        case (m, (Success(r), idx)) ⇒ m + (idx → Future.successful(r))
        case (m, (Failure(e), idx)) ⇒ m + (idx → Future.failed(e))
      }.flatMap(r ⇒ Future.sequence(r.values))
  }

  def exec(requests: Iterable[HttpRequest]): Future[Iterable[Future[HttpResponse]]] = {
    Source(requests.zipWithIndex.toMap)
      .via(pool)
      .runFold(SortedMap[Int, Future[HttpResponse]]()) {
        case (m, (Success(r), idx)) ⇒ m + (idx → Future.successful(r))
        case (m, (Failure(e), idx)) ⇒ m + (idx → Future.failed(e))
      }.map(r ⇒ r.values)
  }
}
