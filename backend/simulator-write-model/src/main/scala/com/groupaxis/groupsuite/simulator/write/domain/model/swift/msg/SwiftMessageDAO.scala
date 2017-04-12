package com.groupaxis.groupsuite.simulator.write.domain.model.swift.msg

import java.sql.Timestamp

import com.groupaxis.groupsuite.persistence.driver.DBDriver
import org.joda.time.DateTime

trait SwiftMessageDAO extends DBDriver {

  import driver.api._

  implicit def mapDate = MappedColumnType.base[Option[DateTime], Timestamp](
  d => new Timestamp(d.map(_.getMillis).getOrElse(new DateTime().getMillis)),
  time => Some(new DateTime(time.getTime))
  )

  protected class SwiftMessages(tag: Tag) extends Table[SwiftMessageEntity](tag, "sml_messages") {

    def id = column[Int]("id", O.AutoInc, O.PrimaryKey)

    def userId = column[Option[String]]("id_user")

    def creationDate = column[Option[DateTime]]("date_creation")

    def fileName = column[Option[String]]("file_name")

    def content = column[Option[String]]("file_content")

    def group = column[Option[String]]("message_group")

    private type MessageEntityTupleType = (Int, Option[String],Option[DateTime],Option[String],Option[String],Option[String] )

    private val messageShapedValue = (id, userId, creationDate, fileName, content, group).shaped[MessageEntityTupleType]

    private val toMessageRow: (MessageEntityTupleType => SwiftMessageEntity) = { messageTuple => {
      SwiftMessageEntity(messageTuple._1, messageTuple._2, messageTuple._3, messageTuple._4, messageTuple._5, messageTuple._6)
//      SwiftMessageEntity(messageTuple._1, messageTuple._2, messageTuple._2, messageTuple._4, messageTuple._5, messageTuple._6)
     }
    }

    private val toMessageTuple: (SwiftMessageEntity => Option[MessageEntityTupleType]) = { messageRow =>
      Some((messageRow.id, messageRow.userId, messageRow.creationDate, messageRow.fileName, messageRow.content, messageRow.group))
//      Some((messageRow.id, messageRow.userId, Some(new Date()), messageRow.fileName, messageRow.content, messageRow.group))
    }

    def * = messageShapedValue <> (toMessageRow, toMessageTuple)

  }

  val messages : TableQuery[SwiftMessages] = TableQuery[SwiftMessages]

}

object SwiftMessageDAO extends SwiftMessageDAO {
  def apply = SwiftMessageDAO
}


