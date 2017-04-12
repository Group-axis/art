package com.groupaxis.groupsuite.synchronizator.domain.model.points

import com.groupaxis.groupsuite.persistence.datastore.jdbc.Database
import com.groupaxis.groupsuite.routing.write.domain.model.routing.point.{Point, PointES}
import com.groupaxis.groupsuite.routing.write.domain.model.routing.rule.RuleEntity
import com.groupaxis.groupsuite.synchronizator.domain.model.{AMHESRepository, AMHRepository, AMHService}
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.jackson.ElasticJackson

import scala.concurrent.{ExecutionContext, Future}

trait SAARuleRepositoryT extends AMHRepository[RuleEntity, (Long, String)] {

}

class SAARuleRepository(val db: Database) extends SAARuleRepositoryT {

  import com.groupaxis.groupsuite.routing.write.domain.model.routing.rule.RuleDAO._
  import driver.api._

  override def query(id: (Long, String)): Future[Option[RuleEntity]] = {
    db.db.run(rules.filter(_.sequence === id._1).filter(_.routingPointName === id._2).result.headOption)
  }

  override def store(rule: RuleEntity): Future[RuleEntity] = {
    db.db.run(rules returning rules += rule)
  }

  override def all: Future[Seq[RuleEntity]] =
    db.db.run(rules.result)

  override def storeAll(rulesToStore: Seq[RuleEntity]): Future[Seq[RuleEntity]] =
    db.db.run(rules returning rules ++= rulesToStore)
}

object SAARuleRepository {
  def apply(db: Database):SAARuleRepository = {
    new SAARuleRepository(db)
  }
}

trait SAAPointESRepositoryT extends AMHESRepository[PointES] {
  implicit override def indexName: String = "routing/points"
}

class SAAPointESRepository(val client: ElasticClient) extends SAAPointESRepositoryT {

  import ElasticJackson.Implicits._
  import com.sksamuel.elastic4s.ElasticDsl._

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit override def esClient : ElasticClient = client

  override def query(id: String): Future[Option[PointES]] = Future {
    None
  }

  override def store(a: PointES): Future[PointES] = Future {
    a
  }

  override def all: Future[Seq[PointES]] = Future {
    Seq()
  }

  override def storeAll(as: Seq[PointES]): Future[Seq[PointES]] = {
    val f = (point: PointES) => index into indexName source point id point.pointName
    withBulk(f)(as)
  }

}

object SAAPointESRepository {
  def apply(esClient: ElasticClient) : SAAPointESRepository = {
    new SAAPointESRepository(esClient)
  }
}


sealed trait SAASynchronousRules {
  type SAARules = Seq[RuleEntity]
  type SAAPointsES = Seq[PointES]

}

object SAASynchronousRules extends SAASynchronousRules with AMHService[RuleEntity, (Long,String), PointES] {

  def synchronize(implicit ec: ExecutionContext) : (AMHRepository[RuleEntity, (Long,String)], AMHRepository[PointES, String]) => Future[Seq[PointES]] = {
    updateIntoES(toES)
  }

  private def toES(rules: SAARules)(implicit ec: ExecutionContext): Future[SAAPointsES] = {

    Future {
      rules
        .groupBy[Point](rule => Point(rule.routingPointName, full = true, Seq()))
        .foldLeft(List[Point]())((acc, v) => acc :+ v._1.copy(rules = v._2))
        .map(p => p.toES)
    }
  }
}