package com.groupaxis.groupsuite.simulator.infrastructor.es

import akka.actor.{Actor, Props}
import com.groupaxis.groupsuite.simulator.write.domain.model.swift.msg.SwiftMessageES
import com.groupaxis.groupsuite.simulator.write.domain.model.swift.msg.SwiftMessages._
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.analyzers.CustomAnalyzer
import com.sksamuel.elastic4s.jackson.ElasticJackson
import com.sksamuel.elastic4s.{ElasticClient, _}
import org.apache.logging.log4j.scala.Logging
import org.elasticsearch.action.ActionWriteResponse
import org.elasticsearch.action.bulk.{BulkItemResponse, BulkResponse}
import org.elasticsearch.action.update.UpdateResponse
import org.elasticsearch.rest.RestStatus

import scala.collection.mutable.ListBuffer
import scala.concurrent._
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object ESSwiftMessageWriteRepository {

  final val Name = "es-swiftmessage-write-repository"

  def props(esClient: ElasticClient): Props = Props(classOf[ESSwiftMessageWriteRepository], esClient)

}

//TODO: Actually this actor should take the data from DB directly, and receive just messages with ids
//class ESSwiftMessageWriteRepository(val databaseService: DatabaseService) extends Actor with ActorLogging  {
class ESSwiftMessageWriteRepository(esClient: ElasticClient) extends Actor with Logging {

  import ElasticJackson.Implicits._

  import scala.concurrent.ExecutionContext.Implicits.global
  val singleIndexType = "messages" / "amh"
  val groupIndexType = "messages" / "group"
  val timeoutDuration = 30.minutes

  def getAllMessages: Future[Seq[SwiftMessageES]] = {


    import scala.concurrent.ExecutionContext.Implicits.global

    val res: Future[RichSearchResponse] =
        esClient execute { search in singleIndexType }
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
      index into singleIndexType source swiftMessageES id swiftMessageES.id
    }

    res onComplete {
      case Success(s) => logger.info(s" success $s")
      case Failure(t) => logger.info(s"An error has occurred: $t")
    }
    val resp = Await.result(res, timeoutDuration)

    if (resp.isCreated) Right(SwiftMessageESCreated(swiftMessageES)) else Left("No message inserted")
  }

  protected def withBulk(f: SwiftMessageES => BulkCompatibleDefinition)(entities: Seq[SwiftMessageES])(implicit ec : ExecutionContext) = {

      val future = esClient.execute(bulk(entities.map(f)))
        .flatMap(_ => Future{entities})

      future.recoverWith({ case e: Throwable => Future { Seq() } })

      future onComplete {
        case Success(s) => logger.info(s" Bulk insert succeed ")
        case Failure(t) => logger.error(s"Error on bulk index : ${t.getLocalizedMessage}")
      }

      future
    }


  private def storeAllWithIndex(as: Seq[SwiftMessageES], indexNameToInsert: IndexAndType): Future[Seq[SwiftMessageES]] = {
    val f = (message: SwiftMessageES) => index into indexNameToInsert source message id message.id
    logger.info(s"Inserting ${as.size} message(s) into ES")
    withBulk(f)(as)
  }

  def insertGroupedMessage(swiftMessageES: SwiftMessageES): Either[String, SwiftMessageESCreated] = {

    import ElasticJackson.Implicits._
//    import scala.concurrent.ExecutionContext.Implicits.global
    val singleMessage = swiftMessageES.copy(messages = Seq())
    val singleInsert = esClient.execute(index into singleIndexType source singleMessage id singleMessage.id)
    val res: Future[Seq[SwiftMessageES]] = for {
      singleInsert <- singleInsert
      groupInsert <- storeAllWithIndex(swiftMessageES.messages, groupIndexType)
    } yield {
      groupInsert
    }
//    val res: Future[IndexResult] = esClient.execute {
//      index into groupIndexType source swiftMessageES id swiftMessageES.id
//    }

    res onComplete {
      case Success(s) => logger.info(s" ${s.length} SWIFT Group Message(s) successfully inserted! ")
      case Failure(t) => logger.info(s"An error has occurred: $t")
    }
    val resp = Await.result(res, timeoutDuration)

    if (resp.nonEmpty) Right(SwiftMessageESCreated(swiftMessageES)) else Left("No message inserted")
  }

  private def updateSwiftMessage(swiftMessage: SwiftMessageES): Either[String, SwiftMessageESInserted] = {
//    import ElasticJackson.Implicits._
//    import scala.concurrent.ExecutionContext.Implicits.global

    val res = esClient.execute {
      update id swiftMessage.id in singleIndexType docAsUpsert swiftMessage
    }

    res onComplete {
      case Success(s) => logger.info(s" success $s")
      case Failure(t) => logger.info(s"An error has occurred: $t")
    }

    val updateResp: UpdateResponse = Await.result(res, timeoutDuration)

    if (swiftMessage.id.toString.equals(updateResp.getId)) Right(SwiftMessageESInserted(swiftMessage)) else Left("message was not inserted")

  }

  def deleteSwiftGroupMessage(groupId : Option[String]): Either[String, SwiftMessagesESDeleted] = {
    groupId.map( gpId => {
      //QueryStringQueryDefinition(gpId)
      val searchQ = esClient.execute { search in groupIndexType query MatchQueryDefinition("group", gpId).analyzer(CustomAnalyzer("id_analyzer")) size 100000 }
      val messagesFound = searchQ.map(msg => {
        msg.as[SwiftMessageES]
      })
      logger.info(s" looking into $groupIndexType with group == ${groupId.getOrElse("undefined")}")
      val searchByGroupResponse = Await.result( messagesFound , timeoutDuration)
      logger.info(s" ${searchByGroupResponse.length} message(s) found")
      searchByGroupResponse match {
        case empty : Array[SwiftMessageES] if empty.isEmpty =>
          Left(s"No group message(s) found with groupId $groupId")
        case nonEmptyResponse =>
          val action = for {
            singleDelete <- esClient.execute { delete id gpId from singleIndexType }
            _ <- Future{logger.info(s" successfully deleted from messages/amh/ ? ${singleDelete.isFound}")}
            bulkDelete <- {
              try {
                //            val fmr = searchResponse.as[SwiftMessageES]
                val bulkOps = nonEmptyResponse.map(msg => {
                  delete id msg.id.toLong from groupIndexType
                })
                logger.info(s"about to bulk delete of ${bulkOps.length} document(s)")
                esClient.execute(bulk(bulkOps))
              } catch {
                case e : Throwable =>
                  logger.error(s" Error while deleting from group ES ${e.getMessage}")
                  Future{BulkResult(new BulkResponse(Array(new BulkItemResponse(1,"", new BulkItemResponse.Failure("messages","group","id_value",e))),0))}
              }
            }
          } yield  bulkDelete

          val deleteResp = Await.result(action, timeoutDuration)
          if (deleteResp.hasSuccesses) Right(SwiftMessagesESDeleted(deleteResp.items.length)) else Left("No message(s) deleted")
      }

    }).getOrElse(Left(s"No message deleted for group id $groupId "))

  }

  private def isOK(response : ActionWriteResponse): Boolean = {
    val shardInfo = response.getShardInfo
    shardInfo != null && shardInfo.status() == RestStatus.OK
  }

  def deleteSwiftMessage(ids: Seq[Int]): Either[String, SwiftMessagesESDeleted] = {
    val bulkOps = new ListBuffer[BulkCompatibleDefinition]()
    try {

      val removeAll = for {
        _ <- Future {
          logger.info("items ids to delete: " + ids)
        }
        _ <- Future {
          for (itemId <- ids) yield {
            bulkOps += delete id itemId from singleIndexType
          }
        }
        _ <- Future {
          logger.info("bulk operations: " + bulkOps)
        }
        bulkResp <- esClient.execute {
          bulk(bulkOps)
        } if bulkOps.nonEmpty
      } yield bulkResp

      val deleteResp = Await.result(removeAll, timeoutDuration)

      if (deleteResp.hasSuccesses) Right(SwiftMessagesESDeleted(deleteResp.items.size)) else Left(deleteResp.failureMessage)

    } catch {
      case ex: Exception =>
        ex.printStackTrace()
        Left(ex.getMessage)
    }
  }


  def receive: Receive = {
    case InsertSwiftMessageES(swiftMessage) =>
      logger.info(s" Command to insert a single swiftMessage ${swiftMessage.id} received.")
      sender() ! insert(swiftMessage)
    case InsertSwiftGroupMessageES(swiftGroupMessage) =>
      logger.info(s" Command to insert a group swiftMessage ${swiftGroupMessage.id} received.")
      sender() ! insertGroupedMessage(swiftGroupMessage)
    case UpdateSwiftMessageES(swiftMessage) =>
      logger.info(s" Command to update swiftMessage ${swiftMessage.id} received.")
      sender() ! updateSwiftMessage(swiftMessage)
    case DeleteSwiftMessagesES(ids) =>
      logger.info(s"Command to delete ${ids.length} swift message(s) received.")
      sender() ! deleteSwiftMessage(ids)
    case DeleteSwiftGroupMessagesES(groupId) =>
      logger.info(s"Command to delete a group message $groupId received.")
      sender() ! deleteSwiftGroupMessage(groupId)
    case FindAllSwiftMessages =>
      logger.info(s"Command to Find All Swift Messages received.")
      sender() ! getAllMessages
  }

}