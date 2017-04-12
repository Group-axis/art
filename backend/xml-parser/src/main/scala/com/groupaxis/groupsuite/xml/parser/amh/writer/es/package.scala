package com.groupaxis.groupsuite.xml.parser.amh.writer

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.{BulkCompatibleDefinition, BulkResult, ElasticClient}
import org.apache.logging.log4j.scala.Logging
import org.elasticsearch.action.bulk.{BulkItemResponse, BulkResponse}

import scala.collection.mutable.ListBuffer


package object es extends Logging {

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent._
  import scala.concurrent.duration._

  def deleteType(name: String, client : ElasticClient) : Future[BulkResult] = {
    var idList = new ListBuffer[String]()
    val bulkOps = new ListBuffer[BulkCompatibleDefinition]()

    val assigns = Await.result(client.execute(search in "amhrouting" -> name size 3000), 25.seconds)
    if (assigns.getHits.getTotalHits == 0) {
      return Future { BulkResult(new BulkResponse(Array.empty[BulkItemResponse],10))}
    }


    for {
      _ <- Future {logger.debug("assigns to delete: " + assigns)}
      _ <- Future {assigns.getHits.getHits.foreach(searchHit => { idList += searchHit.getId }) }
      _ <- Future {logger.debug("assigns ids to delete: " + idList)}
      _ <- Future {
        for (assignId <- idList) yield {
          bulkOps += delete id assignId from "amhrouting" / name
        }
      }
      _ <- Future {logger.debug("bulk operations: " + bulkOps)}
      bulkResp <- client.execute { bulk(bulkOps) } if bulkOps.nonEmpty
    } yield bulkResp

  }

  def initializeIndex (indexName: String, client : ElasticClient) {
    val deleteAssignments = deleteType("assignments", client)
    val deleteRules = deleteType("rules", client)
    val deleteBackends = deleteType("backends", client)
    val deleteDistribution = deleteType("distributionCopies", client)
    val deleteFeedback = deleteType("feedbackDtnCopies", client)

    val removeAll = for {
      a  <- deleteAssignments
      b  <- deleteRules
      c  <- deleteBackends
      d  <- deleteDistribution
      e  <- deleteFeedback
    } yield e

    Await.result(removeAll, 15.seconds)

    logger.debug(s"remove and create done on index -> $indexName")
  }
}
