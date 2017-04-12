package com.groupaxis.groupsuite.simulator.write.domain.model.swift.msg

import com.groupaxis.groupsuite.simulator.write.domain.model.swift.msg.SwiftMessages.{SwiftMessageCreated, SwiftMessageFound, SwiftMessageUpdated, SwiftMessagesFound}

trait SwiftMessageWriteRepository {

  def getMessages: Either[String, SwiftMessagesFound]

  def getMessageById(id : Int): Either[String, SwiftMessageFound]

  def createMessage(user: SwiftMessageEntity): Either[String, SwiftMessageCreated]

  def updateMessage(id: Int, userUpdate: SwiftMessageEntityUpdate): Either[String, SwiftMessageUpdated]

  def deleteMessages(id: Seq[Int], groupId : Option[String]): Either[String, Int]

}