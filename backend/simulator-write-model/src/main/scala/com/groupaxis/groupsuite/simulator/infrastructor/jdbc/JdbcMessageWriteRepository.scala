package com.groupaxis.groupsuite.simulator.infrastructor.jdbc

import java.util.concurrent.TimeoutException

import com.groupaxis.groupsuite.persistence.datastore.jdbc.Database
import com.groupaxis.groupsuite.simulator.write.domain.model.swift.msg.SwiftMessages._
import com.groupaxis.groupsuite.simulator.write.domain.model.swift.msg.{SwiftMessageDAO, SwiftMessageEntity, SwiftMessageEntityUpdate, SwiftMessageWriteRepository}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.Await
import scala.concurrent.duration.FiniteDuration


object JdbcMessageWriteRepository extends Logging {
}

class JdbcMessageWriteRepository(dao: SwiftMessageDAO, database: Database, timeout: FiniteDuration) extends SwiftMessageWriteRepository with Logging {

  import database._
  import slick.driver.PostgresDriver.api._
  import scala.concurrent.ExecutionContext.Implicits.global

  def getMessages : Either[String, SwiftMessagesFound] = {
    try {
      val result = Await.result(db.run(dao.messages.result), timeout)
      Right(SwiftMessagesFound(result))
    } catch {
      case timeEx: TimeoutException => Left(s" Error while looking for all messages DB not responding")
      case e: Exception             => Left(s" Error while looking for all messages msg ${e.getMessage}")
    }
  }

  def getMessageById(id: Int): Either[String, SwiftMessageFound] = {
    try {
      val result: Option[SwiftMessageEntity] = Await.result(db.run(dao.messages.filter(_.id === id).result), timeout).headOption
      Right(SwiftMessageFound(result))
    } catch {
      case timeEx: TimeoutException => Left(s" Error while looking for a USER $id DB not responding")
      case e: Exception             => Left(s" Error while looking for a USER $id msg ${e.getMessage}")
    }
  }

  def createMessage(swiftMsg: SwiftMessageEntity): Either[String, SwiftMessageCreated] = {
    try {
      Await.result(db.run(dao.messages returning dao.messages += swiftMsg).map { swiftMsg => Right(SwiftMessageCreated(swiftMsg)) }, timeout)
    } catch {
      case timeEx: TimeoutException => Left(s" Error while creating message DB not responding: : ${timeEx.getMessage}")
      case e: Exception             => Left(s" Error while creating message msg: ${e.getMessage}")
    }
  }
  def createMessages(swiftMsg: Seq[SwiftMessageEntity]): Either[String, SwiftMessagesCreated] = {
    try {
      Await.result(db.run(dao.messages returning dao.messages ++= swiftMsg).map { swiftMsg => Right(SwiftMessagesCreated(swiftMsg)) }, timeout)
    } catch {
      case timeEx: TimeoutException => Left(s" Error while creating message DB not responding: : ${timeEx.getMessage}")
      case e: Exception             => Left(s" Error while creating message msg: ${e.getMessage}")
    }
  }

  def updateMessage(id: Int, messageUpdate: SwiftMessageEntityUpdate): Either[String, SwiftMessageUpdated] = {
    val eitherResponse = getMessageById(id)

    if (eitherResponse.isLeft) {
      Left(eitherResponse.left.get)
    } else {
      val messageFound = eitherResponse.right.get
      messageFound.swiftMsg match {
        case Some(swiftMsg) =>
          try {
            val updatedMessage = messageUpdate.merge(swiftMsg)
            Await.result(db.run(dao.messages.filter(_.id === id).update(updatedMessage)), timeout)
            Right(SwiftMessageUpdated(updatedMessage))
          } catch {
            case timeEx: TimeoutException => Left(s" Error while updating message DB not responding")
            case e: Exception             => Left(s" Error while updating message msg: ${e.getMessage}")
          }
        case None => Left("")
      }
    }
  }

  def deleteMessages(ids: Seq[Int], groupId : Option[String]): Either[String, Int] = {
   try {
     val query =
       groupId//.map(gpId => gpId.substring(0, gpId.lastIndexOf("_")))
       .map(byGroupId)
       .getOrElse(byIds(ids))

     logger.debug(s"Trying to delete by group: $groupId or single $ids")
     //dao.messages.filter(msg => if (ids.indexOf(msg.id.to) >= 0
     val result = Await.result(db.run(query.delete), timeout)
     if (result == 0) Left("No messages deleted from DB") else Right(result)
   } catch {
     case timeEx: TimeoutException => Left(s" Error while deleting message ids $ids DB not responding")
     case e: Exception             => Left(s" Error while deleting message ids $ids msg: ${e.getMessage}")
   }
  }

  private def byIds(ids: Seq[Int]) =
    for {
      msg <- dao.messages
      if msg.id inSetBind ids
    } yield msg

  private def byGroupId(group: String) = dao.messages.filter(_.group === group)



}