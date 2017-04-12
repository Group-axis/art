package com.groupaxis.groupsuite.routing.write.domain.global.messages

import org.joda.time.DateTime

object AMHRoutingGlobalMessages {
  trait AMHGlobalRequest
  trait AMHGlobalResponse

  //commands
   case class CreateOverviewCSVFile(assignmentType: Int, username : Option[String], date : Option[DateTime]) extends AMHGlobalRequest

  //events
  case class OverviewCSVFileCreated(fileName : Option[String]) extends AMHGlobalResponse

}
