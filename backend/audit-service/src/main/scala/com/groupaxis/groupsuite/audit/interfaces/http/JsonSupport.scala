package com.groupaxis.groupsuite.audit.interfaces.http

import java.text.SimpleDateFormat
import java.time.format.{DateTimeFormatter, DateTimeParseException}

import org.json4s.ext.JodaTimeSerializers
import org.json4s.{DefaultFormats, Formats, native}
import de.heikoseeberger.akkahttpcirce.CirceSupport
import io.circe.{Decoder, Encoder}
import cats.data.Xor
import io.circe._

trait JsonSupport extends CirceSupport { //

  implicit val serialization = native.Serialization

  implicit def json4sFormats: Formats = customDateFormat ++ JodaTimeSerializers.all ++ CustomSerializers.all

  val customDateFormat = new DefaultFormats {
    override def dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
  }

  implicit val dateTimeEncoder : Encoder[org.joda.time.DateTime] =
    Encoder.instance[org.joda.time.DateTime]( x =>  Json.fromString( x.getMillis.toString) )

  implicit val dateTimeDecoder : Decoder[org.joda.time.DateTime] =
    Decoder.instance[org.joda.time.DateTime] (
      c =>
        c.focus.asString match {
          case None       => Xor.left(DecodingFailure("DataTime", c.history))
          case Some(date) =>
            tryParser(date, DecodingFailure("DateTime", c.history))
        }
    )
  private def tryParser(input: String, error: DecodingFailure): Xor[DecodingFailure, org.joda.time.DateTime] = {
    try {
//      val fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME
      val nst = new org.joda.time.DateTime(input.toLong)
      Xor.right(nst)
    }
    catch {
      case t: DateTimeParseException =>
        Xor.left(error)
    }
  }
}