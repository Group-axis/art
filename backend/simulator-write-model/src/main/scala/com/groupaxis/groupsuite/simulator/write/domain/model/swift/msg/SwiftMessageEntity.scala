package com.groupaxis.groupsuite.simulator.write.domain.model.swift.msg

import org.joda.time.DateTime

/********  MESSAGE *************/
case class SwiftMessageEntity(id : Int, userId : Option[String], creationDate: Option[DateTime], fileName : Option[String], content : Option[String], group : Option[String], messageType : Int = 0) {

  def toES(itemMap : String, messages : Seq[SwiftMessageES] = Seq()) = SwiftMessageES(id.toString, userId, creationDate.map(_.getMillis), fileName, group, content, itemMap, messages, messages.length)
  def toGroupES(itemMap : String, messages : Seq[SwiftMessageES] = Seq()) = SwiftMessageES(id.toString, None, None, fileName, group, None, itemMap, messages, messages.length)

}

case class SwiftMessageEntityUpdate(userId : Option[String] = None, creationDate: Option[DateTime] = None, fileName : Option[String] = None, content : Option[String] = None, group : Option[String] = None) {

  def merge(toto: SwiftMessageEntity): SwiftMessageEntity = {
    SwiftMessageEntity(toto.id, userId.orElse(toto.userId), creationDate.orElse(toto.creationDate), fileName.orElse(toto.fileName), content.orElse(toto.content), group.orElse(toto.group))
  }

  def merge(id: Int): SwiftMessageEntity = {
    SwiftMessageEntity(id, userId, creationDate, fileName, content, group)
  }

  def toMessageEntity : SwiftMessageEntity = {
    SwiftMessageEntity(-1, userId, creationDate, fileName, content, group)
  }
}

case class SwiftMessageES(id : String, userId: Option[String], creationDate: Option[Long], name : Option[String], group : Option[String], content : Option[String], itemMap : String, messages : Seq[SwiftMessageES], groupCount : Int) {
  def withGroupedId = this.copy(id = group.getOrElse(id))

}
