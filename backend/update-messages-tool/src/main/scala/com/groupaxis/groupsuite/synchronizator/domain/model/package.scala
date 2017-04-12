package com.groupaxis.groupsuite.synchronizator.domain

import com.sksamuel.elastic4s.{BulkCompatibleDefinition, ElasticClient}
import org.apache.logging.log4j.scala.Logging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

package object model {

  trait Repository[A, IdType] {
    def query(id: IdType): Future[Option[A]]

    def store(a: A): Future[A]

    def all: Future[Seq[A]]

    def storeAll(as: Seq[A]): Future[Seq[A]]
  }

  trait AMHRepository[Entity, IdType] extends Repository[Entity, IdType] {
  }

  trait AMHESRepository[AMHESEntity] extends AMHRepository[AMHESEntity, String] with Logging  {
    import com.sksamuel.elastic4s.ElasticDsl._

    def indexName : String

    def esClient : ElasticClient

    def storeAllInIndex(as: Seq[AMHESEntity], indexName: String): Future[Seq[AMHESEntity]] = {
      //Do nothing implementation, override this method as you want.
      import scala.concurrent.ExecutionContext.Implicits.global
      Future{Seq()}
    }

    protected def withBulk(f: AMHESEntity => BulkCompatibleDefinition)(entities: Seq[AMHESEntity])(implicit indexName : String, ec : ExecutionContext) = {

      val future = esClient.execute(bulk(entities.map(f)))
        .flatMap(_ => Future{entities})

      future.recoverWith({ case e: Throwable => Future { entities } })

      future onComplete {
        case Success(s) => logger.debug(s" Update of $indexName succeed ")
        case Failure(t) => logger.error(s"Error updating index $indexName: ${t.getLocalizedMessage}")
      }

      future
    }

    protected def addRecoverAndLog[A](future : Future[Seq[A]], default :Seq[A])(implicit ec : ExecutionContext) : Future[Seq[A]] = {
      future.recoverWith({ case e: Throwable => Future { default } })

      future onComplete {
        case Success(s) => logger.debug(s" Update of $indexName succeed ")
        case Failure(t) => logger.error(s"Error updating index $indexName: ${t.getLocalizedMessage}")
      }

      future
    }

  }

  trait AMHService[AMHEntity, EntityType, AMHEntityES] {

    def updateIntoES(toES: Seq[AMHEntity] => Future[Seq[AMHEntityES]])(implicit ec: ExecutionContext): (AMHRepository[AMHEntity, EntityType], AMHRepository[AMHEntityES, String]) => Future[Seq[AMHEntityES]]
    = (repo, repoES) => {
      val updatedEntities = for {
        allEntities <- repo.all
        entities <- if (allEntities.nonEmpty) Future.successful(allEntities)
                    else Future.successful(List[AMHEntity]())
        entitiesConvertedToES <- toES(entities)
        entitiesES <- if (entitiesConvertedToES.nonEmpty) Future.successful(entitiesConvertedToES)
                      else Future.successful(List[AMHEntityES]())
        updatedEntities <- {
                    if (entitiesES.nonEmpty) repoES.storeAll(entitiesES)
                    else Future.successful(entitiesES)
        }
      } yield updatedEntities

      updatedEntities
    }
  }

  //  trait AMHAssignmentService[AMHEntity, AMHBackendAssignmentEntity, AMHRuleAssignmentEntity, AMHEntityES] extends AMHService[AMHEntity, AMHEntityES] {
  //    def allBackendAssignments(toES: Seq[AMHEntity] => Future[Try[Seq[AMHEntityES]]])(implicit ec: ExecutionContext): (AMHRepository[AMHEntity], AMHRepository[AMHEntityES]) => Future[Try[Seq[AMHEntityES]]]
  //    = (repo, repoES) =>
  //
  //  }

}