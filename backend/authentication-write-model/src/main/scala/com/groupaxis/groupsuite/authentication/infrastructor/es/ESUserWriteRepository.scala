package com.groupaxis.groupsuite.authentication.infrastructor.es

import akka.actor.{Actor, Props}
import com.groupaxis.groupsuite.authentication.write.domain.model.user.UserEntityES
import com.groupaxis.groupsuite.authentication.write.domain.model.user.UserMessages.{InsertUserES, UpdateUserES, UserESInserted, UserESUpdated}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.jackson.ElasticJackson
import com.sksamuel.elastic4s.{ElasticClient, _}
import org.apache.logging.log4j.scala.Logging
import org.elasticsearch.action.bulk.{BulkItemResponse, BulkResponse}
import org.elasticsearch.action.update.UpdateResponse

import scala.collection.mutable.ListBuffer
import scala.concurrent._
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object ESUserWriteRepository {

  final val Name = "es-user-write-repository"

  def props(esClient : ElasticClient): Props = Props(classOf[ESUserWriteRepository], esClient)

}

//TODO: Actually this actor should take the data from DB directly, and receive just messages with ids
//class ESUserWriteRepository(val databaseService: DatabaseService) extends Actor with ActorLogging  {
class ESUserWriteRepository(esClient : ElasticClient) extends Actor with Logging  {

  import scala.concurrent.ExecutionContext.Implicits.global

  def insert(userES: UserEntityES): Int = {

    val res = esClient.execute {
      //authentication/routingusers/_search
      search in "authentication" -> "routingusers" query {
        termQuery("_id", userES.id)
      }
    }

    12
  }




  private def updateUser(user: UserEntityES): UpdateResponse = {
    import ElasticJackson.Implicits._

    import scala.concurrent.ExecutionContext.Implicits.global

    val res = esClient.execute {
      update id user.id in "authentication/routingusers" docAsUpsert user
    }

    res onComplete {
      case Success(s) => logger.debug(s" success $s")
      case Failure(t) => logger.debug(s"An error has occurred: $t")
    }

    Await.result(res, 15.seconds)

  }


  def deleteRoutingType(name: String) : Future[BulkResult] = {
    var idList = new ListBuffer[String]()
    val bulkOps = new ListBuffer[BulkCompatibleDefinition]()
  try {
    val items = Await.result(esClient.execute(search in "routing" -> name size 10000), 50.seconds)
    if (items.getHits.getTotalHits == 0) {
      return Future {
        BulkResult(new BulkResponse(Array.empty[BulkItemResponse], 10))
      }
    }


    for {
      _ <- Future {
        logger.debug("items to delete: " + items)
      }
      _ <- Future {
        items.getHits.getHits.foreach(searchHit => {
          idList += searchHit.getId
        })
      }
      _ <- Future {
        logger.debug("items ids to delete: " + idList)
      }
      _ <- Future {
        for (itemId <- idList) yield {
          bulkOps += delete id itemId from "routing" / name
        }
      }
      _ <- Future {
        logger.debug("bulk operations: " + bulkOps)
      }
      bulkResp <- esClient.execute {
        bulk(bulkOps)
      } if bulkOps.nonEmpty
    } yield bulkResp
  } catch {
    case ex : Exception =>
      ex.printStackTrace()
      Future {
        BulkResult(new BulkResponse(Array.empty[BulkItemResponse], 10))
      }
  }
  }



  def receive : Receive = {
    case InsertUserES(user) =>
      logger.debug(s" inserting into users $user.id ")
      insert(user)
      sender() ! UserESInserted(user)
    case UpdateUserES(user) =>
      logger.debug(s" updating into point $user")
      updateUser(user)
      sender() ! UserESUpdated(user)
  }

}