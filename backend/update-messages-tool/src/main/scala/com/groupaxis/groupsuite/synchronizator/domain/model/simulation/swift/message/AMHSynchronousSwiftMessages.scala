package com.groupaxis.groupsuite.synchronizator.domain.model.simulation.swift.message

import com.groupaxis.groupsuite.persistence.datastore.jdbc.Database
import com.groupaxis.groupsuite.simulation.util.MessageUtil
import com.groupaxis.groupsuite.simulator.write.domain.model.mapping.MappingEntity
import com.groupaxis.groupsuite.simulator.write.domain.model.swift.msg.{SwiftMessageES, SwiftMessageEntity}
import com.groupaxis.groupsuite.synchronizator.domain.model.{AMHESRepository, AMHRepository, AMHService}
import com.groupaxis.groupsuite.synchronizator.file.GPFileHelper
import com.groupaxis.groupsuite.synchronizator.parser.GPParserHelper
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.jackson.ElasticJackson
import org.apache.logging.log4j.scala.Logging
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}

trait AMHSwiftMessageRepositoryT extends AMHRepository[SwiftMessageEntity, String] {
  def allGroupedMessages: Future[Map[String, Seq[SwiftMessageEntity]]]

  def allSingleMessages: Future[Seq[SwiftMessageEntity]]
}

class AMHSwiftMessageRepository(val db: Database) extends AMHSwiftMessageRepositoryT {


  import com.groupaxis.groupsuite.simulator.write.domain.model.swift.msg.SwiftMessageDAO._
  import driver.api._

  import scala.concurrent.ExecutionContext.Implicits.global

  override def allGroupedMessages: Future[Map[String, Seq[SwiftMessageEntity]]] = {
    db.db.run(messages.filter(!_.group.isEmpty).result)
      .map(_.foldLeft(Map[String, Seq[SwiftMessageEntity]]()) {
        (a, v) => {
          val groupName = v.group.getOrElse("")
          val groupMessages = a.getOrElse(groupName, Seq[SwiftMessageEntity]())
          val updatedGroupMessages = groupMessages :+ v
          a + (groupName -> updatedGroupMessages)
        }
      }
      )
  }

  override def allSingleMessages: Future[Seq[SwiftMessageEntity]] = {
    db.db.run(messages.filter(_.group.isEmpty).result)
  }

  override def query(id: String): Future[Option[SwiftMessageEntity]] = {
    db.db.run(messages.filter(_.id === id.toInt).result.headOption)
  }

  override def store(message: SwiftMessageEntity): Future[SwiftMessageEntity] = {
    db.db.run(messages returning messages += message)
  }

  override def all: Future[Seq[SwiftMessageEntity]] =
    db.db.run(messages.result)


  override def storeAll(newMessages: Seq[SwiftMessageEntity]): Future[Seq[SwiftMessageEntity]] =
    db.db.run(messages returning messages ++= newMessages)
}

object AMHSwiftMessageRepository {
  def apply(db: Database) = {
    new AMHSwiftMessageRepository(db)
  }
}

trait AMHSwiftMessageESRepositoryT extends AMHESRepository[SwiftMessageES] {

  implicit override def indexName: String = "messages/amh"

}

class AMHSwiftMessageESRepository(val client: ElasticClient) extends AMHSwiftMessageESRepositoryT {

  import ElasticJackson.Implicits._
  import com.sksamuel.elastic4s.ElasticDsl._

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit override def esClient = client

  override def query(id: String): Future[Option[SwiftMessageES]] = Future {
    None
  }

  override def store(a: SwiftMessageES): Future[SwiftMessageES] = Future {
    a
  }

  override def all: Future[Seq[SwiftMessageES]] = Future {
    Seq()
  }

  override def storeAllInIndex(as: Seq[SwiftMessageES], indexNameToInsert: String): Future[Seq[SwiftMessageES]] = {
    storeAllWithIndex(as, indexNameToInsert)
  }

  override def storeAll(as: Seq[SwiftMessageES]): Future[Seq[SwiftMessageES]] = {
    storeAllWithIndex(as, indexName)
  }

  private def storeAllWithIndex(as: Seq[SwiftMessageES], indexNameToInsert: String): Future[Seq[SwiftMessageES]] = {
    val f = (message: SwiftMessageES) => index into indexNameToInsert source message id message.id
    logger.debug(s"Indexing into ES ${as.size} record(s) ")
    withBulk(f)(as)
  }

}

object AMHSwiftMessageESRepository {
  def apply(esClient: ElasticClient) = {
    new AMHSwiftMessageESRepository(esClient)
  }
}

sealed trait AMHSynchronousSwiftMessages {
  type AMHSwiftMessages = Seq[SwiftMessageEntity]
  type AMHSwiftMessagesES = Seq[SwiftMessageES]
}

object AMHSynchronousSwiftMessages extends AMHSynchronousSwiftMessages with AMHService[SwiftMessageEntity, String, SwiftMessageES] with Logging {

  def synchronize(mappingsFromDB: Seq[MappingEntity])(implicit ec: ExecutionContext): (AMHRepository[SwiftMessageEntity, String], AMHESRepository[SwiftMessageES]) => Future[AMHSwiftMessagesES]
  = (repo, repoES) => {
    val singleMessageToESMapper = singleMessagesToES(getItemMap(mappingsFromDB)) _
    val groupedMessageToESMapper = groupedMessagesToES(getItemMap(mappingsFromDB)) _

    val getAllSingleMessages = repo.asInstanceOf[AMHSwiftMessageRepositoryT].allSingleMessages
    val getAllGroupedMessages = repo.asInstanceOf[AMHSwiftMessageRepositoryT].allGroupedMessages

    for {
      singleInsertedMessages <- insertSingleMessagesIntoES(getAllSingleMessages, singleMessageToESMapper).apply(repo, repoES)
      groupedInsertedMessages <- insertGroupMessagesIntoES(getAllGroupedMessages, groupedMessageToESMapper).apply(repo, repoES)
    } yield singleInsertedMessages ++ groupedInsertedMessages

  }

  def getItemMap(mappingsFromDB: Seq[MappingEntity])(content: Option[String]): String = {
    val msgType = GPFileHelper.findMsgType(content)
    val mappings = MessageUtil.getMappings(msgType, Right(mappingsFromDB))
    GPParserHelper.findMatches2(msgType, content, mappings)
  }

  override def updateIntoES(toES: AMHSwiftMessages => Future[AMHSwiftMessagesES])(implicit ec: ExecutionContext): (AMHRepository[SwiftMessageEntity, String], AMHRepository[SwiftMessageES, String]) => Future[AMHSwiftMessagesES]
  = (repo, repoES) => {
    Future {
      Seq()
    }
  }


  def insertGroupMessagesIntoES(f: => Future[Map[String, AMHSwiftMessages]], toES: Map[String, AMHSwiftMessages] => Seq[SwiftMessageES])(implicit ec: ExecutionContext): (AMHRepository[SwiftMessageEntity, String], AMHESRepository[SwiftMessageES]) => Future[AMHSwiftMessagesES]
  = {
    def toSingleESMessages(entities: Map[String, AMHSwiftMessages]): Future[Seq[SwiftMessageES]] =
      Future {
        entities.map(pair => SwiftMessageES(pair._1, None, None, Some(pair._1), Some(pair._1), Some(s"The file contains ${pair._2.length} message(s)"), "", Seq(),pair._2.length))
          .toSeq
      }
    (repo, repoES) => {

      val updatedEntities = for {
        allEntities <- f
        entities <- if (allEntities.nonEmpty) Future.successful(allEntities)
        else Future.successful(Map[String, AMHSwiftMessages]())
        entitiesConvertedToES <- Future {
          toES(entities)
        }
        entitiesES <- if (entitiesConvertedToES.nonEmpty) Future.successful(entitiesConvertedToES)
        else Future.successful(List[SwiftMessageES]())
        singleESEntities <- toSingleESMessages(entities)
        updatedEntities <- {
          if (entitiesES.nonEmpty) repoES.storeAllInIndex(entitiesES, "messages/group")
          else Future.successful(entitiesES)
        }
        updatedSingleEntities <- {
          if (singleESEntities.nonEmpty) repoES.storeAll(singleESEntities)
          else Future.successful(singleESEntities)
        }
      } yield updatedEntities
      logger.debug(s" JUST AFTER $updatedEntities ")
      updatedEntities
    }
  }

  def insertSingleMessagesIntoES(f: => Future[AMHSwiftMessages], toES: AMHSwiftMessages => Seq[SwiftMessageES])(implicit ec: ExecutionContext): (AMHRepository[SwiftMessageEntity, String], AMHRepository[SwiftMessageES, String]) => Future[AMHSwiftMessagesES]
  = (repo, repoES) => {
    val updatedEntities = for {
      allEntities <- f
      entities <- if (allEntities.nonEmpty) Future.successful(allEntities)
      else Future.successful(Seq[SwiftMessageEntity]())
      entitiesConvertedToES <- Future {
        toES(entities)
      }
      entitiesES <- if (entitiesConvertedToES.nonEmpty) Future.successful(entitiesConvertedToES)
      else Future.successful(Seq[SwiftMessageES]())
      updatedEntities <- {
        if (entitiesES.nonEmpty) repoES.storeAll(entitiesES)
        else Future.successful(entitiesES)
      }
    } yield updatedEntities
    logger.debug(s" JUST AFTER $updatedEntities ")
    updatedEntities
  }

  private def singleMessagesToES(getItemMap: Option[String] => String)(messages: AMHSwiftMessages)(implicit ec: ExecutionContext): AMHSwiftMessagesES =
    messages.map(message => message.toES(getItemMap(message.content), Seq()))

//  private def groupedMessageOld(groupName: String, message: Option[SwiftMessageEntity]): SwiftMessageEntity = {
//    message.map(message =>
//      message.copy(fileName = Some(groupName), group = Some(groupName), content = Some(""))
//    ).getOrElse(SwiftMessageEntity(-1, Some("not_defined_user"), Some(DateTime.now), Some(groupName), Some(""), Some(groupName)))
//  }

  private def groupedMessage(groupName: String, message: Option[SwiftMessageEntity]): SwiftMessageEntity = {
    message.map(message =>
      message.copy(group = Some(groupName))
    ).getOrElse(SwiftMessageEntity(-1, Some("not_defined_user"), Some(DateTime.now), Some(groupName), Some(""), Some(groupName)))
  }

//  private def groupedMessagesToESOld(getItemMap: Option[String] => String)(messages: Map[String, AMHSwiftMessages])(implicit ec: ExecutionContext): AMHSwiftMessagesES = {
//    messages
//      .map(pair => groupedMessage(pair._1, pair._2.headOption) -> singleMessagesToES(getItemMap)(pair._2))
//      .map(pair => pair._1.toES(getItemMap(pair._1.content), pair._2).withGroupedId)
//      .map(message => message.copy(itemMap = "", content = Some("The file contains " + message.messages.size + " message(s)")))
//      .toSeq
//  }

  private def groupMessagesToGroupES(getItemMap: Option[String] => String, messages: AMHSwiftMessages) =
  {
    messages.map(message => message.toGroupES(getItemMap(message.content)))

  }

  private def groupedMessagesToES(getItemMap: Option[String] => String)(messages: Map[String, AMHSwiftMessages])(implicit ec: ExecutionContext): AMHSwiftMessagesES = {
    messages
      .flatMap(pair => groupMessagesToGroupES(getItemMap,pair._2.map(msg => groupedMessage(pair._1, Some(msg)))))
      .toSeq
  }

}
