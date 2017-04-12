package com.groupaxis.groupsuite.synchronizator.date

import org.joda.time.{DateTime, DateTimeZone}


object GPDateHelper {
  import java.text.SimpleDateFormat
  val minuteFormat = new SimpleDateFormat("yyyyMMddHHmmss")

  def currentDate : String = {

    import java.util.Calendar
    val today = Calendar.getInstance().getTime
    minuteFormat.format(today)
  }

  def todayMillis : String = {
    import java.util.Calendar
    val today = Calendar.getInstance()
    today.getTimeInMillis.toString
  }

  def mapToDateTime(value : Option[String]) : Option[org.joda.time.DateTime] = {
    value.map(date =>
      //new DateTime(date.toLong, DateTimeZone.UTC)
        new DateTime(date.toLong) )
      //.getOrElse(new DateTime())
  }
}
