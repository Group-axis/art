package com.groupaxis.groupsuite.xml.parser.amh.writer.es

import com.groupaxis.groupsuite.routing.write.domain.model.amh.backend.AMHBackendEntityES
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s._
import com.sksamuel.elastic4s.jackson.ElasticJackson
import org.apache.logging.log4j.scala.Logging
import org.elasticsearch.action.bulk.{BulkItemResponse, BulkResponse}

import scala.collection.mutable.ListBuffer
import scala.concurrent._
import scala.concurrent.duration._
import scala.util.{Failure, Success}

class ESAMHBackendRepository(client : ElasticClient) extends Logging  {

  import scala.concurrent.ExecutionContext.Implicits.global

//  private def createIndexV1(indexName: String): Future[CreateIndexResponse] = {
//    client.execute {
//      create index indexName
//    }
//  }

  /*
"rules": {
            "properties": {
              "code": {
                "type": "string"
              },
              "dataOwner": {
                "type": "string"
              },
              "expression": {
                "type": "string"
              },
              "lockCode": {
                "type": "string"
              },
              "sequence": {
                "type": "long"
              }
            }
          },
          "sequence": {
            "type": "long"
          }

          "analysis" : {
        "filter" : {
          "autocomplete_filter" : {
            "type" : "nGram",
            "min_gram" : 2,
            "max_gram" : 20
          }
        },
		"analyzer" : {
          "autocomplete": {
            "type": "custom",
            "tokenizer": "whitespace",
            "filter": [
			         "lowercase",
               "autocomplete_filter"
            ]
          }
        }
      }
* */
//  private def createIndex(indexName: String): Future[CreateIndexResponse] = {
//    client.execute {
//      create index indexName mappings {
//        "assignments" fields(
//          "active"
//            typed BooleanType
//            store true,
//          "backendPrimaryKey" typed StringType fields(
//            "code" typed StringType store true,
//            "direction" typed StringType store true
//            ),
//          "code"
//            typed StringType
//            analyzer CustomAnalyzer("autocomplete")
//            store true
//          )
//      } analysis
//        CustomAnalyzerDefinition(
//          "autocomplete",
//          WhitespaceTokenizer,
//          LowercaseTokenFilter,
//          NGramTokenFilter("autocomplete_filter", 2, 20)
//        )
//    }
//  }

  def deleteType(name: String) : Future[BulkResult] = {
    var idList = new ListBuffer[String]()
    val bulkOps = new ListBuffer[BulkCompatibleDefinition]()

    val assigns = Await.result(client.execute(search in "amhrouting" -> name size 3000), 25.seconds)
    if (assigns.getHits.getTotalHits == 0) {
      return Future { BulkResult(new BulkResponse(Array.empty[BulkItemResponse],10))}
    }


    for {
    _ <- Future {logger.debug(s"$name to delete: ")}
    _ <- Future {assigns.getHits.getHits.foreach(searchHit => { idList += searchHit.getId }) }
    _ <- Future {logger.debug(s"$name ids to delete: " + idList)}
    _ <- Future {
          for (assignId <- idList) yield {
            bulkOps += delete id assignId from "amhrouting" / name
          }
        }
    _ <- Future {logger.debug(s"$name bulk operations ready ")}
    bulkResp <- client.execute { bulk(bulkOps) } if bulkOps.nonEmpty
    } yield bulkResp

  }

  def initializeIndex (indexName: String) {
    val deleteAssignments = deleteType("assignments")
    val deleteRules = deleteType("rules")
    val deleteBackends = deleteType("backends")
    val deleteDistribution = deleteType("distributionCopies")
    val deleteFeedback = deleteType("feedbackDtnCopies")

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

  def insert(backends: Seq[AMHBackendEntityES]) {
    if (backends.isEmpty) {
      logger.debug("No backends to insert into ES")
    } else {
      withBulk(backends)
      logger.debug("ES Backends insertion done.")
    }
  }

//  private def withoutBulk(backends: Seq[AMHBackendEntityES]) {
////    var responses: List[Future[IndexDefinition]] = List()
//
//    logger.debug("after inserting " + backends.length)
//    import ElasticJackson.Implicits._
//     backends.forall { backend =>
//     val res = client.execute(index into "amhrouting/backends" source backend)
//      val rr = Await.result(res, 10.seconds)
//      logger.debug(s" backend created = $rr.created")
//      true
//    }
//
//    logger.debug("AMH BACKEND FINISHED!")
//  }

  private def withBulk(backends: Seq[AMHBackendEntityES]) {
    import ElasticJackson.Implicits._
    val bulkOps = new ListBuffer[BulkCompatibleDefinition]()
    for (backend <- backends) yield { bulkOps += index into "amhrouting/backends" source backend id backend.code }
    val res = client.execute(bulk(bulkOps)).await
    logger.debug("backend BULK DONE!! " + res)
  }

//  private def bulkBackends(backends: Seq[AMHBackendEntityES]): Iterable[BulkCompatibleDefinition] = {
//    import com.sksamuel.elastic4s.jackson.ElasticJackson.Implicits._
//    val bulkBackends: List[IndexDefinition] = List()
//    backends.foreach(backend => { bulkBackends.+:(index into "routing/backends" source backend) })
//    bulkBackends
//  }

  def insert(backends: AMHBackendEntityES): Future[IndexResult] = {
    //import com.sksamuel.elastic4s.jackson.ElasticJackson.Implicits._
    import ElasticJackson.Implicits._

    import scala.concurrent.ExecutionContext.Implicits.global

    val res = client.execute {
      index into "amhrouting/backends" source backends
    }

        res onComplete {
          case Success(s) => logger.debug(s" success $s")
          case Failure(t) => logger.debug(s"An error has occured: $t")
        }
    res
  }

}
