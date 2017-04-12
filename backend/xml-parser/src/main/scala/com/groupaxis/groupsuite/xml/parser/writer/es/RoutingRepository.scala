package com.groupaxis.groupsuite.xml.parser.writer.es

import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.ElasticsearchClientUri
import com.sksamuel.elastic4s.ElasticDsl._

import scala.concurrent._
import scala.concurrent.duration._
import ExecutionContext.Implicits.global
import scala.util.{Failure, Success}
import com.sksamuel.elastic4s.IndexDefinition
import com.sksamuel.elastic4s.IndexResult
import com.sksamuel.elastic4s.BulkCompatibleDefinition
import com.sksamuel.elastic4s.ElasticDsl
import com.sksamuel.elastic4s.jackson.ElasticJackson
import com.sksamuel.elastic4s.jackson.ObjectSource
import com.groupaxis.groupsuite.routing.write.domain.model.routing.point.PointES
import org.apache.logging.log4j.scala.Logging

class RoutingRepository(machine: String, port: Integer) extends Logging {
  import org.elasticsearch.common.settings.Settings
  val settings = Settings.settingsBuilder().put("cluster.name", "groupsuite").build()
  val client = ElasticClient.transport(settings, ElasticsearchClientUri("elasticsearch://127.0.0.1:9300"))

  //  val res = client execute { search in "index_name/type_name" query "your_query" }

  //  res onComplete {
  //    case Success(s) => logger.debug(s)
  //    case Failure(t) => logger.debug("An error has occured: " + t)
  //  }

  //  logger.debug("Request sent")

  def insert(points: Seq[PointES]) {
    withoutBulk(points)
  }

  private def withoutBulk(points: Seq[PointES]) {
    var responses: List[Future[IndexDefinition]] = List();

    logger.debug("after inserting " + points.length)
    import ElasticDsl._
    import ElasticJackson.Implicits._
    val unPoint = points.forall { point =>  
     val res = client.execute(index into "routing/points" source point)
      val rr = Await.result(res, 10.seconds)
      logger.debug(s" created = $rr.created")
      true
    }
    
//    points.foreach(point => { responses.+:(insert(point)) })
//    val res = Future.sequence(responses) .await
//    res.onComplete {
//      case Success(s) => logger.debug(s"*** success $s")
//      case Failure(t) => logger.debug(s"*** An error has occured: $t")
//      case zz         => logger.debug(s"*** Something else!! : $zz")
//    }
    logger.debug("FINITO!")
  }
  private def withBulk(points: Seq[PointES]) {
    /*
     client.execute { create index "places" shards 3 replicas 2 } 
     * */
    import ElasticDsl._
    import ElasticJackson.Implicits._

    val ops: Iterable[BulkCompatibleDefinition] = for (point <- points) yield { index into "routing/points" source point }
    //    val ops : Iterable[BulkCompatibleDefinition] = for (point <- points) yield { index into "routing/points" source point.JacksonJsonIndexable.json(point)}
    val res = client.execute(bulk(ops)).await
    logger.error("failureMsg " + res.failureMessage)
    logger.error("has failures  " + res.hasFailures)
    logger.debug("res " + res)

    //    res.onComplete {
    //      case Success(s) => logger.debug(s"*** success $s")
    //      case Failure(t) => logger.debug(s"*** An error has occured: $t")
    //      case _         => logger.debug(s"*** Something else!! : ")
    //    }
  }

  private def bulkPoints(points: Seq[PointES]): Iterable[BulkCompatibleDefinition] = {
    import com.sksamuel.elastic4s.jackson.ElasticJackson.Implicits._
    var bulkPoints: List[IndexDefinition] = List();
    points.foreach(point => { bulkPoints.+:(index into "routing/points" source point) })
    bulkPoints
  }

  def insert(point: PointES): Future[IndexResult] = {
    import com.sksamuel.elastic4s.jackson.ElasticJackson.Implicits._
    val res = client.execute {
      index into "routing/points" source point
    }

        res onComplete {
          case Success(s) => logger.debug(s" success $s")
          case Failure(t) => logger.error(s"An error has occured: $t")
        }
    res
  }

}
