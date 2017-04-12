package com.groupaxis.groupsuite.synchronizator.domain.model.schemas

import com.groupaxis.groupsuite.persistence.datastore.jdbc.Database
import com.groupaxis.groupsuite.routing.write.domain.model.routing.schema.{Schema, SchemaES}
import com.groupaxis.groupsuite.synchronizator.domain.model.{AMHESRepository, AMHRepository, AMHService}
import com.sksamuel.elastic4s.ElasticClient
import com.sksamuel.elastic4s.jackson.ElasticJackson

import scala.concurrent.{ExecutionContext, Future}

trait SAASchemaRepositoryT extends AMHRepository[Schema, String] {

}

class SAASchemaRepository(val db: Database) extends SAASchemaRepositoryT {

  import com.groupaxis.groupsuite.routing.write.domain.model.routing.schema.SchemaDAO._
  import driver.api._

  override def query(id: String): Future[Option[Schema]] = {
    db.db.run(schemas.filter(_.name === id).result.headOption)
  }

  override def store(schema: Schema): Future[Schema] = {
    db.db.run(schemas returning schemas += schema)
  }

  override def all: Future[Seq[Schema]] =
    db.db.run(schemas.result)

  override def storeAll(schemasToStore: Seq[Schema]): Future[Seq[Schema]] =
    db.db.run(schemas returning schemas ++= schemasToStore)
}

object SAASchemaRepository {
  def apply(db: Database) : SAASchemaRepository = {
    new SAASchemaRepository(db)
  }
}

trait SAASchemaESRepositoryT extends AMHESRepository[SchemaES] {
  implicit override def indexName: String = "routing/schemas"
}

class SAASchemaESRepository(val client: ElasticClient) extends SAASchemaESRepositoryT {

  import ElasticJackson.Implicits._
  import com.sksamuel.elastic4s.ElasticDsl._

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit override def esClient: ElasticClient = client

  override def query(id: String): Future[Option[SchemaES]] = Future {
    None
  }

  override def store(a: SchemaES): Future[SchemaES] = Future {
    a
  }

  override def all: Future[Seq[SchemaES]] = Future {
    Seq()
  }

  override def storeAll(as: Seq[SchemaES]): Future[Seq[SchemaES]] = {
    val f = (schema: SchemaES) => index into indexName source schema id schema.name
    withBulk(f)(as)
  }

}

object SAASchemaESRepository {
  def apply(esClient: ElasticClient):SAASchemaESRepository = {
    new SAASchemaESRepository(esClient)
  }
}


sealed trait SAASynchronousSchemas {
  type SAASchemas = Seq[Schema]
  type SAASchemasES = Seq[SchemaES]

}

object SAASynchronousSchemas extends SAASynchronousSchemas with AMHService[Schema, String, SchemaES] {

  def synchronize(implicit ec: ExecutionContext) : (AMHRepository[Schema, String], AMHRepository[SchemaES, String]) => Future[Seq[SchemaES]] = {
    updateIntoES(toES)
  }

  private def toES(schemas: SAASchemas)(implicit ec: ExecutionContext): Future[SAASchemasES] = {

    Future {
      schemas.map(s => s.toES)
    }
  }
}