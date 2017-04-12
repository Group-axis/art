package com.groupaxis.groupsuite.synchronizator.system

import org.apache.logging.log4j.scala.Logging

import scala.collection.mutable.StringBuilder
import sys.process._

object GPSystemHelper {

  def execute(command: String): (String, StringBuilder, StringBuilder)  = {
    val out = new StringBuilder
    val err = new StringBuilder

    val logger = ProcessLogger(
      (o: String) => out.append(o),
      (e: String) => err.append(e))

    val resp = command !! logger

    (resp, out, err)
  }

}

object main extends App with Logging {
  logger.debug(s" exe: ${GPSystemHelper.execute("cmd /c dir")}" )
}

