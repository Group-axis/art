package com.groupaxis.groupsuite.synchronizator.domain.model.backends

import com.groupaxis.groupsuite.persistence.datastore.jdbc.Database
import com.groupaxis.groupsuite.routing.write.domain.model.amh.backend.{AMHBackendEntity, AMHBackendEntityES}
import com.groupaxis.groupsuite.synchronizator.domain.model.{AMHESRepository, AMHRepository, AMHService}
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.jackson.ElasticJackson

import scala.concurrent.{ExecutionContext, Future}

trait AMHBackendRepositoryT extends AMHRepository[AMHBackendEntity, String] {
}

class AMHBackendRepository(val db: Database) extends AMHBackendRepositoryT {

  import com.groupaxis.groupsuite.routing.write.domain.model.amh.backend.BackendDAO._
  import driver.api._

  override def query(id: String): Future[Option[AMHBackendEntity]] = {
    db.db.run(amhBackends.filter(_.code === id).result.headOption)
  }

  override def store(backend: AMHBackendEntity): Future[AMHBackendEntity] = {
    db.db.run(amhBackends returning amhBackends += backend)
  }

  override def all: Future[Seq[AMHBackendEntity]] =
    db.db.run(amhBackends.result)

  override def storeAll(backends: Seq[AMHBackendEntity]): Future[Seq[AMHBackendEntity]] =
    db.db.run(amhBackends returning amhBackends ++= backends)
}

object AMHBackendRepository {
  def apply(db: Database) = {
    new AMHBackendRepository(db)
  }
}

trait AMHBackendESRepositoryT extends AMHESRepository[AMHBackendEntityES] {
  implicit override def indexName: String = "amhrouting/backends"
}

class AMHBackendESRepository(val client: ElasticClient) extends AMHBackendESRepositoryT {

  import ElasticJackson.Implicits._

  import com.sksamuel.elastic4s.ElasticDsl._

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit override def esClient = client

  override def query(id: String): Future[Option[AMHBackendEntityES]] = Future {
    None
  }

  override def store(a: AMHBackendEntityES): Future[AMHBackendEntityES] = Future {
    a
  }

  override def all: Future[Seq[AMHBackendEntityES]] = Future {
    Seq()
  }

  override def storeAll(as: Seq[AMHBackendEntityES]): Future[Seq[AMHBackendEntityES]] = {
    val f = (backend: AMHBackendEntityES) => index into indexName source backend id backend.code
    withBulk(f)(as)
  }

}

object AMHBackendESRepository {
  def apply(esClient: ElasticClient) = {
    new AMHBackendESRepository(esClient)
  }
}


sealed trait AMHSynchronousBackends {
  type AMHBackends = Seq[AMHBackendEntity]
  type AMHBackendsES = Seq[AMHBackendEntityES]

}

object AMHSynchronousBackends extends AMHSynchronousBackends with AMHService[AMHBackendEntity, String, AMHBackendEntityES] {

  def synchronize(implicit ec: ExecutionContext) = {
    updateIntoES(toES)
  }

  private def toES(backends: AMHBackends)(implicit ec: ExecutionContext): Future[AMHBackendsES] = {
    Future {
      backends.map(backend => backend.toES)
    }
  }
}