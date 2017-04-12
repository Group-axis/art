package com.groupaxis.groupsuite.routing.write.domain.model.routing.exit.point

trait ExitPointRequest
trait ExitPointResponse

object KeywordMessages {

  //commands
  case class CreateExitPoint(name: String, exitPoint: ExitPointUpdate) extends ExitPointRequest
  case class UpdateExitPoint(name: String, exitPoint: ExitPointUpdate) extends ExitPointRequest
  case class FindExitPointByPK(name: String) extends ExitPointRequest
//  case class FindAllAMHAssignments() extends KeywordRequest

  //events
  case class ExitPointFound(exitPoint: Option[ExitPoint]) extends ExitPointResponse
  case class ExitPointCreated(exitPoint : ExitPoint) extends ExitPointResponse
  case class ExitPointUpdated(exitPoint : ExitPoint) extends ExitPointResponse
  case class ExitPointsFound(exitPoints: Option[Seq[ExitPoint]]) extends ExitPointResponse

}
