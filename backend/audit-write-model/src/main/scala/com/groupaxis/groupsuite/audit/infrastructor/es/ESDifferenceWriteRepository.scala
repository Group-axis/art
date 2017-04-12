package com.groupaxis.groupsuite.audit.infrastructor.es

import akka.actor.{Actor, Props}
import com.groupaxis.groupsuite.simulator.write.domain.model.swift.msg.SwiftMessageES
import com.groupaxis.groupsuite.simulator.write.domain.model.swift.msg.SwiftMessages._
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.jackson.ElasticJackson
import com.sksamuel.elastic4s.{ElasticClient, _}
import org.apache.logging.log4j.scala.Logging
import org.elasticsearch.action.update.UpdateResponse

import scala.collection.mutable.ListBuffer
import scala.concurrent._
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object ESDifferenceWriteRepository {

  final val Name = "es-difference-write-repository"

  def props(esClient: ElasticClient): Props = Props(classOf[ESDifferenceWriteRepository], esClient)

}

//TODO: Actually this actor should take the data from DB directly, and receive just messages with ids
//class ESSwiftMessageWriteRepository(val databaseService: DatabaseService) extends Actor with ActorLogging  {
class ESDifferenceWriteRepository(esClient: ElasticClient) extends Actor with Logging {

  import scala.concurrent.ExecutionContext.Implicits.global

  def getAllMessages: Future[Seq[SwiftMessageES]] = {

    import ElasticJackson.Implicits._

    import scala.concurrent.ExecutionContext.Implicits.global

    val res: Future[RichSearchResponse] =
        esClient execute { search in "messages" / "amh" }
    logger.info("launching the search....")
    res.map(msg => {
      logger.info("mapping $msg")
      msg.as[SwiftMessageES]
    })
//    res onComplete {
//      case Success(s) => s.as[SwiftMessageES]
//      case Failure(t) => logger.info(s"An error has occured: $t")
//    }
//    val resp = Await.result(res, 10.seconds)

//    if (resp.isCreated) Right(SwiftMessageESCreated(swiftMessageES)) else Left("No message inserted")
  }

  def insert(swiftMessageES: SwiftMessageES): Either[String, SwiftMessageESCreated] = {

    import ElasticJackson.Implicits._

    import scala.concurrent.ExecutionContext.Implicits.global

    val res: Future[IndexResult] = esClient.execute {
      index into "messages/amh" source swiftMessageES id swiftMessageES.id
    }

    res onComplete {
      case Success(s) => logger.info(s" success $s")
      case Failure(t) => logger.info(s"An error has occured: $t")
    }
    val resp = Await.result(res, 10.seconds)

    if (resp.isCreated) Right(SwiftMessageESCreated(swiftMessageES)) else Left("No message inserted")
  }



  private def updateSwiftMessage(swiftMessage: SwiftMessageES): Either[String, SwiftMessageESInserted] = {
    import ElasticJackson.Implicits._

    import scala.concurrent.ExecutionContext.Implicits.global

    val res = esClient.execute {
      update id swiftMessage.id in "messages/amh" docAsUpsert swiftMessage
    }

    res onComplete {
      case Success(s) => logger.info(s" success $s")
      case Failure(t) => logger.info(s"An error has occurred: $t")
    }

    val updateResp: UpdateResponse = Await.result(res, 15.seconds)

    if (swiftMessage.id.toString.equals(updateResp.getId)) Right(SwiftMessageESInserted(swiftMessage)) else Left("message was not inserted")

  }

  def deleteSwiftMessage(groupId : Option[String]): Either[String, SwiftMessagesESDeleted] = {
    groupId.map( gpId => {
      val deleteResp = Await.result(esClient.execute { delete id gpId from "messages" / "amh" }, 15.seconds)
      if (deleteResp.isFound) Right(SwiftMessagesESDeleted(1)) else Left("No message deleted")
    }).getOrElse(Right(SwiftMessagesESDeleted(0)))

  }

//  private def isOK(response : ActionWriteResponse): Boolean = {
//    val shardInfo = response.getShardInfo
//    shardInfo != null && shardInfo.status() == RestStatus.OK
//  }

  def deleteSwiftMessage(ids: Seq[Int]): Either[String, SwiftMessagesESDeleted] = {
    val bulkOps = new ListBuffer[BulkCompatibleDefinition]()
    try {

      val removeAll = for {
        _ <- Future {
          logger.info("items ids to delete: " + ids)
        }
        _ <- Future {
          for (itemId <- ids) yield {
            bulkOps += delete id itemId from "messages" / "amh"
          }
        }
        _ <- Future {
          logger.info("bulk operations: " + bulkOps)
        }
        bulkResp <- esClient.execute {
          bulk(bulkOps)
        } if bulkOps.nonEmpty
      } yield bulkResp

      val deleteResp = Await.result(removeAll, 15.seconds)

      if (deleteResp.hasSuccesses) Right(SwiftMessagesESDeleted(deleteResp.items.size)) else Left(deleteResp.failureMessage)

    } catch {
      case ex: Exception =>
        ex.printStackTrace()
        Left(ex.getMessage)
    }
  }


  def receive: Receive = {
    case InsertSwiftMessageES(swiftMessage) =>
      logger.info(s" inserting into swiftMessage $swiftMessage.id ")
      sender() ! insert(swiftMessage)
    case UpdateSwiftMessageES(swiftMessage) =>
      logger.info(s" updating swiftMessage $swiftMessage")
      sender() ! updateSwiftMessage(swiftMessage)
    case DeleteSwiftMessagesES(ids) =>
      logger.info(s"Message DeleteSwiftMessagesES($ids) received")
      sender() ! deleteSwiftMessage(ids)
    case DeleteSwiftGroupMessagesES(groupId) =>
      logger.info(s"Message DeleteSwiftGroupMessagesES($groupId) received")
      sender() ! deleteSwiftMessage(groupId)
    case FindAllSwiftMessages =>
      logger.info(s"Message FindAllSwiftMessages received")
      sender() ! getAllMessages
  }

}