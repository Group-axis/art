package com.groupaxis.groupsuite.routing.interfaces.http

import java.text.SimpleDateFormat

import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s.ext.JodaTimeSerializers
import org.json4s.{native, DefaultFormats, Formats}
import de.heikoseeberger.akkahttpcirce.CirceSupport
import de.heikoseeberger.akkahttpcirce.CirceSupport
trait JsonSupport  extends CirceSupport{ //

  implicit val serialization = native.Serialization

  implicit def json4sFormats: Formats = customDateFormat ++ JodaTimeSerializers.all ++ CustomSerializers.all

  val customDateFormat = new DefaultFormats {
    override def dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
  }
  
}