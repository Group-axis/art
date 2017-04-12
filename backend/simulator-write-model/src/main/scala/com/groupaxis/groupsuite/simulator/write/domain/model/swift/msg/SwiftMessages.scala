package com.groupaxis.groupsuite.simulator.write.domain.model.swift.msg

trait SwiftMessageESRequest
trait SwiftMessageESResponse
trait SwiftMessageRequest
trait SwiftMessageResponse

object SwiftMessages {

  //commands
  case class CreateSwiftMessage(swiftMsg : SwiftMessageEntityUpdate) extends SwiftMessageRequest
  case class UpdateSwiftMessage(id : Int, swiftMsg : SwiftMessageEntityUpdate) extends SwiftMessageRequest
  case class FindAllSwiftMessages() extends SwiftMessageRequest
  case class FindSwiftMessageById(id : Int) extends SwiftMessageRequest
  case class DeleteSwiftMessagesByIds(ids : Seq[Int], userId : Option[String], group: Option[String], time : Option[String]) extends SwiftMessageRequest
  case class InsertSwiftMessageES(swiftMsg: SwiftMessageES) extends SwiftMessageESRequest
  case class InsertSwiftGroupMessageES(swiftMsg: SwiftMessageES) extends SwiftMessageESRequest
  case class UpdateSwiftMessageES(swiftMsg: SwiftMessageES) extends SwiftMessageESRequest
  case class DeleteSwiftMessagesES(ids: Seq[Int]) extends SwiftMessageESRequest
  case class DeleteSwiftGroupMessagesES(groupId : Option[String]) extends SwiftMessageESRequest

  //events
  case class SwiftMessageFound(swiftMsg : Option[SwiftMessageEntity]) extends SwiftMessageResponse
  case class SwiftMessageCreated(swiftMsg : SwiftMessageEntity) extends SwiftMessageResponse
  case class SwiftMessagesCreated(swiftMsg : Seq[SwiftMessageEntity]) extends SwiftMessageResponse
  case class SwiftMessageUpdated(swiftMsg : SwiftMessageEntity) extends SwiftMessageResponse
  case class SwiftMessagesDeleted(count : Int) extends SwiftMessageResponse
  case class SwiftMessagesFound(swiftMessages : Seq[SwiftMessageEntity]) extends SwiftMessageResponse
  case class SwiftMessageESCreated(swiftMsg : SwiftMessageES) extends SwiftMessageESResponse
  case class SwiftMessageESInserted(swiftMsg : SwiftMessageES) extends SwiftMessageESResponse
  case class SwiftMessagesESDeleted(count: Int) extends SwiftMessageESResponse

}
