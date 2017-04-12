package com.groupaxis.groupsuite.routing.write.domain.model.routing.keyword

trait KeywordRequest
trait KeywordResponse

object KeywordMessages {
 
  //commands
  case class CreateKeyword(code: String, assignment: KeywordUpdate) extends KeywordRequest
  case class UpdateKeyword(code: String, assignment: KeywordUpdate) extends KeywordRequest
  case class FindKeywordByPK(name: String) extends KeywordRequest
//  case class FindAllAMHAssignments() extends KeywordRequest

  //events
  case class KeywordFound(assignment: Option[KeywordEntity]) extends KeywordResponse
  case class KeywordCreated(response : KeywordEntity) extends KeywordResponse
  case class KeywordUpdated(response : KeywordEntity) extends KeywordResponse
  case class KeywordsFound(assignments: Option[Seq[KeywordEntity]]) extends KeywordResponse

}
