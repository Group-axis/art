package com.groupaxis.groupsuite.commons.service

import scala.concurrent.ExecutionContext

import com.typesafe.config.Config

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.stream.Materializer

trait GenericServices {
  implicit def system: ActorSystem

  implicit def logger: LoggingAdapter

  implicit def ec: ExecutionContext

  implicit def config: Config

  implicit def mat: Materializer
}
