package com.groupaxis.groupsuite.routing.infrastructor.es

import akka.actor.{Actor, Props}
import com.groupaxis.groupsuite.persistence.datastore.es.util.GPESHelper
import com.groupaxis.groupsuite.routing.es.schema.SAASchema._
import com.groupaxis.groupsuite.routing.write.domain.model.routing.exit.point.ExitPointES
import com.groupaxis.groupsuite.routing.write.domain.model.routing.message.partner.MessagePartnerES
import com.groupaxis.groupsuite.routing.write.domain.model.routing.point.PointES
import com.groupaxis.groupsuite.routing.write.domain.model.routing.rule.RuleEntityES
import com.groupaxis.groupsuite.routing.write.domain.model.routing.rule.RuleMessages._
import com.groupaxis.groupsuite.routing.write.domain.model.routing.schema.SchemaES
import com.groupaxis.groupsuite.routing.write.domain.{ImportSAARoutingES, SAARoutingESImported}
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.jackson.ElasticJackson
import com.sksamuel.elastic4s.jackson.ElasticJackson.Implicits._
import com.sksamuel.elastic4s.{ElasticClient, _}
import org.apache.logging.log4j.scala.Logging
import org.elasticsearch.action.update.UpdateResponse

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.{Failure, Success}

trait ESRepository extends Logging {

  import com.sksamuel.elastic4s.ElasticDsl._

  def indexName: String

  def esClient: ElasticClient

  def storeAllInIndex[ESEntity](as: Seq[ESEntity], indexName: String): Future[Seq[ESEntity]] = {
    //Do nothing implementation, override this method as you want.
    import scala.concurrent.ExecutionContext.Implicits.global
    Future {
      Seq()
    }
  }

  protected def withBulk[ESEntity](f: ESEntity => BulkCompatibleDefinition)(entities: Seq[ESEntity])(implicit indexName: String, ec: ExecutionContext) = {
    val response = Future {
      entities
    }

    val future = if (entities.isEmpty) response
                 else esClient.execute(bulk(entities.map(f)))
                     .flatMap(_ => response)

    future onComplete {
      case Success(s) => logger.debug(s"Bulk Operation on index $indexName succeed. ")
      case Failure(t) => logger.error(s"Error while bulking on index $indexName: ${t.getMessage}")
    }

    future
  }

  protected def addRecoverAndLog[ESEntity](future: Future[Seq[ESEntity]], default: Seq[ESEntity])(implicit ec: ExecutionContext): Future[Seq[ESEntity]] = {
    future.recoverWith({ case e: Throwable => Future {
      default
    }
    })

    future onComplete {
      case Success(s) => logger.debug(s"Operation on index $indexName succeed. ")
      case Failure(t) => logger.error(s"Error on index $indexName: ${t.getMessage}")
    }

    future
  }

}

object ESRuleWriteRepository {

  final val Name = "es-rule-write-repository"

  def props(esClient: ElasticClient): Props = Props(classOf[ESRuleWriteRepository], esClient)

}

//TODO: Actually this actor should take the data from DB directly, and receive just messages with ids
//class ESRuleWriteRepository(val databaseService: DatabaseService) extends Actor with ActorLogging  {
class ESRuleWriteRepository(val esClient: ElasticClient) extends Actor with Logging with ESRepository {
  implicit val indexName: String = saaIndexName

  /**
    * import ElasticJackson.Implicits._
    */

  import scala.concurrent.ExecutionContext.Implicits.global

  //  def insert(points: Seq[PointES]) : Either[String, PointsESInserted] =  {
  //    withBulk(points)
  //  }


  //  private def withBulk(points: Seq[PointES]) : Either[String, PointsESInserted] = {
  //    import ElasticJackson.Implicits._
  //
  //    val bulkOps = new ListBuffer[BulkCompatibleDefinition]()
  //    for (point <- points) yield { bulkOps += index into "routing" -> "points" source point id point.pointName }
  //    val res : BulkResult = Await.result(esClient.execute(bulk(bulkOps)), 15.seconds)
  //    if (res.hasFailures) {
  //      val errorMsg = res.failureMessage
  //      logger.info(s" points failed with $errorMsg")
  //      Left(res.failureMessage)
  //    }
  //    else {
  //      logger.info(" points BULK DONE!! ")
  //      Right(PointsESInserted(points))
  //    }
  //  }

  def insert(pointName: String, ruleES: RuleEntityES): Int = {
    import ElasticJackson.Implicits._

    val res = esClient.execute {
      search in "routing" -> "points" query {
        termQuery("_id", pointName)
      }
    }

    val result: RichSearchResponse = Await.result(res, 15.seconds)
    val points: Seq[PointES] = result.as[PointES]
    val pointsFiltered = points.find(_.pointName equals pointName)

    val updatedPoint = pointsFiltered.map(point => {
      point.copy(rules = point.rules :+ ruleES)
    })

    updatedPoint.foreach(point => {
      logger.debug(s"Inserting a rule $ruleES into point name $pointName")
      updatePoint(point)
    })

    updatedPoint.size
  }


  def updateRule(pointName: String, ruleES: RuleEntityES): Int = {
    import ElasticJackson.Implicits._

    val res = esClient.execute {
      search in "routing" -> "points" query {
        termQuery("_id", pointName)
      }
    }

    val result: RichSearchResponse = Await.result(res, 15.seconds)
    if (result.isEmpty) {
      return 0
    }

    //     val points :Seq[PointES]= result.as[PointES]
    //
    //     val pointsFiltered = points.find(_.pointName equals pointName)
    //val updatedPoint = pointsFiltered.map(point => {
    //  point.copy(rules = point.rules.map(rule =>
    //    if(rule.sequence equals ruleES.sequence)
    //      ruleES
    //    else
    //      rule
    //  ))
    //})
    //     updatedPoint.foreach(point => {
    //       logger.debug(s"Updating rule $ruleES into point name $pointName")
    //       updatePoint(point)
    //     })
    val pointsFiltered = result.hits(0).as[PointES]

    val updatedPoint = pointsFiltered.copy(rules = pointsFiltered.rules.map(rule =>
      if (rule.sequence equals ruleES.sequence)
        ruleES
      else
        rule
    ))

    logger.debug(s"Updating rule $ruleES into point name $pointName")
    updatePoint(updatedPoint)

    1
  }

  private def updatePoint(point: PointES): UpdateResponse = {
    import ElasticJackson.Implicits._

    import scala.concurrent.ExecutionContext.Implicits.global

    val res = esClient.execute {
      update id point.pointName in "routing/points" docAsUpsert point
    }

    res onComplete {
      case Success(s) => logger.debug(s" success $s")
      case Failure(t) => logger.debug(s"An error has occurred: $t")
    }

    Await.result(res, 15.seconds)

  }


  //  def deleteRoutingType(name: String) : Future[BulkResult] = {
  //    var idList = new ListBuffer[String]()
  //    val bulkOps = new ListBuffer[BulkCompatibleDefinition]()
  //  try {
  //    val items = Await.result(esClient.execute(search in "routing" -> name size 10000), 50.seconds)
  //    if (items.getHits.getTotalHits == 0) {
  //      return Future {
  //        BulkResult(new BulkResponse(Array.empty[BulkItemResponse], 10))
  //      }
  //    }
  //
  //
  //    for {
  //      _ <- Future {
  //        logger.debug("items to delete: " + items)
  //      }
  //      _ <- Future {
  //        items.getHits.getHits.foreach(searchHit => {
  //          idList += searchHit.getId
  //        })
  //      }
  //      _ <- Future {
  //        logger.debug("items ids to delete: " + idList)
  //      }
  //      _ <- Future {
  //        for (itemId <- idList) yield {
  //          bulkOps += delete id itemId from "routing" / name
  //        }
  //      }
  //      _ <- Future {
  //        logger.debug("bulk operations: " + bulkOps)
  //      }
  //      bulkResp <- esClient.execute {
  //        bulk(bulkOps)
  //      } if bulkOps.nonEmpty
  //    } yield bulkResp
  //  } catch {
  //    case ex : Exception =>
  //      ex.printStackTrace()
  //      Future {
  //        BulkResult(new BulkResponse(Array.empty[BulkItemResponse], 10))
  //      }
  //  }
  //  }

  //  private def initializePointES() : Either[String, Int] = {
  //    val deletePoints = deleteRoutingType("points")
  //
  //     val removeAll = for {
  //      a  <- deletePoints
  //    } yield a
  //
  //    val deleteResp = Await.result(deleteRoutingType("points"), 15.seconds)
  //    //TODO: Find a way to get rules count.
  //    if (deleteResp.hasSuccesses) Right(200) else Left(deleteResp.failureMessage)
  //  }

  private def importSAARoutingES(points: Seq[PointES], exitPoints: Seq[ExitPointES], messagePartners: Seq[MessagePartnerES], schemas : Seq[SchemaES]): Future[Either[String, SAARoutingESImported]] = {

    val indexPoint = (point: PointES) => index into indexName -> "points" source point id point.pointName
    val indexExitPoint = (exitPoint: ExitPointES) => index into indexName -> "exitPoints" source exitPoint id s"${exitPoint.name}_${exitPoint.queueType}"
    val indexMessagePartner = (messagePartner: MessagePartnerES) => index into indexName -> "messagePartners" source messagePartner id messagePartner.name
    val indexSchema = (schema: SchemaES) => index into indexName -> "schemas" source schema id schema.name

    val importIntoSAARouting = for {
      initIndex <- GPESHelper.initializeIndex(indexName, createSAAIndex)(esClient)
      logPointIndexing <- Future {
        logger.debug(s"Indexing into points ${points.size} record(s) ")
      }
      insertedPoints <- withBulk[PointES](indexPoint)(points)
      logExitPointIndexing <- Future {
        logger.debug(s"Indexing into exitPoints ${exitPoints.size} record(s) ")
      }
      insertedExitPoints <- withBulk[ExitPointES](indexExitPoint)(exitPoints)
      logMessagePartnerIndexing <- Future {
        logger.debug(s"Indexing into messagePartners ${messagePartners.size} record(s) ")
      }
      insertedMessagePartners <- withBulk[MessagePartnerES](indexMessagePartner)(messagePartners)
      logMessagePartnerIndexing <- Future {
        logger.debug(s"Indexing into schemas ${schemas.size} record(s) ")
      }
      insertedSchemas <- withBulk[SchemaES](indexSchema)(schemas)
    } yield Right(SAARoutingESImported(insertedPoints, insertedExitPoints, insertedMessagePartners,insertedSchemas))

    importIntoSAARouting.recoverWith {
      case e: Throwable =>
        logger.error(s"An error has occurred while importing SAA xml definition into ES: ${e.getMessage}")
        Future {
          Left(e.getMessage)
        }
    }

    //Future[Either[String, SAARoutingESImported]] {Left("")}
  }

  def receive: Receive = {
    case InsertRuleES(pointName, rule) =>
      logger.debug(s" inserting into point $pointName the rule $rule")
      insert(pointName, rule)
      sender() ! RuleESInserted(rule)
    case UpdateRuleES(pointName, rule) =>
      logger.debug(s" updating into point $pointName the rule $rule")
      updateRule(pointName, rule)
      sender() ! RuleESUpdated(rule)
    case InsertPointsES(points) =>
      logger.debug(s" inserting into points ")
    //      val response = insert(points)
    //      sender() ! response
    case ImportSAARoutingES(points, existPoints, messagePartners, schemas) =>
      logger.debug(s" ImportSAARoutingES received. ")
      val originalSender = sender()
      importSAARoutingES(points, existPoints, messagePartners, schemas)
        .onComplete{
          case Success(either) =>
            logger.debug(s"ImportSAARoutingES success with either == $either")
            originalSender ! either

          case Failure(error) =>
            logger.debug(s"ImportSAARoutingES error ${error.getMessage}")
            originalSender ! Left(error.getMessage)
        }
      //(_ => originalSender ! _)
    //    case InitializePointsES =>
    //      logger.debug(s" initializing points ")
    //      val resp = initializePointES()
    //      resp.fold(
    //        ex  => {
    //          logger.warn(s"Exception while initializing $ex")
    //          sender() ! Left(ex)
    //        },
    //        deleted => {
    //          logger.debug(s"$deleted ES rule(s) initialized")
    //          sender() ! Right(PointsESInitialized(deleted))
    //        }
    //    )

  }

}