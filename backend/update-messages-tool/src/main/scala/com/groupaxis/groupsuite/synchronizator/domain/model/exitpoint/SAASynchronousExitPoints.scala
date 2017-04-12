package com.groupaxis.groupsuite.synchronizator.domain.model.exitpoint

import com.groupaxis.groupsuite.persistence.datastore.jdbc.Database
import com.groupaxis.groupsuite.routing.write.domain.model.routing.exit.point.{ExitPoint, ExitPointES}
import com.groupaxis.groupsuite.synchronizator.domain.model.{AMHESRepository, AMHRepository, AMHService}
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.jackson.ElasticJackson

import scala.concurrent.{ExecutionContext, Future}

trait SAAExitPointRepositoryT extends AMHRepository[ExitPoint, (String, String)] {

}

class SAAExitPointRepository(val db: Database) extends SAAExitPointRepositoryT {

  import com.groupaxis.groupsuite.routing.write.domain.model.routing.exit.point.ExitPointDAO._
  import driver.api._

  override def query(id: (String, String)): Future[Option[ExitPoint]] = {
    db.db.run(exitPoints.filter(_.name === id._1).filter(_.queueType === id._2).result.headOption)
  }

  override def store(exitPoint: ExitPoint): Future[ExitPoint] = {
    db.db.run(exitPoints returning exitPoints += exitPoint)
  }

  override def all: Future[Seq[ExitPoint]] =
    db.db.run(exitPoints.result)

  override def storeAll(exitPointsToStore: Seq[ExitPoint]): Future[Seq[ExitPoint]] =
    db.db.run(exitPoints returning exitPoints ++= exitPointsToStore)
}

object SAAExitPointRepository {
  def apply(db: Database) : SAAExitPointRepository = {
    new SAAExitPointRepository(db)
  }
}

trait SAAExitPointESRepositoryT extends AMHESRepository[ExitPointES] {
  implicit override def indexName: String = "routing/exitPoints"
}

class SAAExitPointESRepository(val client: ElasticClient) extends SAAExitPointESRepositoryT {

  import ElasticJackson.Implicits._
  import com.sksamuel.elastic4s.ElasticDsl._

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit override def esClient: ElasticClient = client

  override def query(id: String): Future[Option[ExitPointES]] = Future {
    None
  }

  override def store(a: ExitPointES): Future[ExitPointES] = Future {
    a
  }

  override def all: Future[Seq[ExitPointES]] = Future {
    Seq()
  }

  override def storeAll(as: Seq[ExitPointES]): Future[Seq[ExitPointES]] = {
    val f = (exitPoint: ExitPointES) => index into indexName source exitPoint id s"${exitPoint.name}_${exitPoint.queueType}"
    withBulk(f)(as)
  }

}

object SAAExitPointESRepository {
  def apply(esClient: ElasticClient):SAAExitPointESRepository = {
    new SAAExitPointESRepository(esClient)
  }
}


sealed trait SAASynchronousExitPoints {
  type SAAExitPoints = Seq[ExitPoint]
  type SAAExitPointsES = Seq[ExitPointES]

}

object SAASynchronousExitPoints extends SAASynchronousExitPoints with AMHService[ExitPoint, (String, String), ExitPointES] {

  def synchronize(implicit ec: ExecutionContext) : (AMHRepository[ExitPoint, (String, String)], AMHRepository[ExitPointES, String]) => Future[Seq[ExitPointES]] = {
    updateIntoES(toES)
  }

  private def toES(exitPoints: SAAExitPoints)(implicit ec: ExecutionContext): Future[SAAExitPointsES] = {

    Future {
      exitPoints.map(s => s.toES)
    }
  }
}